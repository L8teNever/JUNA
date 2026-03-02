package bea.l8tenever.com.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Stoppt den laufenden Alarm wenn der Nutzer den "Wecker stoppen" Button drückt.
 */
class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmDismissReceiver", "Alarm gestoppt")
        context.stopService(Intent(context, AlarmService::class.java))
    }
}
