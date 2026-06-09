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

class RecentTransactionsWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, RecentTransactionsWidgetProvider::class.java)
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
                val sortedList = transactions.sortedByDescending { it.timestamp }.take(4)
                
                val bitmap = createRecentTransactionsBitmap(context, theme, sortedList)

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.recent_transactions_widget).apply {
                        setImageViewBitmap(R.id.widget_image, bitmap)
                        
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pending = PendingIntent.getActivity(
                            context,
                            12,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_image, pending)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createRecentTransactionsBitmap(
        context: Context,
        theme: String,
        recent: List<TransactionEntity>
    ): Bitmap {
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = when (theme) {
            "Dark" -> Color.parseColor("#1E2322")
            "Blueprint" -> Color.parseColor("#0C1B33")
            "Light" -> Color.parseColor("#FAF3E0")
            else -> Color.parseColor("#1B3D2B")
        }

        val paintBg = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        val isDark = theme != "Light"
        val chalkWhiteColor = if (isDark) {
            when (theme) {
                "Blueprint" -> Color.parseColor("#D3E7FF")
                else -> Color.parseColor("#F5F5F5")
            }
        } else {
            Color.parseColor("#4A3B32")
        }

        // Hand-drawn double border
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

        // Title
        val paintTitle = Paint().apply {
            color = chalkWhiteColor
            typeface = fontTitle
            textSize = 38f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Recent Journal", (width / 2).toFloat(), 70f, paintTitle)

        // Separating line
        val paintLine = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(100f, 90f, (width - 100).toFloat(), 90f, paintLine)

        if (recent.isEmpty()) {
            val paintEmpty = Paint().apply {
                color = chalkWhiteColor
                typeface = fontBody
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("No Transactions Yet", (width / 2).toFloat(), 220f, paintEmpty)
            return bitmap
        }

        val startY = 110f
        val rowHeight = 65f
        val paintText = Paint().apply {
            color = chalkWhiteColor
            typeface = fontBody
            textSize = 24f
            isAntiAlias = true
        }
        val paintSubtext = Paint().apply {
            color = chalkWhiteColor
            typeface = fontBody
            textSize = 18f
            alpha = 180
            isAntiAlias = true
        }
        val paintAmount = Paint().apply {
            typeface = fontBody
            textSize = 26f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
            isFakeBoldText = true
        }

        val chalkGreen = if (isDark) Color.parseColor("#8FCE5E") else Color.parseColor("#2D6E4E")
        val chalkRed = if (isDark) Color.parseColor("#F07D7D") else Color.parseColor("#C62828")

        for (i in recent.indices) {
            val tx = recent[i]
            val yPos = startY + (i * rowHeight)

            // Format date: e.g. "Jun 01"
            val dateStr = SimpleDateFormat("MMM dd", Locale.US).format(Date(tx.timestamp))
            canvas.drawText(dateStr, 40f, yPos + 35f, paintSubtext)

            // Note or Category
            val splits = getSplitsList(tx.splits)
            val displayNote = when {
                splits.isNotEmpty() -> "Split Transaction"
                tx.note.isNotEmpty() -> tx.note
                else -> tx.categoryName
            }
            // Truncate note if too long
            val truncatedNote = if (displayNote.length > 22) displayNote.take(20) + ".." else displayNote
            canvas.drawText(truncatedNote, 130f, yPos + 35f, paintText)

            // Amount
            val amountVal = if (splits.isNotEmpty()) splits.sumOf { it.amount } else tx.amount
            val amountStr = if (tx.type == "INCOME") {
                paintAmount.color = chalkGreen
                String.format(Locale.US, "+$%.2f", amountVal)
            } else {
                paintAmount.color = chalkRed
                String.format(Locale.US, "-$%.2f", amountVal)
            }
            canvas.drawText(amountStr, (width - 45).toFloat(), yPos + 35f, paintAmount)

            // Dotted dividing lines between rows
            if (i < recent.size - 1) {
                val paintDivider = Paint().apply {
                    color = chalkWhiteColor
                    strokeWidth = 1f
                    alpha = 80
                    pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                }
                canvas.drawLine(40f, yPos + rowHeight - 2f, (width - 40).toFloat(), yPos + rowHeight - 2f, paintDivider)
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
            val intent = Intent(context, RecentTransactionsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
