package bea.l8tenever.com.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import bea.l8tenever.com.data.CustomAlarm
import bea.l8tenever.com.data.PrefsKeys
import bea.l8tenever.com.data.TimetableEntry
import bea.l8tenever.com.data.dataStore
import bea.l8tenever.com.worker.TimetableWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Nach einem Geräte-Neustart wird der Alarm neu gesetzt.
 * Startet auch den WorkManager neu.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )
        if (intent.action !in validActions) return
        Log.d("BootReceiver", "Gerät neugestartet (${intent.action}) – stelle Alarm wieder her...")

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.dataStore.data.first()
            val enabled = prefs[PrefsKeys.ALARM_ENABLED] != "false"
            val minutes = prefs[PrefsKeys.ALARM_MINUTES] ?: 60
            val disabledDates = prefs[PrefsKeys.ALARM_DISABLED_DATES] ?: emptySet()

            // Custom-Alarms laden
            val customAlarmsJson = prefs[PrefsKeys.CUSTOM_ALARMS]
            val customAlarms: List<CustomAlarm> = if (customAlarmsJson.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<CustomAlarm>>() {}.type
                    Gson().fromJson(customAlarmsJson, type) ?: emptyList()
                } catch (e: Exception) {
                    Log.w("BootReceiver", "Custom-Alarms konnten nicht geladen werden: ${e.message}")
                    emptyList()
                }
            }

            // Lokalen Cache auslesen & Alarm direkt stellen (selbst ohne Internet!)
            val cachedJson = prefs[stringPreferencesKey("cached_timetable")]
            if (!cachedJson.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<TimetableEntry>>() {}.type
                    val entries: List<TimetableEntry> = Gson().fromJson(cachedJson, type)

                    AlarmScheduler.scheduleNextAlarm(
                        context       = context,
                        timetable     = entries,
                        minutesBefore = minutes,
                        enabled       = enabled,
                        disabledDates = disabledDates,
                        customAlarms  = customAlarms
                    )
                    Log.d("BootReceiver", "Alarm erfolgreich neu gesetzt (${customAlarms.size} Custom-Alarms)")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Konnte gecachten Stundenplan für Alarm nicht laden", e)
                }
            }

            // WorkManager für Stundenplan-Updates neu starten
            TimetableWorker.schedule(context)
            Log.d("BootReceiver", "WorkManager neu gestartet.")
        }
    }
}
