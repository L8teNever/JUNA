package bea.l8tenever.com.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bea_prefs")

object PrefsKeys {
    val SERVER_URL    = stringPreferencesKey("server_url")
    val SCHOOL        = stringPreferencesKey("school")
    val USERNAME      = stringPreferencesKey("username")
    val PASSWORD      = stringPreferencesKey("password")
    val SESSION_ID    = stringPreferencesKey("session_id")
    val KLASSE_ID     = intPreferencesKey("klasse_id")
    val PERSON_ID     = intPreferencesKey("person_id")
    val ALARM_MINUTES = intPreferencesKey("alarm_minutes")
    val ALARM_ENABLED = stringPreferencesKey("alarm_enabled") // "true"/"false"
    val ALARM_DISABLED_DATES = stringSetPreferencesKey("alarm_disabled_dates") // yyyy-MM-dd
    val CUSTOM_ALARMS = stringPreferencesKey("custom_alarms")
    val HABITS        = stringPreferencesKey("habits")
    val HABIT_LOGS    = stringPreferencesKey("habit_logs")
    val TEMPLATES     = stringPreferencesKey("alarm_templates")    // JSON-Liste von AlarmTemplate
    val ONE_TIME_TEMPLATE = stringPreferencesKey("one_time_template") // JSON von OneTimeTemplate?
    val WAS_LIVE_NOTIFICATION_BEFORE_DISABLE = stringPreferencesKey("was_live_notification_before_disable") // "true"/"false"

    // Sleep Tracker Einstellungen
    val SLEEP_ENABLED = stringPreferencesKey("sleep_enabled") // "true"/"false"
    val SLEEP_HOURS = stringPreferencesKey("sleep_hours") // "8.0"
    val SLEEP_FULLSCREEN = stringPreferencesKey("sleep_fullscreen") // "true"/"false"
    val SLEEP_VIBRATE_ONLY = stringPreferencesKey("sleep_vibrate_only") // "true"/"false"
    val SLEEP_AUTO_DISMISS_MINUTES = intPreferencesKey("sleep_auto_dismiss_minutes") // 30
    val SLEEP_WAS_ENABLED_BEFORE_DISABLE = stringPreferencesKey("sleep_was_enabled_before_disable") // "true"/"false"

    // Schulmodus-Einstellungen
    val AUTO_DND_ENABLED = stringPreferencesKey("auto_dnd_enabled") // "true"/"false"
    val AUTO_VOLUME_ENABLED = stringPreferencesKey("auto_volume_enabled") // "true"/"false"
    val AUTO_VOLUME_RING = stringPreferencesKey("auto_volume_ring") // "true"/"false"
    val AUTO_VOLUME_NOTIFICATION = stringPreferencesKey("auto_volume_notification") // "true"/"false"
    val AUTO_VOLUME_MEDIA = stringPreferencesKey("auto_volume_media") // "true"/"false"
    val DND_PAUSE_BEHAVIOR = stringPreferencesKey("dnd_pause_behavior") // "deactivate" oder "keep_active"
    val AUTO_DND_NOTIFY = stringPreferencesKey("auto_dnd_notify") // "true"/"false"

    // Theme & Color
    val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
    val USE_DYNAMIC_COLORS = stringPreferencesKey("use_dynamic_colors") // "true"/"false"
    val NEXT_ALARM_DATE = stringPreferencesKey("next_alarm_date")
    val NEXT_ALARM_TIME = stringPreferencesKey("next_alarm_time")
}

class UserPreferences(private val context: Context) {

