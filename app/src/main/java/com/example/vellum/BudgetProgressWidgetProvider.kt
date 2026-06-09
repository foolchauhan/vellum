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
import com.example.vellum.data.local.CategoryEntity
import com.example.vellum.data.local.TransactionEntity
import com.example.vellum.data.local.TransactionSplit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.*

class BudgetProgressWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, BudgetProgressWidgetProvider::class.java)
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
                
                val categories = db.categoryDao().getAllActiveCategories()
                val budgetedCategories = categories.filter { it.budget > 0.0 }
                val transactions = db.transactionDao().getAllActiveTransactions()
                
                val bitmap = createBudgetProgressBitmap(context, theme, budgetedCategories, transactions)

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.budget_progress_widget).apply {
                        setImageViewBitmap(R.id.widget_image, bitmap)
                        
                        // Click widget to launch main app
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pending = PendingIntent.getActivity(
                            context,
                            10,
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

    private fun createBudgetProgressBitmap(
        context: Context,
        theme: String,
        budgeted: List<CategoryEntity>,
        transactions: List<TransactionEntity>
    ): Bitmap {
        val width = 400
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

        // Hand drawn border lines
        val paintBorder = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 5f
            style = Paint.Style.STROKE
            pathEffect = CornerPathEffect(8f)
            isAntiAlias = true
        }
        canvas.drawRect(12f, 12f, width - 12f, height - 12f, paintBorder)

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
            textSize = 34f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Envelope Budgets", (width / 2).toFloat(), 60f, paintTitle)

        // Draw separating line
        val paintLine = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(80f, 80f, (width - 80).toFloat(), 80f, paintLine)

        if (budgeted.isEmpty()) {
            val paintEmpty = Paint().apply {
                color = chalkWhiteColor
                typeface = fontBody
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("No Budgets Active", (width / 2).toFloat(), 210f, paintEmpty)
            canvas.drawText("Tap to set budgets in app!", (width / 2).toFloat(), 260f, paintEmpty)
            return bitmap
        }

        // Calculate current month's spent for each category
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val spentMap = mutableMapOf<String, Double>()

        transactions.forEach { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                val splits = getSplitsList(tx.splits)
                if (splits.isNotEmpty()) {
                    splits.forEach { split ->
                        spentMap[split.categoryId] = (spentMap[split.categoryId] ?: 0.0) + split.amount
                    }
                } else {
                    spentMap[tx.categoryId] = (spentMap[tx.categoryId] ?: 0.0) + tx.amount
                }
            }
        }

        val limit = minOf(budgeted.size, 3)
        val itemHeight = 90f
        val startY = 110f

        val chalkGreenColor = if (isDark) Color.parseColor("#8FCE5E") else Color.parseColor("#2D6E4E")
        val chalkRedColor = Color.parseColor("#F07D7D")

        for (i in 0 until limit) {
            val cat = budgeted[i]
            val budget = cat.budget
            val spent = spentMap[cat.id] ?: 0.0
            val remaining = maxOf(0.0, budget - spent)
            val progress = minOf(1.0, spent / budget).toFloat()

            val yPos = startY + (i * itemHeight)

            // Category name
            val paintCatName = Paint().apply {
                color = chalkWhiteColor
                typeface = fontBody
                textSize = 24f
                isAntiAlias = true
            }
            canvas.drawText(cat.name, 40f, yPos + 30f, paintCatName)

            // Progress text: e.g. "$120 of $200"
            val paintProgressText = Paint().apply {
                color = chalkWhiteColor
                typeface = fontBody
                textSize = 22f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            val progressStr = String.format(Locale.US, "$%.0f / $%.0f", spent, budget)
            canvas.drawText(progressStr, (width - 40).toFloat(), yPos + 30f, paintProgressText)

            // Draw progress bar outline (hand drawn look)
            val barLeft = 40f
            val barTop = yPos + 45f
            val barRight = (width - 40).toFloat()
            val barBottom = yPos + 68f
            
            val paintBarOutline = Paint().apply {
                color = chalkWhiteColor
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRect(barLeft, barTop, barRight, barBottom, paintBarOutline)

            // Draw filled progress
            if (progress > 0) {
                val fillRight = barLeft + (progress * (barRight - barLeft))
                val paintBarFill = Paint().apply {
                    color = if (spent > budget) chalkRedColor else chalkGreenColor
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(barLeft + 3f, barTop + 3f, fillRight - 3f, barBottom - 3f, paintBarFill)
            }
        }

        // Draw remaining envelope summary at bottom
        val totalBudget = budgeted.sumOf { it.budget }
        val totalSpent = budgeted.sumOf { spentMap[it.id] ?: 0.0 }
        val overallRemaining = maxOf(0.0, totalBudget - totalSpent)

        val paintSummary = Paint().apply {
            color = chalkWhiteColor
            typeface = fontBody
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val summaryText = String.format(Locale.US, "Total Remaining: $%.2f", overallRemaining)
        canvas.drawText(summaryText, (width / 2).toFloat(), 370f, paintSummary)

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
            val intent = Intent(context, BudgetProgressWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
