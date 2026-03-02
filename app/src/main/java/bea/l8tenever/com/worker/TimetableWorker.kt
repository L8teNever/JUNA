package bea.l8tenever.com.worker

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.*
import bea.l8tenever.com.alarm.AlarmScheduler
import bea.l8tenever.com.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit



/**
 * WorkManager Worker – läuft alle 30 Minuten im Hintergrund.
 * Loggt sich bei Untis ein, lädt den Stundenplan
 * und berechnet den nächsten Wecker neu.
 */
class TimetableWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "TimetableWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "=== Stundenplan-Sync gestartet ===")

        // Prefs laden
        val prefs     = applicationContext.dataStore.data.first()
        val serverUrl = prefs[PrefsKeys.SERVER_URL]    ?: return Result.success()
        val school    = prefs[PrefsKeys.SCHOOL]        ?: return Result.success()
        val username  = prefs[PrefsKeys.USERNAME]      ?: return Result.success()
        val password  = prefs[PrefsKeys.PASSWORD]      ?: return Result.success()
        var klasseId  = prefs[PrefsKeys.KLASSE_ID]     ?: 0
        val enabled   = prefs[PrefsKeys.ALARM_ENABLED] != "false"
        val minutes   = prefs[PrefsKeys.ALARM_MINUTES] ?: 60

        if (serverUrl.isBlank() || username.isBlank()) {
            Log.d(TAG, "Keine Credentials → überspringe")
            return Result.success()
        }

        val creds = LoginCredentials(serverUrl, school, username, password)
        val repo  = UntisRepository()

        // 1. Einloggen
        val loginResult = repo.login(creds)
        if (loginResult is UntisResult.Error) {
            Log.e(TAG, "Login-Fehler: ${loginResult.message}")
            return Result.retry()
        }
        val session   = (loginResult as UntisResult.Success).data
        val sessionId = session.sessionId ?: return Result.retry()
        if (klasseId == 0) klasseId = session.klasseId ?: 0
        if (klasseId == 0) {
            Log.e(TAG, "Keine Klassen-ID → Abbruch")
            return Result.failure()
        }

        val personId = session.personId ?: 0

        // Session speichern
        val userPrefs = UserPreferences(applicationContext)
        userPrefs.saveSession(sessionId, klasseId, personId)

        // 2. Stundenplan laden (heute + 14 Tage)
        val today = LocalDate.now()
        val timetableResult = repo.getTimetable(
            creds     = creds,
            sessionId = sessionId,
            klasseId  = klasseId,
            personId  = personId,
            startDate = today,
            endDate   = today.plusDays(14)
        )

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
                Log.w(TAG, "Custom-Alarms konnten nicht geladen werden: ${e.message}")
                emptyList()
            }
        }

            // Templates laden
            val templatesJson = prefs[PrefsKeys.TEMPLATES]
            val templates: List<AlarmTemplate> = if (templatesJson.isNullOrBlank()) emptyList() else {
                try { val t = object : TypeToken<List<AlarmTemplate>>() {}.type; Gson().fromJson(templatesJson, t) ?: emptyList() } catch (_: Exception) { emptyList() }
            }

            // OneTimeTemplate laden
            val ottJson = prefs[PrefsKeys.ONE_TIME_TEMPLATE]
            val oneTimeTemplate: OneTimeTemplate? = if (ottJson.isNullOrBlank()) null else {
                try { Gson().fromJson(ottJson, OneTimeTemplate::class.java) } catch (_: Exception) { null }
            }

            return if (timetableResult is UntisResult.Success) {
                val entries = timetableResult.data
                Log.d(TAG, "Sync erfolgreich: ${entries.size} Stunden, ${customAlarms.size} Custom-Alarms, ${templates.size} Templates")

                // Cache speichern
                cacheTimetable(entries)

                // Alarm neu berechnen – mit allen Parametern!
                AlarmScheduler.scheduleNextAlarm(
                    context         = applicationContext,
                    timetable       = entries,
                    minutesBefore   = minutes,
                    enabled         = enabled,
                    disabledDates   = disabledDates,
                    customAlarms    = customAlarms,
                    templates       = templates,
                    oneTimeTemplate = oneTimeTemplate
                )
                Result.success()
            } else {
                val err = (timetableResult as UntisResult.Error).message
                Log.e(TAG, "Stundenplan-Fehler: $err")
                Result.retry()
            }
        }

    private suspend fun cacheTimetable(entries: List<TimetableEntry>) {
        // Gson für korrektes Escaping aller Sonderzeichen verwenden
        val json = Gson().toJson(entries)
        applicationContext.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("cached_timetable")] = json
        }
    }


    companion object {
        const val WORK_NAME = "timetable_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TimetableWorker>(
                60, TimeUnit.MINUTES // Erhöht auf 60 für mehr Energieeffizienz
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true) // Akkuschonend
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d("TimetableWorker", "60-Min-Sync (Akkuschonend) geplant ✓")
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<TimetableWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_now",
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d("TimetableWorker", "Sofort-Sync geplant ✓")
        }
    }
}