    val serverUrl: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.SERVER_URL] ?: ""
    }
    val school: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.SCHOOL] ?: ""
    }
    val username: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.USERNAME] ?: ""
    }
    val password: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.PASSWORD] ?: ""
    }
    val sessionId: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.SESSION_ID] ?: ""
    }
    val klasseId: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.KLASSE_ID] ?: 0
    }
    val alarmMinutes: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.ALARM_MINUTES] ?: 60
    }
    val alarmEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.ALARM_ENABLED] != "false"
    }
    val alarmDisabledDates: Flow<Set<String>> = context.dataStore.data.map {
        it[PrefsKeys.ALARM_DISABLED_DATES] ?: emptySet()
    }
    val customAlarms: Flow<List<CustomAlarm>> = context.dataStore.data.map {
        val json = it[PrefsKeys.CUSTOM_ALARMS]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<CustomAlarm>>() {}.type
                com.google.gson.Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val habits: Flow<List<Habit>> = context.dataStore.data.map {
        val json = it[PrefsKeys.HABITS]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<Habit>>() {}.type
                com.google.gson.Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val habitLogs: Flow<List<HabitLog>> = context.dataStore.data.map {
        val json = it[PrefsKeys.HABIT_LOGS]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<HabitLog>>() {}.type
                com.google.gson.Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val autoDndEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.AUTO_DND_ENABLED] != "false"
    }
    val autoVolumeEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.AUTO_VOLUME_ENABLED] != "false"
    }
    val autoVolumeRing: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.AUTO_VOLUME_RING] != "false"
    }
    val autoVolumeNotification: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.AUTO_VOLUME_NOTIFICATION] != "false"
    }
    val autoVolumeMedia: Flow<Boolean> = context.dataStore.data.map {
        (it[PrefsKeys.AUTO_VOLUME_MEDIA] ?: "false") != "false"
    }
    val dndPauseBehavior: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.DND_PAUSE_BEHAVIOR] ?: "deactivate"
    }
    val autoDndNotify: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.AUTO_DND_NOTIFY] != "false"
    }

    val themeMode: Flow<String> = context.dataStore.data.map {
        it[PrefsKeys.THEME_MODE] ?: "system"
    }
    val useDynamicColors: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.USE_DYNAMIC_COLORS] != "false"
    }

    val sleepSettings: Flow<SleepSettings> = context.dataStore.data.map {
        SleepSettings(
            isEnabled = it[PrefsKeys.SLEEP_ENABLED] != "false",
            desiredSleepHours = it[PrefsKeys.SLEEP_HOURS]?.toFloatOrNull() ?: 8.0f,
            showFullscreenReminder = it[PrefsKeys.SLEEP_FULLSCREEN] != "false",
            vibrateOnly = it[PrefsKeys.SLEEP_VIBRATE_ONLY] == "true",
            autoDismissMinutes = it[PrefsKeys.SLEEP_AUTO_DISMISS_MINUTES] ?: 30,
            wasEnabledBeforeMainAlarmDisable =
                if (it[PrefsKeys.SLEEP_WAS_ENABLED_BEFORE_DISABLE] == "true") true else null
        )
    }

    suspend fun saveCredentials(credentials: LoginCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.SERVER_URL] = credentials.serverUrl
            prefs[PrefsKeys.SCHOOL]     = credentials.school
            prefs[PrefsKeys.USERNAME]   = credentials.username
            prefs[PrefsKeys.PASSWORD]   = credentials.password
        }
    }

    suspend fun saveSession(sessionId: String, klasseId: Int, personId: Int) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.SESSION_ID] = sessionId
            prefs[PrefsKeys.KLASSE_ID]  = klasseId
            prefs[PrefsKeys.PERSON_ID]  = personId
        }
    }

    suspend fun saveAlarmSettings(enabled: Boolean, minutesBefore: Int) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.ALARM_ENABLED] = if (enabled) "true" else "false"
            prefs[PrefsKeys.ALARM_MINUTES]  = minutesBefore
        }
    }

    suspend fun setAlarmDisabledForDate(dateStr: String, disabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[PrefsKeys.ALARM_DISABLED_DATES] ?: emptySet()
            val newSet = if (disabled) {
                current + dateStr
            } else {
                current - dateStr
            }
            prefs[PrefsKeys.ALARM_DISABLED_DATES] = newSet
        }
    }

    suspend fun saveTemplates(templates: List<AlarmTemplate>) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.TEMPLATES] = com.google.gson.Gson().toJson(templates)
        }
    }

    suspend fun saveOneTimeTemplate(oneTime: OneTimeTemplate?) {
        context.dataStore.edit { prefs ->
            if (oneTime != null) {
                prefs[PrefsKeys.ONE_TIME_TEMPLATE] = com.google.gson.Gson().toJson(oneTime)
            } else {
                prefs.remove(PrefsKeys.ONE_TIME_TEMPLATE)
            }
        }
    }

    suspend fun saveCustomAlarms(alarms: List<CustomAlarm>) {
        context.dataStore.edit { prefs ->
            // Cleanup: wasEnabledBeforeDisable nur speichern wenn alarmEnabled == false
            // Sonst entfernen wir es, um Speicher zu sparen
            val cleaned = alarms.map { alarm ->
                if (alarm.isEnabled) {
                    alarm.copy(wasEnabledBeforeDisable = null)
                } else {
                    alarm
                }
            }
            prefs[PrefsKeys.CUSTOM_ALARMS] = com.google.gson.Gson().toJson(cleaned)
        }
    }

    suspend fun saveHabits(habitsList: List<Habit>) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.HABITS] = com.google.gson.Gson().toJson(habitsList)
        }
    }

    suspend fun saveHabitLogs(logs: List<HabitLog>) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.HABIT_LOGS] = com.google.gson.Gson().toJson(logs)
        }
    }

    suspend fun saveSchoolModeSettings(
        dndEnabled: Boolean,
        volumeEnabled: Boolean,
        volumeRing: Boolean,
        volumeNotification: Boolean,
        volumeMedia: Boolean,
        pauseBehavior: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.AUTO_DND_ENABLED] = if (dndEnabled) "true" else "false"
            prefs[PrefsKeys.AUTO_VOLUME_ENABLED] = if (volumeEnabled) "true" else "false"
            prefs[PrefsKeys.AUTO_VOLUME_RING] = if (volumeRing) "true" else "false"
            prefs[PrefsKeys.AUTO_VOLUME_NOTIFICATION] = if (volumeNotification) "true" else "false"
            prefs[PrefsKeys.AUTO_VOLUME_MEDIA] = if (volumeMedia) "true" else "false"
            prefs[PrefsKeys.DND_PAUSE_BEHAVIOR] = pauseBehavior
        }
    }

    suspend fun saveAutoDndNotify(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.AUTO_DND_NOTIFY] = if (enabled) "true" else "false"
        }
    }

    suspend fun saveThemeSettings(mode: String, dynamicColors: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.THEME_MODE] = mode
            prefs[PrefsKeys.USE_DYNAMIC_COLORS] = if (dynamicColors) "true" else "false"
        }
    }

    suspend fun saveWasLiveNotificationEnabledBeforeDisable(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.WAS_LIVE_NOTIFICATION_BEFORE_DISABLE] = if (enabled) "true" else "false"
        }
    }

    suspend fun saveSleepSettings(settings: SleepSettings) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.SLEEP_ENABLED] = if (settings.isEnabled) "true" else "false"
            prefs[PrefsKeys.SLEEP_HOURS] = settings.desiredSleepHours.toString()
            prefs[PrefsKeys.SLEEP_FULLSCREEN] = if (settings.showFullscreenReminder) "true" else "false"
            prefs[PrefsKeys.SLEEP_VIBRATE_ONLY] = if (settings.vibrateOnly) "true" else "false"
            prefs[PrefsKeys.SLEEP_AUTO_DISMISS_MINUTES] = settings.autoDismissMinutes
            prefs[PrefsKeys.SLEEP_WAS_ENABLED_BEFORE_DISABLE] =
                if (settings.wasEnabledBeforeMainAlarmDisable == true) "true" else "false"
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(PrefsKeys.SESSION_ID)
            prefs.remove(PrefsKeys.KLASSE_ID)
        }
    }

    suspend fun saveNextAlarm(dateStr: String, timeStr: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.NEXT_ALARM_DATE] = dateStr
            prefs[PrefsKeys.NEXT_ALARM_TIME] = timeStr
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
