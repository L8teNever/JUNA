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
    val TEMPLATES     = stringPreferencesKey("alarm_templates")    // JSON-Liste von AlarmTemplate
    val ONE_TIME_TEMPLATE = stringPreferencesKey("one_time_template") // JSON von OneTimeTemplate?
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
            prefs[PrefsKeys.CUSTOM_ALARMS] = com.google.gson.Gson().toJson(alarms)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(PrefsKeys.SESSION_ID)
            prefs.remove(PrefsKeys.KLASSE_ID)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
