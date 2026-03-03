package bea.l8tenever.com.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import bea.l8tenever.com.data.dataStore
import bea.l8tenever.com.worker.SleepReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepReminderDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SleepReminderDismiss", "User dismissed sleep reminder")
        SleepReminderWorker.cancel(context)

        // Clear stored state
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey("sleep_main_alarm_time"))
                prefs.remove(stringPreferencesKey("sleep_reminder_start_time"))
            }
        }
    }
}
