package bea.l8tenever.com.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Wird vom AlarmManager ausgelöst wenn der Wecker klingeln soll.
 * Startet den AlarmService der den Sound abspielt.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm ausgelöst!")

        val subject = intent.getStringExtra("lesson_subject") ?: "Unterricht"
        val time    = intent.getStringExtra("lesson_time")    ?: ""
        val date    = intent.getStringExtra("lesson_date")    ?: ""
        val title   = intent.getStringExtra("alarm_title")    ?: "⏰ Zeit aufzustehen!"
        val showWeather = intent.getBooleanExtra("show_weather", false)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("lesson_subject", subject)
            putExtra("lesson_time", time)
            putExtra("lesson_date", date)
            putExtra("alarm_title", title)
            putExtra("show_weather", showWeather)
        }

        context.startForegroundService(serviceIntent)
    }
}
