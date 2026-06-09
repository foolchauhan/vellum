package com.example.vellum

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.example.vellum.data.local.VellumDatabase
import com.example.vellum.data.local.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickAddWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, QuickAddWidgetProvider::class.java)
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

                val allCategories = db.categoryDao().getAllActiveCategories().filter { it.type == "EXPENSE" }

                appWidgetIds.forEach { widgetId ->
                    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                    val saved = prefs.getString("quick_add_categories_$widgetId", null)

                    val chosenCategories = if (saved != null) {
                        val ids = saved.split(",")
                        ids.mapNotNull { id -> allCategories.find { it.id == id } }
                    } else {
                        allCategories.take(4)
                    }

                    val views = RemoteViews(context.packageName, R.layout.quick_add_widget).apply {
                        val buttonIds = listOf(R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5)

                        // 1. Set categories buttons
                        for (i in buttonIds.indices) {
                            val btnId = buttonIds[i]
                            if (i < chosenCategories.size) {
                                val cat = chosenCategories[i]
                                val bitmap = createButtonBitmap(context, theme, cat.name, cat.icon)
                                
                                setViewVisibility(btnId, View.VISIBLE)
                                setImageViewBitmap(btnId, bitmap)
                                setOnClickPendingIntent(btnId, getPendingIntent(context, cat.name, widgetId * 10 + i))
                            } else {
                                setViewVisibility(btnId, View.GONE)
                            }
                        }

                        // 2. Set settings button
                        val settingsBitmap = createButtonBitmap(context, theme, "Settings", "⚙️")
                        setViewVisibility(R.id.btn_settings, View.VISIBLE)
                        setImageViewBitmap(R.id.btn_settings, settingsBitmap)
                        
                        val intentConfigure = Intent(context, QuickAddWidgetConfigureActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            data = android.net.Uri.parse("widget://configure/quick_add/$widgetId")
                        }
                        val pendingConfigure = PendingIntent.getActivity(
                            context,
                            widgetId * 10 + 9,
                            intentConfigure,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.btn_settings, pendingConfigure)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getPendingIntent(context: Context, categoryName: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("quick_add_category", categoryName)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createButtonBitmap(context: Context, theme: String, label: String, icon: String): Bitmap {
        val size = 150
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
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
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paintBg)

        val isDark = theme != "Light" && theme != "Cement" && theme != "Glass"
        val chalkWhiteColor = if (isDark) {
            when (theme) {
                "Blueprint" -> Color.parseColor("#D3E7FF")
                else -> Color.parseColor("#F5F5F5")
            }
        } else {
            Color.parseColor("#4A3B32")
        }

        val paintBorder = Paint().apply {
            color = chalkWhiteColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
            pathEffect = CornerPathEffect(6f)
            isAntiAlias = true
        }
        canvas.drawRect(6f, 6f, size - 6f, size - 6f, paintBorder)

        val fontBody = try {
            ResourcesCompat.getFont(context, R.font.patrick_hand)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val paintIcon = Paint().apply {
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(icon, (size / 2).toFloat(), 65f, paintIcon)

        val paintText = Paint().apply {
            color = chalkWhiteColor
            typeface = fontBody
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(label, (size / 2).toFloat(), 115f, paintText)

        return bitmap
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, QuickAddWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
