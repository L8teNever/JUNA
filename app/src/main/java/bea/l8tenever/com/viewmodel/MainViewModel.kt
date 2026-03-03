package bea.l8tenever.com.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bea.l8tenever.com.data.*
import bea.l8tenever.com.alarm.AlarmScheduler
import bea.l8tenever.com.worker.TimetableWorker
import bea.l8tenever.com.widget.NextLessonWidget
import bea.l8tenever.com.widget.LessonCountdownWidget
import bea.l8tenever.com.widget.TimetableListWidget
import androidx.glance.appwidget.updateAll
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val timetable: List<TimetableEntry> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val alarmEnabled: Boolean = true,
    val alarmMinutes: Int = 60,
    val alarmDisabledDates: Set<String> = emptySet(),
    val customAlarms: List<CustomAlarm> = emptyList(),
    val nextAlarmInfo: String = "",
    // Templates
    val templates: List<AlarmTemplate> = emptyList(),
    val oneTimeTemplate: OneTimeTemplate? = null,
    // Login-Felder
    val serverUrl: String = "",
    val school: String = "",
    val username: String = "",
    val password: String = "",
    // Session
    val sessionId: String = "",
    val klasseId: Int = 0,
    val personId: Int = 0,
    val liveNotificationEnabled: Boolean = false,
    val wasLiveNotificationEnabledBeforeDisable: Boolean = false,
    // Schulmodus-Einstellungen
    val autoDndEnabled: Boolean = false,
    val autoVolumeEnabled: Boolean = false,
    val autoVolumeRing: Boolean = true,
    val autoVolumeNotification: Boolean = true,
    val autoVolumeMedia: Boolean = false,
    val dndPauseBehavior: String = "deactivate",
    val autoDndNotify: Boolean = true,
    val hasDndPermission: Boolean = false,
    val themeMode: String = "system", // system, light, dark
    val useDynamicColors: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val prefs = UserPreferences(application)
    private val repo  = UntisRepository()
    private val context = application
    private val gson  = Gson()

    init {
        viewModelScope.launch {
            // Alle gespeicherten Einstellungen auf einmal laden
            val p = context.dataStore.data.first()
            val url      = p[PrefsKeys.SERVER_URL] ?: ""
            val school   = p[PrefsKeys.SCHOOL]     ?: ""
            val user     = p[PrefsKeys.USERNAME]   ?: ""
            val pass     = p[PrefsKeys.PASSWORD]   ?: ""
            val sid      = p[PrefsKeys.SESSION_ID] ?: ""
            val kid      = p[PrefsKeys.KLASSE_ID]  ?: 0
            val alarmEn  = p[PrefsKeys.ALARM_ENABLED] != "false"
            val alarmMin = p[PrefsKeys.ALARM_MINUTES] ?: 60
            val disabledDates = p[PrefsKeys.ALARM_DISABLED_DATES] ?: emptySet()
            
            // Custom Alarms synchron laden für Init
            val customAlarmsJson = p[PrefsKeys.CUSTOM_ALARMS]
            val customAlarmsList: List<CustomAlarm> = if (customAlarmsJson.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<CustomAlarm>>() {}.type
                    gson.fromJson(customAlarmsJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // Templates laden
            val templatesJson = p[PrefsKeys.TEMPLATES]
            val templatesList: List<AlarmTemplate> = if (templatesJson.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<AlarmTemplate>>() {}.type
                    gson.fromJson(templatesJson, type) ?: emptyList()
                } catch (e: Exception) { emptyList() }
            }

            // OneTimeTemplate laden (und ggf. abgelaufene löschen)
            val oneTimeJson = p[PrefsKeys.ONE_TIME_TEMPLATE]
            var oneTime: OneTimeTemplate? = if (oneTimeJson.isNullOrBlank()) null else {
                try { gson.fromJson(oneTimeJson, OneTimeTemplate::class.java) } catch (_: Exception) { null }
            }
            // Abgelaufen? (Datum in der Vergangenheit)
            if (oneTime != null && !LocalDate.now().isBefore(LocalDate.parse(oneTime.date).plusDays(1))) {
                prefs.saveOneTimeTemplate(null)
                oneTime = null
            }

            val liveEnabled = p[stringPreferencesKey("live_notification_enabled")] == "true"
            val wasLiveEnabledBeforeDisable = p[PrefsKeys.WAS_LIVE_NOTIFICATION_BEFORE_DISABLE] == "true"

            // Schulmodus-Einstellungen laden
            val autoDndEnabled = p[PrefsKeys.AUTO_DND_ENABLED] != "false"
            val autoVolumeEnabled = p[PrefsKeys.AUTO_VOLUME_ENABLED] != "false"
            val autoVolumeRing = p[PrefsKeys.AUTO_VOLUME_RING] != "false"
            val autoVolumeNotification = p[PrefsKeys.AUTO_VOLUME_NOTIFICATION] != "false"
            val autoVolumeMedia = p[PrefsKeys.AUTO_VOLUME_MEDIA] ?: "false" != "false"
            val dndPauseBehavior = p[PrefsKeys.DND_PAUSE_BEHAVIOR] ?: "deactivate"
            val autoDndNotify = p[PrefsKeys.AUTO_DND_NOTIFY] != "false"
            
            // Theme & Color
            val themeMode = p[PrefsKeys.THEME_MODE] ?: "system"
            val useDynamicColors = p[PrefsKeys.USE_DYNAMIC_COLORS] != "false"

            // DND Permission prüfen (ab Android 6.0)
            val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
            val hasDndPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm.isNotificationPolicyAccessGranted
            } else true

            _state.update {
                it.copy(
                    serverUrl    = url,
                    school       = school,
                    username     = user,
                    password     = pass,
                    sessionId    = sid,
                    klasseId     = kid,
                    alarmEnabled = alarmEn,
                    alarmMinutes = alarmMin,
                    alarmDisabledDates = disabledDates,
                    customAlarms = customAlarmsList,
                    templates    = templatesList,
                    oneTimeTemplate = oneTime,
                    isLoggedIn   = false,
                    liveNotificationEnabled = liveEnabled,
                    wasLiveNotificationEnabledBeforeDisable = wasLiveEnabledBeforeDisable,
                    autoDndEnabled = autoDndEnabled,
                    autoVolumeEnabled = autoVolumeEnabled,
                    autoVolumeRing = autoVolumeRing,
                    autoVolumeNotification = autoVolumeNotification,
                    autoVolumeMedia = autoVolumeMedia,
                    dndPauseBehavior = dndPauseBehavior,
                    autoDndNotify = autoDndNotify,
                    hasDndPermission = hasDndPermission,
                    themeMode = themeMode,
                    useDynamicColors = useDynamicColors
                )
            }

            // Gecachten Stundenplan sofort anzeigen (während Reload läuft)
            val cached = p[stringPreferencesKey("cached_timetable")]
            if (!cached.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<TimetableEntry>>() {}.type
                    val entries: List<TimetableEntry> = gson.fromJson(cached, type)
                    _state.update { it.copy(timetable = entries) }
                    updateAlarmInfo(entries)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Cache-Fehler", e)
                }
            }

            // Automatisch neu einloggen + Stundenplan laden (CookieJar ist leer nach App-Start)
            if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                Log.d("MainViewModel", "Auto-Login beim App-Start...")
                silentLoginAndFetch(url, school, user, pass)
            }
        }
    }

    // ----------------------------
    // Login
    // ----------------------------
    fun login(serverUrl: String, school: String, username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val credentials = LoginCredentials(serverUrl, school, username, password)
            when (val result = repo.login(credentials)) {
                is UntisResult.Success -> {
                    val session = result.data
                    prefs.saveCredentials(credentials)
                    prefs.saveSession(
                        session.sessionId ?: "",
                        session.klasseId  ?: 0,
                        session.personId  ?: 0
                    )
                    val newKid = session.klasseId ?: 0
                    val newPid = session.personId ?: 0
                    _state.update {
                        it.copy(
                            isLoading  = false,
                            isLoggedIn = true,
                            sessionId  = session.sessionId ?: "",
                            klasseId   = newKid,
                            personId   = newPid,
                            serverUrl  = serverUrl,
                            school     = school,
                            username   = username,
                            password   = password
                        )
                    }
                    TimetableWorker.schedule(context)
                    fetchTimetableOnly(newKid, newPid, credentials)
                }
                is UntisResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    // ----------------------------
    // Stundenplan abrufen
    // ----------------------------
    /**
     * Öffentliche Funktion: Login + Stundenplan-Fetch (z.B. beim Refresh-Button).
     * Loggt sich immer neu ein (CookieJar könnte leer sein).
     */
    fun fetchTimetable() {
        val st = _state.value
        if (st.serverUrl.isBlank() || st.username.isBlank() || st.password.isBlank()) {
            TimetableWorker.scheduleImmediate(context)
            return
        }
        viewModelScope.launch {
            silentLoginAndFetch(st.serverUrl, st.school, st.username, st.password)
        }
    }

    /**
     * Intern: Login → Stundenplan.  Wird beim App-Start und beim Refresh genutzt.
     * Kein Dialog, kein Redirect – only silently updates the state.
     */
    private suspend fun silentLoginAndFetch(
        serverUrl: String, school: String,
        username: String, password: String
    ) {
        _state.update { it.copy(isLoading = true, error = null) }
        val creds = LoginCredentials(serverUrl, school, username, password)

        Log.d("MainViewModel", "silentLoginAndFetch: url=$serverUrl school=$school user=$username")

        // 1. Login → SessionInterceptor bekommt JSESSIONID
        when (val loginResult = repo.login(creds)) {
            is UntisResult.Error -> {
                Log.e("MainViewModel", "Login fehlgeschlagen: ${loginResult.message}")
                _state.update { it.copy(isLoading = false, error = "Login: ${loginResult.message}") }
                return
            }
            is UntisResult.Success -> {
                val session = loginResult.data
                val newSid  = session.sessionId ?: ""
                val newKid  = session.klasseId  ?: _state.value.klasseId
                val newPid  = session.personId  ?: _state.value.personId
                Log.d("MainViewModel", "Login OK: sessionId=$newSid klasseId=$newKid personId=$newPid")
                prefs.saveSession(newSid, newKid, newPid)
                _state.update {
                    it.copy(
                        isLoggedIn = true,
                        sessionId  = newSid,
                        klasseId   = newKid,
                        personId   = newPid
                    )
                }
                fetchTimetableOnly(newKid, newPid, creds)
            }
        }
    }

    /**
     * Lädt den Stundenplan OHNE neuen Login.
     * Voraussetzung: SessionInterceptor hat bereits ein gültiges JSESSIONID.
     * Wird nach login() und silentLoginAndFetch() aufgerufen.
     */
    private suspend fun fetchTimetableOnly(klasseId: Int, personId: Int, creds: LoginCredentials) {
        val st    = _state.value
        val today = LocalDate.now()
        Log.d("MainViewModel", "fetchTimetableOnly: klasseId=$klasseId personId=$personId")
        when (val ttResult = repo.getTimetable(
            creds     = creds,
            sessionId = st.sessionId,
            klasseId  = klasseId,
            personId  = personId,
            startDate = today,
            endDate   = today.plusDays(14)
        )) {
            is UntisResult.Success -> {
                val entries = ttResult.data
                Log.d("MainViewModel", "Stundenplan geladen: ${entries.size} Einträge")
                _state.update { it.copy(isLoading = false, timetable = entries, error = null) }
                cacheTimetable(entries)
                updateAlarmInfo(entries)
                scheduleAlarm(_state.value)
            }
            is UntisResult.Error -> {
                Log.e("MainViewModel", "Stundenplan-Fehler: ${ttResult.message}")
                _state.update { it.copy(isLoading = false, error = ttResult.message) }
            }
        }
    }

    // ----------------------------
    // Datum wählen
    // ----------------------------
    fun setSelectedDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
    }

    // ----------------------------
    // Alarm-Einstellungen speichern
    // ----------------------------
    fun updateAlarmSettings(enabled: Boolean, minutesBefore: Int) {
        viewModelScope.launch {
            prefs.saveAlarmSettings(enabled, minutesBefore)
            val currentState = _state.value

            // Wenn Wecker aktiviert wird, vorherige Einstellungen wiederherstellen
            if (enabled && !currentState.alarmEnabled) {
                // Custom Alarms wiederherstellen
                val restoredCustomAlarms = currentState.customAlarms.map { alarm ->
                    if (alarm.wasEnabledBeforeDisable != null) {
                        alarm.copy(isEnabled = alarm.wasEnabledBeforeDisable, wasEnabledBeforeDisable = null)
                    } else {
                        alarm
                    }
                }

                // Live Notification wiederherstellen
                val restoredLiveNotification = currentState.wasLiveNotificationEnabledBeforeDisable

                _state.update {
                    it.copy(
                        alarmEnabled = enabled,
                        alarmMinutes = minutesBefore,
                        customAlarms = restoredCustomAlarms,
                        liveNotificationEnabled = restoredLiveNotification,
                        wasLiveNotificationEnabledBeforeDisable = false
                    )
                }

                prefs.saveCustomAlarms(restoredCustomAlarms)
                prefs.saveWasLiveNotificationEnabledBeforeDisable(false)
                if (restoredLiveNotification) {
                    context.dataStore.edit { it[stringPreferencesKey("live_notification_enabled")] = "true" }
                    bea.l8tenever.com.worker.LiveStundeWorker.schedule(context, true)
                }
            }
            // Wenn Wecker deaktiviert wird, aktuelle Einstellungen speichern
            else if (!enabled && currentState.alarmEnabled) {
                // Status vor Deaktivierung speichern
                val savedCustomAlarms = currentState.customAlarms.map { alarm ->
                    alarm.copy(wasEnabledBeforeDisable = alarm.isEnabled, isEnabled = false)
                }

                _state.update {
                    it.copy(
                        alarmEnabled = enabled,
                        alarmMinutes = minutesBefore,
                        customAlarms = savedCustomAlarms,
                        wasLiveNotificationEnabledBeforeDisable = currentState.liveNotificationEnabled,
                        liveNotificationEnabled = false
                    )
                }

                prefs.saveCustomAlarms(savedCustomAlarms)
                prefs.saveWasLiveNotificationEnabledBeforeDisable(currentState.liveNotificationEnabled)
                context.dataStore.edit { it[stringPreferencesKey("live_notification_enabled")] = "false" }
                bea.l8tenever.com.worker.LiveStundeWorker.schedule(context, false)
            } else {
                _state.update {
                    it.copy(alarmEnabled = enabled, alarmMinutes = minutesBefore)
                }
            }

            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    fun toggleLiveNotification(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[stringPreferencesKey("live_notification_enabled")] = if (enabled) "true" else "false" }
            _state.update { it.copy(liveNotificationEnabled = enabled) }
            bea.l8tenever.com.worker.LiveStundeWorker.schedule(context, enabled)
        }
    }

    // ----------------------------
    // Schulmodus-Einstellungen
    // ----------------------------
    fun toggleAutoDnd(enabled: Boolean) {
        viewModelScope.launch {
            val st = _state.value
            prefs.saveSchoolModeSettings(
                dndEnabled = enabled,
                volumeEnabled = st.autoVolumeEnabled,
                volumeRing = st.autoVolumeRing,
                volumeNotification = st.autoVolumeNotification,
                volumeMedia = st.autoVolumeMedia,
                pauseBehavior = st.dndPauseBehavior
            )
            _state.update { it.copy(autoDndEnabled = enabled) }
        }
    }

    fun toggleAutoVolume(enabled: Boolean) {
        viewModelScope.launch {
            val st = _state.value
            prefs.saveSchoolModeSettings(
                dndEnabled = st.autoDndEnabled,
                volumeEnabled = enabled,
                volumeRing = st.autoVolumeRing,
                volumeNotification = st.autoVolumeNotification,
                volumeMedia = st.autoVolumeMedia,
                pauseBehavior = st.dndPauseBehavior
            )
            _state.update { it.copy(autoVolumeEnabled = enabled) }
        }
    }

    fun updateVolumeSettings(ring: Boolean? = null, notification: Boolean? = null, media: Boolean? = null) {
        viewModelScope.launch {
            val st = _state.value
            val newRing = ring ?: st.autoVolumeRing
            val newNotif = notification ?: st.autoVolumeNotification
            val newMedia = media ?: st.autoVolumeMedia

            prefs.saveSchoolModeSettings(
                dndEnabled = st.autoDndEnabled,
                volumeEnabled = st.autoVolumeEnabled,
                volumeRing = newRing,
                volumeNotification = newNotif,
                volumeMedia = newMedia,
                pauseBehavior = st.dndPauseBehavior
            )
            _state.update { it.copy(autoVolumeRing = newRing, autoVolumeNotification = newNotif, autoVolumeMedia = newMedia) }
        }
    }

    fun updatePauseBehavior(behavior: String) {
        viewModelScope.launch {
            val st = _state.value
            prefs.saveSchoolModeSettings(
                dndEnabled = st.autoDndEnabled,
                volumeEnabled = st.autoVolumeEnabled,
                volumeRing = st.autoVolumeRing,
                volumeNotification = st.autoVolumeNotification,
                volumeMedia = st.autoVolumeMedia,
                pauseBehavior = behavior
            )
            _state.update { it.copy(dndPauseBehavior = behavior) }
        }
    }

    fun toggleAutoDndNotify(enabled: Boolean) {
        viewModelScope.launch {
            prefs.saveAutoDndNotify(enabled)
            _state.update { it.copy(autoDndNotify = enabled) }
        }
    }

    // ----------------------------
    // Einzelne Tage für den Wecker deaktivieren
    // ----------------------------
    fun toggleAlarmForDate(date: LocalDate, disable: Boolean) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            prefs.setAlarmDisabledForDate(dateStr, disable)
            
            // State patchen
            val currentDisabled = _state.value.alarmDisabledDates.toMutableSet()
            if (disable) currentDisabled.add(dateStr)
            else currentDisabled.remove(dateStr)
            
            _state.update { it.copy(alarmDisabledDates = currentDisabled) }

            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    // ----------------------------
    // Zusatzwecker verwalten
    // ----------------------------
    fun addCustomAlarm(name: String, minutesBefore: Int, showWeather: Boolean = false) {
        val newAlarm = CustomAlarm(name = name, minutesBefore = minutesBefore, showWeather = showWeather)
        val updated = _state.value.customAlarms + newAlarm
        updateCustomAlarms(updated)
    }

    fun removeCustomAlarm(id: String) {
        val updated = _state.value.customAlarms.filter { it.id != id }
        updateCustomAlarms(updated)
    }

    fun toggleCustomAlarm(id: String, enabled: Boolean) {
        val updated = _state.value.customAlarms.map { 
            if (it.id == id) it.copy(isEnabled = enabled) else it 
        }
        updateCustomAlarms(updated)
    }

    private fun updateCustomAlarms(alarms: List<CustomAlarm>) {
        viewModelScope.launch {
            prefs.saveCustomAlarms(alarms)
            _state.update { it.copy(customAlarms = alarms) }
            scheduleAlarm(_state.value)
        }
    }

    // ----------------------------
    // Templates verwalten
    // ----------------------------
    fun saveTemplate(template: AlarmTemplate) {
        viewModelScope.launch {
            val existing = _state.value.templates
            val updated = if (existing.any { it.id == template.id }) {
                existing.map { if (it.id == template.id) template else it }
            } else {
                existing + template
            }
            prefs.saveTemplates(updated)
            _state.update { it.copy(templates = updated) }
            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    fun removeTemplate(id: String) {
        viewModelScope.launch {
            // OneTimeTemplate löschen falls es dasselbe Template referenziert
            val ott = _state.value.oneTimeTemplate
            if (ott?.templateId == id) {
                prefs.saveOneTimeTemplate(null)
                _state.update { it.copy(oneTimeTemplate = null) }
            }
            val updated = _state.value.templates.filter { it.id != id }
            prefs.saveTemplates(updated)
            _state.update { it.copy(templates = updated) }
            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    fun toggleTemplate(id: String, enabled: Boolean) {
        val updated = _state.value.templates.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
        viewModelScope.launch {
            prefs.saveTemplates(updated)
            _state.update { it.copy(templates = updated) }
            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    /**
     * Aktiviert ein Template einmalig für das angegebene Datum.
     * Nach diesem Tag wird es automatisch beim App-Start gelöscht.
     */
    fun activateTemplateForDate(templateId: String, date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val oneTime = OneTimeTemplate(templateId = templateId, date = dateStr)
            prefs.saveOneTimeTemplate(oneTime)
            _state.update { it.copy(oneTimeTemplate = oneTime) }
            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    /**
     * Hebt die einmalige Template-Aktivierung auf.
     */
    fun clearOneTimeTemplate() {
        viewModelScope.launch {
            prefs.saveOneTimeTemplate(null)
            _state.update { it.copy(oneTimeTemplate = null) }
            scheduleAlarm(_state.value)
            updateAlarmInfo(_state.value.timetable)
        }
    }

    /**
     * Zentraler Alarm-Scheduling-Aufruf – liest immer den aktuellen State.
     */
    private fun scheduleAlarm(st: AppUiState) {
        AlarmScheduler.scheduleNextAlarm(
            context         = context,
            timetable       = st.timetable,
            minutesBefore   = st.alarmMinutes,
            enabled         = st.alarmEnabled,
            disabledDates   = st.alarmDisabledDates,
            customAlarms    = st.customAlarms,
            templates       = st.templates,
            oneTimeTemplate = st.oneTimeTemplate
        )
    }

    // ----------------------------
    // Ausloggen
    // ----------------------------
    fun logout() {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(context)
            SessionStore.clear()  // JSESSIONID löschen
            prefs.clearAll()
            _state.update { AppUiState() }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ----------------------------
    // Hilfsfunktionen
    // ----------------------------
    private fun updateAlarmInfo(entries: List<TimetableEntry>) {
        val st = _state.value
        if (!st.alarmEnabled) {
            _state.update { it.copy(nextAlarmInfo = "Wecker deaktiviert") }
            return
        }

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val dayAfterTomorrow = tomorrow.plusDays(1)

        // Dieselbe Logik wie scheduleNextAlarm:
        // 1. Heute → nur wenn Weckzeit noch in der Zukunft
        // 2. Morgen
        // 3. Übermorgen
        fun findValidLesson(date: LocalDate): Pair<TimetableEntry, LocalDateTime>? {
            val lesson = AlarmScheduler.findFirstActiveLesson(entries, date, st.alarmDisabledDates) ?: return null
            val (h, m) = lesson.startTime.split(":").map { it.toInt() }
            val alarmDateTime = java.time.LocalDateTime.of(
                lesson.date.split("-").let { java.time.LocalDate.of(it[0].toInt(), it[1].toInt(), it[2].toInt()) },
                LocalTime.of(h, m).minusMinutes(st.alarmMinutes.toLong())
            )
            return if (alarmDateTime.isAfter(now)) Pair(lesson, alarmDateTime) else null
        }

        val result = findValidLesson(today)
            ?: findValidLesson(tomorrow)
            ?: findValidLesson(dayAfterTomorrow)

        if (result != null) {
            val (nextLesson, alarmDateTime) = result
            val lessonDate = LocalDate.parse(nextLesson.date)
            val dayPrefix = when (nextLesson.date) {
                today.toString()    -> "Heute"
                tomorrow.toString() -> "Morgen"
                else                -> "Übermorgen"
            }

            // Aktives Template für diesen Tag ermitteln
            val (effectiveMinutes, _) = AlarmScheduler.resolveEffectiveSettings(
                lessonDate, st.alarmMinutes, st.customAlarms, st.templates, st.oneTimeTemplate
            )
            val activeTemplate = st.templates.find { tmpl ->
                val dateStr = lessonDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                (st.oneTimeTemplate?.templateId == tmpl.id && st.oneTimeTemplate.date == dateStr) ||
                (st.oneTimeTemplate == null && lessonDate.dayOfWeek.value in tmpl.activeDays && tmpl.isEnabled)
            }
            val templateLabel = if (activeTemplate != null) " · 📋 ${activeTemplate.name}" else ""

            _state.update {
                it.copy(
                    nextAlarmInfo = "Nächster Wecker: $dayPrefix %02d:%02d Uhr · %s%s".format(
                        alarmDateTime.hour, alarmDateTime.minute, nextLesson.subject, templateLabel
                    )
                )
            }
        } else {
            _state.update { it.copy(nextAlarmInfo = "Kein Unterricht in Sicht") }
        }
    }

    fun getEntriesForDate(date: LocalDate): List<TimetableEntry> {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val lessons = _state.value.timetable.filter { it.date == dateStr && !it.isCancelled }.sortedBy { it.startTime }
        
        if (lessons.isEmpty()) return emptyList()
        
        // Pausen einfügen
        val result = mutableListOf<TimetableEntry>()
        for (i in lessons.indices) {
            result.add(lessons[i])
            if (i < lessons.size - 1) {
                val currentEnd = lessons[i].endTime
                val nextStart  = lessons[i+1].startTime
                if (currentEnd != nextStart) {
                    val duration = java.time.Duration.between(
                        java.time.LocalTime.parse(currentEnd),
                        java.time.LocalTime.parse(nextStart)
                    ).toMinutes()
                    
                    if (duration > 0) {
                        result.add(
                            TimetableEntry(
                                id = -100 - i, // Unique negativ ID für Pausen
                                date = dateStr,
                                startTime = currentEnd,
                                endTime = nextStart,
                                subject = "Pause",
                                room = "${duration} Min.",
                                teacher = "",
                                isCancelled = false
                            )
                        )
                    }
                }
            }
        }
        return result
    }
    fun updateThemeSettings(mode: String, dynamicColors: Boolean) {
        viewModelScope.launch {
            prefs.saveThemeSettings(mode, dynamicColors)
            _state.update { it.copy(themeMode = mode, useDynamicColors = dynamicColors) }
        }
    }

    private fun cacheTimetable(entries: List<TimetableEntry>) {
        viewModelScope.launch {
            val json = gson.toJson(entries)
            context.dataStore.edit { it[stringPreferencesKey("cached_timetable")] = json }
            NextLessonWidget().updateAll(context)
            LessonCountdownWidget().updateAll(context)
            TimetableListWidget().updateAll(context)
        }
    }
}
