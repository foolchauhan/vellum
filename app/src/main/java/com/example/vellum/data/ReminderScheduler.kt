package com.example.vellum.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "vellum_reminders"
    const val WORK_NAME = "VellumReminderWork"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminders"
            val descriptionText = "Notifications to remind you to log your daily transactions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, frequency: String) {
        val workManager = WorkManager.getInstance(context)
        
        if (frequency == "Off") {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        // Calculate delay to next 8:00 PM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var delay = calendar.timeInMillis - System.currentTimeMillis()
        if (delay < 0) {
            delay += 24 * 60 * 60 * 1000L // 8:00 PM tomorrow
        }

        val request = OneTimeWorkRequest.Builder(ReminderWorker::class.java)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNextReminder(context: Context, frequency: String) {
        val workManager = WorkManager.getInstance(context)
        if (frequency == "Off") return

        val delayDays = when (frequency) {
            "Daily" -> 1L
            "Every Week" -> 7L
            "Monthly" -> 30L
            else -> 1L
        }

        val delayMs = delayDays * 24L * 60L * 60L * 1000L

        val request = OneTimeWorkRequest.Builder(ReminderWorker::class.java)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
