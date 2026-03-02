package bea.l8tenever.com.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import bea.l8tenever.com.data.dataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Wird aufgerufen wenn der Nutzer die Live-Stunden-Notification wegwischt.
 * Startet den LiveStundeWorker sofort neu, damit die Notification direkt wieder erscheint.
 */
class NotificationDismissReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val isDismissible = intent.getBooleanExtra("is_dismissible", false)
        if (isDismissible) {
            Log.d("NotificationDismissReceiver", "Notification (Schule aus/Frei) wurde weggewischt – wird heute nicht mehr gezeigt")
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            GlobalScope.launch {
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey("live_notification_dismissed_date")] = todayStr
                }
            }
        } else {
            Log.d("NotificationDismissReceiver", "Notification wurde weggewischt – wird sofort neu gesetzt")
            val immediateWork = OneTimeWorkRequestBuilder<LiveStundeWorker>().build()
            WorkManager.getInstance(context).enqueue(immediateWork)
        }
    }
}
