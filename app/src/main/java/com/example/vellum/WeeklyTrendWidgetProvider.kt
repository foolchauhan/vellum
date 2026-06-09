package com.example.vellum

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.example.vellum.data.local.VellumDatabase
import com.example.vellum.data.local.TransactionEntity
import com.example.vellum.data.local.TransactionSplit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class WeeklyTrendWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WeeklyTrendWidgetProvider::class.java)
            )
            updateAllWidgets(context, appWidgetManager, ids)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val db = VellumDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val theme = try {
                    db.preferenceDao().getPreferenceValue("theme") ?: "Greenboard"
                } catch (e: Exception) {
                    "Greenboard"
                }
                
                val transactions = db.transactionDao().getAllActiveTransactions()

                appWidgetIds.forEach { widgetId ->
                    val bitmap = createWeeklyTrendBitmap(context, theme, transactions, widgetId)
                    val views = RemoteViews(context.packageName, R.layout.weekly_trend_widget).apply {
                        setImageViewBitmap(R.id.widget_image, bitmap)
                        
                        // Main click -> App home
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pending = PendingIntent.getActivity(
                            context,
                            widgetId * 10,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_image, pending)

                        // Settings click -> Configure Screen
                        val intentConfigure = Intent(context, WeeklyTrendWidgetConfigureActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            data = android.net.Uri.parse("widget://configure/weekly_trend/$widgetId")
                        }
                        val pendingConfigure = PendingIntent.getActivity(
                            context,
                            widgetId * 10 + 1,
                            intentConfigure,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.btn_configure, pendingConfigure)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createWeeklyTrendBitmap(
        context: Context,
        theme: String,
        transactions: List<TransactionEntity>,
        widgetId: Int
    ): Bitmap {
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = when (theme) {
            "Dark" -> Color.parseColor("#1E2322")
            "Blueprint" -> Color.parseColor("#0C1B33")
            "Light" -> Color.parseColor("#FAF3E0")
            "Cement" -> Color.parseColor("#EFEFEF")
            "Glass" -> Color.parseColor("#D0E6F8")
            else -> Color.parseColor("#1B3D2B")
        }

        val paintBg = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        val isDark = theme != "Light" && theme != "Cement" && theme != "Glass"
        val chalkWhiteColor = if (isDark) {
            when (theme) {
                "Blueprint" -> Color.parseColor("#D3E7FF")
                else -> Color.parseColor("#F5F5F5")
            }
        } else {
            Color.parseColor("#4A3B32")
        }

        // Double hand-drawn border
        val paintBorder = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 6f
            style = Paint.Style.STROKE
            pathEffect = CornerPathEffect(10f)
            isAntiAlias = true
        }
        canvas.drawRect(12f, 12f, width - 12f, height - 12f, paintBorder)
        paintBorder.strokeWidth = 2f
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, paintBorder)

        // Fonts
        val fontTitle = try {
            ResourcesCompat.getFont(context, R.font.fredericka_the_great_regular)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
        val fontBody = try {
            ResourcesCompat.getFont(context, R.font.patrick_hand)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        // Retrieve saved interval
        val interval = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .getString("weekly_trend_interval_$widgetId", "weekly") ?: "weekly"

        val pointsCount: Int
        val dailySpends: DoubleArray
        val dayLabels: Array<String>
        val titleText: String

        val cal = Calendar.getInstance()
        when (interval) {
            "monthly" -> {
                pointsCount = 30
                dailySpends = DoubleArray(30)
                dayLabels = Array(30) { "" }
                for (i in 29 downTo 0) {
                    val startOfDay = Calendar.getInstance().apply {
                        time = cal.time
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

                    val dayExpenses = transactions.filter {
                        it.timestamp in startOfDay..endOfDay && it.type == "EXPENSE" && !it.isDeleted
                    }.sumOf { tx ->
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) splits.sumOf { it.amount } else tx.amount
                    }
                    dailySpends[i] = dayExpenses
                    // Only label every 6 days to fit layout beautifully
                    if (i % 6 == 0) {
                        dayLabels[i] = SimpleDateFormat("d/M", Locale.US).format(cal.time)
                    }
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                titleText = "Monthly Trend"
            }
            "yearly" -> {
                pointsCount = 12
                dailySpends = DoubleArray(12)
                dayLabels = Array(12) { "" }
                for (i in 11 downTo 0) {
                    val startOfMonth = Calendar.getInstance().apply {
                        time = cal.time
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val endOfMonth = Calendar.getInstance().apply {
                        time = cal.time
                        set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val monthExpenses = transactions.filter {
                        it.timestamp in startOfMonth..endOfMonth && it.type == "EXPENSE" && !it.isDeleted
                    }.sumOf { tx ->
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) splits.sumOf { it.amount } else tx.amount
                    }
                    dailySpends[i] = monthExpenses
                    dayLabels[i] = SimpleDateFormat("MMM", Locale.US).format(cal.time)
                    cal.add(Calendar.MONTH, -1)
                }
                titleText = "Yearly Trend"
            }
            "all_time" -> {
                pointsCount = 5
                dailySpends = DoubleArray(5)
                dayLabels = Array(5) { "" }
                for (i in 4 downTo 0) {
                    val year = cal.get(Calendar.YEAR)
                    val startOfYear = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val endOfYear = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, Calendar.DECEMBER)
                        set(Calendar.DAY_OF_MONTH, 31)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val yearExpenses = transactions.filter {
                        it.timestamp in startOfYear..endOfYear && it.type == "EXPENSE" && !it.isDeleted
                    }.sumOf { tx ->
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) splits.sumOf { it.amount } else tx.amount
                    }
                    dailySpends[i] = yearExpenses
                    dayLabels[i] = year.toString()
                    cal.add(Calendar.YEAR, -1)
                }
                titleText = "All Time Trend"
            }
            else -> { // weekly
                pointsCount = 7
                dailySpends = DoubleArray(7)
                dayLabels = Array(7) { "" }
                for (i in 6 downTo 0) {
                    val startOfDay = Calendar.getInstance().apply {
                        time = cal.time
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

                    val dayExpenses = transactions.filter {
                        it.timestamp in startOfDay..endOfDay && it.type == "EXPENSE" && !it.isDeleted
                    }.sumOf { tx ->
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) splits.sumOf { it.amount } else tx.amount
                    }
                    dailySpends[i] = dayExpenses
                    dayLabels[i] = SimpleDateFormat("EEE", Locale.US).format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                titleText = "Weekly Trend"
            }
        }

        // Header Title
        val paintTitle = Paint().apply {
            color = chalkWhiteColor
            typeface = fontTitle
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(titleText, (width / 2).toFloat(), 75f, paintTitle)

        // Separator Line
        val paintLine = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(120f, 95f, (width - 120).toFloat(), 95f, paintLine)

        // Setup chart boundaries
        val chartLeft = 80f
        val chartRight = 540f
        val chartBottom = 330f
        val chartTop = 140f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // Y-Max calculation
        val maxSpend = dailySpends.maxOrNull() ?: 0.0
        val yMax = if (maxSpend > 0) Math.ceil(maxSpend / 50.0) * 50.0 else 100.0

        // Draw Y Axes and grid lines
        val paintAxes = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawLine(chartLeft, chartTop - 10f, chartLeft, chartBottom, paintAxes) // Y axis
        canvas.drawLine(chartLeft, chartBottom, chartRight + 10f, chartBottom, paintAxes) // X axis

        // Grid lines
        val paintGrid = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 1f
            alpha = 60
            isAntiAlias = true
        }
        canvas.drawLine(chartLeft, chartTop + (chartHeight * 0.5f), chartRight, chartTop + (chartHeight * 0.5f), paintGrid)
        canvas.drawLine(chartLeft, chartTop, chartRight, chartTop, paintGrid)

        // Draw Y labels
        val paintLabels = Paint().apply {
            color = chalkWhiteColor
            typeface = fontBody
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText(String.format(Locale.US, "$%.0f", yMax), 15f, chartTop + 8f, paintLabels)
        canvas.drawText(String.format(Locale.US, "$%.0f", yMax * 0.5), 15f, chartTop + (chartHeight * 0.5f) + 8f, paintLabels)
        canvas.drawText("$0", 35f, chartBottom + 8f, paintLabels)

        // Draw points and lines
        val stepX = chartWidth / (pointsCount - 1).toFloat()
        val points = Array(pointsCount) { PointF() }
        for (i in 0 until pointsCount) {
            val px = chartLeft + (i * stepX)
            val py = chartBottom - ((dailySpends[i].toFloat() / yMax.toFloat()) * chartHeight)
            points[i] = PointF(px, py)
        }

        // Draw line connection path
        val paintChartLine = Paint().apply {
            color = if (isDark) {
                if (theme == "Blueprint") Color.parseColor("#87CEEB") else Color.parseColor("#8FCE5E")
            } else {
                Color.parseColor("#2D6E4E")
            }
            strokeWidth = 5f
            style = Paint.Style.STROKE
            pathEffect = CornerPathEffect(12f)
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until pointsCount) {
                lineTo(points[i].x, points[i].y)
            }
        }
        canvas.drawPath(path, paintChartLine)

        // Draw points and X Labels
        val paintPoint = Paint().apply {
            color = if (isDark) Color.parseColor("#F07D7D") else Color.parseColor("#C62828")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val paintTextX = Paint().apply {
            color = chalkWhiteColor
            typeface = fontBody
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in 0 until pointsCount) {
            canvas.drawCircle(points[i].x, points[i].y, 7f, paintPoint)
            if (dayLabels[i].isNotEmpty()) {
                canvas.drawText(dayLabels[i], points[i].x, chartBottom + 35f, paintTextX)
            }
        }

        return bitmap
    }

    private fun getSplitsList(splitsJson: String): List<TransactionSplit> {
        if (splitsJson.isEmpty()) return emptyList()
        return try {
            Json.decodeFromString<List<TransactionSplit>>(splitsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, WeeklyTrendWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
