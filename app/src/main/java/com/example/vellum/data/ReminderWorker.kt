package com.example.vellum.data

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vellum.MainActivity
import com.example.vellum.data.local.VellumDatabase

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        
        // Retrieve database and preferences
        val db = VellumDatabase.getInstance(context)
        val remindersSetting = db.preferenceDao().getPreferenceValue("reminders") ?: "Off"

        if (remindersSetting == "Off") {
            return Result.success()
        }

        // Build notification intent to launch app on click
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(com.example.vellum.R.mipmap.ic_launcher)
            .setContentTitle("Vellum Chalkboard Update")
            .setContentText("Time to log your spending! Keep your chalkboard updated.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check permission (on API 33+)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(1001, notification)
        }

        // Schedule next reminder
        ReminderScheduler.scheduleNextReminder(context, remindersSetting)

        return Result.success()
    }
}
