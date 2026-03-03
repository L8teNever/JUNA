package bea.l8tenever.com.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import bea.l8tenever.com.data.dataStore
import bea.l8tenever.com.worker.SleepReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class BedtimeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BedtimeAlarmReceiver", "Bedtime alarm triggered!")

        val mainAlarmTime = intent.getStringExtra("main_alarm_time") ?: return
        val sleepHours = intent.getFloatExtra("sleep_hours", 8.0f)
        val fullscreenEnabled = intent.getBooleanExtra("fullscreen_enabled", true)
        val vibrateOnly = intent.getBooleanExtra("vibrate_only", false)
        val autoDismissMinutes = intent.getIntExtra("auto_dismiss_minutes", 30)

        // Store state for SleepReminderWorker
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("sleep_main_alarm_time")] = mainAlarmTime
                prefs[stringPreferencesKey("sleep_reminder_start_time")] = LocalDateTime.now().toString()
                prefs[intPreferencesKey("sleep_auto_dismiss_minutes")] = autoDismissMinutes
            }
        }

        // Start persistent notification worker
        SleepReminderWorker.schedule(context)

        // If fullscreen mode enabled, show fullscreen activity
        if (fullscreenEnabled) {
            val activityIntent = Intent(context, SleepReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("vibrate_only", vibrateOnly)
            }
            context.startActivity(activityIntent)
        }
    }
}
