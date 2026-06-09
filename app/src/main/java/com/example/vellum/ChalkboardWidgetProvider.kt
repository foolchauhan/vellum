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
import java.util.*

class ChalkboardWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, ChalkboardWidgetProvider::class.java)
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
                val transactions = db.transactionDao().getAllActiveTransactions()
                val theme = try {
                    db.preferenceDao().getPreferenceValue("theme") ?: "Greenboard"
                } catch (e: Exception) {
                    "Greenboard"
                }

                val accountsList = db.accountDao().getAllActiveAccounts()

                appWidgetIds.forEach { widgetId ->
                    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                    val accountId = prefs.getString("chalkboard_account_$widgetId", "all") ?: "all"
                    val period = prefs.getString("chalkboard_period_$widgetId", "daily") ?: "daily"

                    val filteredTransactions = if (accountId == "all") {
                        transactions
                    } else {
                        transactions.filter { it.accountId == accountId }
                    }

                    val (expenseSum, balance) = calculateWidgetMetrics(filteredTransactions, period)
                    
                    val titleText = if (accountId == "all") {
                        "Vellum Finances"
                    } else {
                        accountsList.find { it.id == accountId }?.name ?: "Vellum Finances"
                    }

                    val bitmap = createChalkboardBitmap(context, theme, expenseSum, balance, titleText, period)

                    val views = RemoteViews(context.packageName, R.layout.chalkboard_widget).apply {
                        setImageViewBitmap(R.id.widget_image, bitmap)

                        // Click to open main app
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pending = PendingIntent.getActivity(
                            context,
                            widgetId * 20,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_image, pending)

                        // Settings gear click
                        val intentConfigure = Intent(context, ChalkboardWidgetConfigureActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            data = android.net.Uri.parse("widget://configure/chalkboard/$widgetId")
                        }
                        val pendingConfigure = PendingIntent.getActivity(
                            context,
                            widgetId * 20 + 2,
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

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, ChalkboardWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }

        private fun calculateWidgetMetrics(transactions: List<TransactionEntity>, period: String): Pair<Double, Double> {
            var totalBalance = 0.0
            var expenses = 0.0

            transactions.forEach { tx ->
                if (tx.isDeleted) return@forEach

                val splits = getSplitsList(tx.splits)
                val txAmount = if (splits.isNotEmpty()) splits.sumOf { it.amount } else tx.amount
                
                if (tx.type == "INCOME") {
                    totalBalance += txAmount
                } else {
                    totalBalance -= txAmount
                    
                    val matchPeriod = when (period) {
                        "daily" -> isToday(tx.timestamp)
                        "weekly" -> isCurrentWeek(tx.timestamp)
                        "monthly" -> isCurrentMonth(tx.timestamp)
                        "yearly" -> isCurrentYear(tx.timestamp)
                        else -> true // all_time
                    }

                    if (matchPeriod) {
                        expenses += txAmount
                    }
                }
            }
            return Pair(expenses, totalBalance)
        }

        private fun getSplitsList(splitsJson: String): List<TransactionSplit> {
            if (splitsJson.isEmpty()) return emptyList()
            return try {
                Json.decodeFromString<List<TransactionSplit>>(splitsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun isToday(timestamp: Long): Boolean {
            val calToday = Calendar.getInstance()
            val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
            return calToday.get(Calendar.YEAR) == calTx.get(Calendar.YEAR) &&
                   calToday.get(Calendar.DAY_OF_YEAR) == calTx.get(Calendar.DAY_OF_YEAR)
        }

        private fun isCurrentWeek(timestamp: Long): Boolean {
            val calNow = Calendar.getInstance()
            val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
            return calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR) &&
                   calNow.get(Calendar.WEEK_OF_YEAR) == calTx.get(Calendar.WEEK_OF_YEAR)
        }

        private fun isCurrentMonth(timestamp: Long): Boolean {
            val calNow = Calendar.getInstance()
            val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
            return calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR) &&
                   calNow.get(Calendar.MONTH) == calTx.get(Calendar.MONTH)
        }

        private fun isCurrentYear(timestamp: Long): Boolean {
            val calNow = Calendar.getInstance()
            val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
            return calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR)
        }

        private fun createChalkboardBitmap(
            context: Context,
            theme: String,
            expenseSum: Double,
            balance: Double,
            titleText: String,
            period: String
        ): Bitmap {
            val width = 600
            val height = 400
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgColor = when (theme) {
                "Dark" -> Color.parseColor("#1E2322") // slate
                "Blueprint" -> Color.parseColor("#0C1B33") // blueprint
                "Light" -> Color.parseColor("#FAF3E0") // warm parchment
                "Cement" -> Color.parseColor("#EFEFEF")
                "Glass" -> Color.parseColor("#D0E6F8")
                else -> Color.parseColor("#1B3D2B") // greenboard
            }
            
            val paintBg = Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

            val isDark = theme != "Light" && theme != "Cement" && theme != "Glass"

            // Chalk borders
            val chalkWhiteColor = if (isDark) {
                when (theme) {
                    "Blueprint" -> Color.parseColor("#D3E7FF")
                    else -> Color.parseColor("#F5F5F5")
                }
            } else {
                Color.parseColor("#4A3B32") // Ink color for parchment
            }

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

            // Colors
            val chalkSecondaryColor = if (isDark) {
                when (theme) {
                    "Blueprint" -> Color.parseColor("#87CEEB")
                    else -> Color.parseColor("#8FCE5E") // chalk green
                }
            } else {
                Color.parseColor("#2D6E4E")
            }
            
            val chalkAlertColor = Color.parseColor("#D32F2F") // chalk red

            // Header Title
            val paintTitle = Paint().apply {
                color = chalkWhiteColor
                typeface = fontTitle
                textSize = 44f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(titleText, (width / 2).toFloat(), 80f, paintTitle)

            // Separator line
            val paintLine = Paint().apply {
                color = chalkWhiteColor
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawLine(100f, 105f, (width - 100).toFloat(), 105f, paintLine)

            // Spending Label
            val paintLabel = Paint().apply {
                color = chalkWhiteColor
                typeface = fontBody
                textSize = 30f
                isAntiAlias = true
                alpha = if (isDark) 200 else 255
            }
            
            val paintAmount = Paint().apply {
                typeface = fontBody
                textSize = 40f
                isAntiAlias = true
                isFakeBoldText = true
            }

            val expenseLabel = when (period) {
                "daily" -> "Today's Spend"
                "weekly" -> "Weekly Spend"
                "monthly" -> "Monthly Spend"
                "yearly" -> "Yearly Spend"
                else -> "All Time Spend"
            }

            canvas.drawText(expenseLabel, 60f, 170f, paintLabel)
            
            paintAmount.color = chalkAlertColor
            canvas.drawText(String.format("$%.2f", expenseSum), 60f, 220f, paintAmount)

            // Balance
            canvas.drawText("Current Balance", 60f, 290f, paintLabel)
            
            paintAmount.color = if (balance >= 0) chalkSecondaryColor else chalkAlertColor
            canvas.drawText(String.format("$%.2f", balance), 60f, 340f, paintAmount)

            // Decorative graph
            val paintChart = Paint().apply {
                color = chalkWhiteColor
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val path = Path().apply {
                moveTo(400f, 320f)
                lineTo(440f, 260f)
                lineTo(480f, 280f)
                lineTo(520f, 210f)
                lineTo(560f, 230f)
            }
            canvas.drawPath(path, paintChart)

            // Chalk sparkle
            val paintSparkle = Paint().apply {
                color = chalkSecondaryColor
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(520f, 190f, 520f, 200f, paintSparkle)
            canvas.drawLine(515f, 195f, 525f, 195f, paintSparkle)

            // Axes
            val paintAxes = Paint().apply {
                color = chalkWhiteColor
                strokeWidth = 2f
                alpha = if (isDark) 150 else 255
            }
            canvas.drawLine(380f, 340f, 560f, 340f, paintAxes)
            canvas.drawLine(380f, 180f, 380f, 340f, paintAxes)

            return bitmap
        }
    }
}
