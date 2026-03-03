package bea.l8tenever.com.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import bea.l8tenever.com.data.dataStore
import bea.l8tenever.com.alarm.SleepReminderActivity
import bea.l8tenever.com.alarm.SleepReminderDismissReceiver
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class SleepReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "sleep_reminder_update"
        const val CHANNEL_ID = "bea_sleep_channel"
        const val NOTIFICATION_ID = 2000

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SleepReminderWorker>(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        }
    }

    override suspend fun doWork(): Result {
        try {
            val prefs = applicationContext.dataStore.data.first()
            val mainAlarmTimeStr = prefs[stringPreferencesKey("sleep_main_alarm_time")]
            val startTimeStr = prefs[stringPreferencesKey("sleep_reminder_start_time")]
            val autoDismissMinutes = prefs[intPreferencesKey("sleep_auto_dismiss_minutes")] ?: 30

            if (mainAlarmTimeStr == null || startTimeStr == null) {
                cancel(applicationContext)
                return Result.success()
            }

            val mainAlarmTime = LocalDateTime.parse(mainAlarmTimeStr)
            val startTime = LocalDateTime.parse(startTimeStr)
            val now = LocalDateTime.now()

            // Auto-dismiss check
            val minutesSinceStart = java.time.Duration.between(startTime, now).toMinutes()
            if (minutesSinceStart >= autoDismissMinutes) {
                cancel(applicationContext)
                return Result.success()
            }

            // Calculate sleep hours if user goes to sleep now
            val sleepDuration = java.time.Duration.between(now, mainAlarmTime)
            val sleepHours = sleepDuration.toHours()
            val sleepMinutes = sleepDuration.toMinutes() % 60

            showNotification(sleepHours.toInt(), sleepMinutes.toInt())

            return Result.retry()
        } catch (e: Exception) {
            Log.e("SleepReminderWorker", "Error", e)
            return Result.retry()
        }
    }

    private fun showNotification(hours: Int, minutes: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Schlaf-Erinnerung",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Erinnert dich ans Schlafen gehen"
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)

        val intent = Intent(applicationContext, SleepReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(applicationContext, SleepReminderDismissReceiver::class.java)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (hours > 0) {
            "Du bekommst $hours Std. ${minutes} Min. Schlaf wenn du jetzt schläfst"
        } else {
            "Du bekommst $minutes Min. Schlaf wenn du jetzt schläfst"
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌙 Zeit ins Bett zu gehen!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .addAction(0, "Ich gehe schlafen", dismissPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        nm.notify(NOTIFICATION_ID, builder.build())
    }
}
