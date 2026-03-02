package bea.l8tenever.com.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.*
import bea.l8tenever.com.MainActivity
import bea.l8tenever.com.R
import bea.l8tenever.com.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import android.os.Build

/**
 * Worker, der eine "Sticky Notification" mit der aktuellen Schulstunde anzeigt.
 */
class LiveStundeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "LiveStundeWorker"
        const val WORK_NAME = "live_stunde_update"
        const val CHANNEL_ID = "bea_live_channel"
        const val NOTIFICATION_ID = 1337

        fun schedule(context: Context, enabled: Boolean) {
            if (!enabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
                return
            }

            val request = PeriodicWorkRequestBuilder<LiveStundeWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            
            // Auch sofort einmal ausführen
            val immediate = OneTimeWorkRequestBuilder<LiveStundeWorker>().build()
            WorkManager.getInstance(context).enqueue(immediate)
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.dataStore.data.first()
        val enabled = prefs[stringPreferencesKey("live_notification_enabled")] == "true"
        val dismissedDate = prefs[stringPreferencesKey("live_notification_dismissed_date")]
        
        if (!enabled) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            return Result.success()
        }

        val alarmMinutes = prefs[androidx.datastore.preferences.core.intPreferencesKey("alarm_minutes")] ?: 60

        val cached = prefs[stringPreferencesKey("cached_timetable")]
        if (cached.isNullOrBlank()) return Result.success()

        val gson = Gson()
        val type = object : TypeToken<List<TimetableEntry>>() {}.type
        val entries: List<TimetableEntry> = gson.fromJson(cached, type)

        val now = LocalTime.now()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val todayEntries = entries.filter { it.date == todayStr }.sortedBy { it.startTime }
        val alreadyDismissedToday = dismissedDate == todayStr

        if (todayEntries.isEmpty()) {
            if (!alreadyDismissedToday) {
                showNotification("Kein Unterricht heute", "Genieß den freien Tag!", isDismissible = true)
            }
            return Result.success()
        }

        // Aktuelle Stunde finden
        val current = todayEntries.find {
            val start = parseTime(it.startTime)
            val end = parseTime(it.endTime)
            !now.isBefore(start) && !now.isAfter(end)
        }

        if (current != null) {
            val end = parseTime(current.endTime)
            showNotification(
                "Aktuell: ${current.subject}",
                "Bis ${current.endTime} in ${current.room}",
                targetTime = end
            )
            return Result.success()
        }

        // Pause oder Nächste Stunde?
        val next = todayEntries.find { parseTime(it.startTime).isAfter(now) }
        if (next != null) {
            val start = parseTime(next.startTime)
            
            // Sonderfall: Wir sind VOR der ersten Stunde
            val isFirstLesson = next == todayEntries.first()
            if (isFirstLesson) {
                val (h, m) = next.startTime.split(":").map { it.toInt() }
                val wakeTime = LocalTime.of(h, m).minusMinutes(alarmMinutes.toLong())
                
                if (now.isBefore(wakeTime)) {
                    // Wir sind noch vor dem Wecker
                    showNotification(
                        "Wecker gestellt: %02d:%02d".format(wakeTime.hour, wakeTime.minute),
                        "Erste Stunde: ${next.subject} in ${next.room}",
                        targetTime = wakeTime
                    )
                } else {
                    // Wir sind zwischen Wecker und Schulbeginn
                    showNotification(
                        "Nächste Stunde: ${next.subject}",
                        "Beginnt um ${next.startTime} in ${next.room}",
                        targetTime = start
                    )
                }
            } else {
                // Reguläre Pause zwischen Stunden
                val last = todayEntries.lastOrNull { parseTime(it.endTime).isBefore(now) }
                if (last != null && java.time.Duration.between(parseTime(last.endTime), now).toMinutes() < 30) {
                    showNotification(
                        "Pause ☕", 
                        "Nächste Stunde: ${next.subject} um ${next.startTime}",
                        targetTime = start
                    )
                } else {
                    showNotification(
                        "Nächste Stunde: ${next.subject}",
                        "Um ${next.startTime} in ${next.room}",
                        targetTime = start
                    )
                }
            }
        } else {
            if (!alreadyDismissedToday) {
                showNotification("Schule aus! 🎉", "Alle Stunden für heute sind beendet.", isDismissible = true)
            }
        }

        return Result.success()
    }

    private fun parseTime(time: String): LocalTime {
        val (h, m) = time.split(":").map { it.toInt() }
        return LocalTime.of(h, m)
    }

    private fun showNotification(title: String, text: String, targetTime: LocalTime? = null, isDismissible: Boolean = false) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(CHANNEL_ID, "YUNA Live", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Zeigt die verbleibende Zeit der aktuellen Schulstunde an"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent für den Fall dass die Notification doch weggewischt wird
        // → direkt wieder anzeigen ODER Flag setzen (wenn dismissible)
        val dismissIntent = Intent(applicationContext, NotificationDismissReceiver::class.java).apply {
            putExtra("is_dismissible", isDismissible)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            if (isDismissible) 1 else 0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Neues Logo
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isDismissible)               // Nur nicht wegwischbar wenn nicht dismissible
            .setAutoCancel(false)           // Nicht beim Antippen schließen
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Falls weggewischt -> Receiver triggern

        if (targetTime != null) {
            val now = LocalDateTime.now()
            var targetDateTime = LocalDateTime.of(now.toLocalDate(), targetTime)
            
            // Falls die Zielzeit heute schon vorbei ist, meinen wir wahrscheinlich morgen
            if (targetDateTime.isBefore(now)) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            
            val millis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            builder.setWhen(millis)
                .setUsesChronometer(true)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setChronometerCountDown(true)
            }
        }

        nm.notify(NOTIFICATION_ID, builder.build())
    }
}
