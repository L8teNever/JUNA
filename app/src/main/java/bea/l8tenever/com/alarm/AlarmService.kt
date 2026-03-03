package bea.l8tenever.com.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import bea.l8tenever.com.MainActivity
import bea.l8tenever.com.R

/**
 * Foreground Service der den Alarm-Sound abspielt und eine Benachrichtigung anzeigt.
 * Läuft auch wenn die App im Hintergrund ist.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "bea_alarm_channel"
        const val NOTIFICATION_ID = 42
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val subject = intent?.getStringExtra("lesson_subject") ?: "Unterricht"
        val time    = intent?.getStringExtra("lesson_time")    ?: ""
        val title   = intent?.getStringExtra("alarm_title")    ?: "⏰ Zeit aufzustehen!"
        val showWeather = intent?.getBooleanExtra("show_weather", false) ?: false

        Log.d("AlarmService", "Alarm gestartet: $title / $subject um $time (Wetter: $showWeather)")

        val notification = buildNotification(subject, time, title, showWeather)
        startForeground(NOTIFICATION_ID, notification)

        playAlarmSound()
        startVibration()

        return START_NOT_STICKY
    }

    private fun playAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Fehler beim Sound-Abspielen", e)
        }
    }

    private fun startVibration() {
        try {
            val vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(vibrationPattern, 0)
            )
        } catch (e: Exception) {
            Log.e("AlarmService", "Fehler bei Vibration", e)
        }
    }

    private fun buildNotification(subject: String, time: String, title: String, showWeather: Boolean): Notification {
        // Fullscreen Intent: Öffnet die AlarmActivity über dem Sperrbildschirm
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("lesson_subject", subject)
            putExtra("lesson_time", time)
            putExtra("alarm_title", title)
            putExtra("show_weather", showWeather)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingFullScreenIntent = PendingIntent.getActivity(
            this, 2, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java)
        val dismissPending = PendingIntent.getBroadcast(
            this, 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $title")
            .setContentText("$subject startet um $time Uhr")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title: Dein $subject beginnt um $time Uhr.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, "Wecker stoppen", dismissPending)
            .setAutoCancel(false)
            .setOngoing(true)

        // Ab Android 10+ ist FullScreenIntent streng reguliert. Die Activity ploppt nur auf, wenn das Gerät gesperrt ist.
        builder.setFullScreenIntent(pendingFullScreenIntent, true)

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "YUNA Wecker",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Wecker-Benachrichtigung von YUNA"
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
