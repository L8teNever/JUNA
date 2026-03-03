package bea.l8tenever.com.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import bea.l8tenever.com.data.AlarmTemplate
import bea.l8tenever.com.data.CustomAlarm
import bea.l8tenever.com.data.OneTimeTemplate
import bea.l8tenever.com.data.TimetableEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import bea.l8tenever.com.data.UserPreferences
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import bea.l8tenever.com.widget.*

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val ALARM_REQUEST_CODE = 1001
    private const val BEDTIME_ALARM_REQUEST_CODE = 3000

    /**
     * Berechnet den nächsten Wecktermin.
     * Nimmt die erste NICHT-ausgefallene Stunde des nächsten Tages,
     * zieht 'minutesBefore' Minuten ab und stellt den Alarm.
     */
    fun scheduleNextAlarm(
        context: Context,
        timetable: List<TimetableEntry>,
        minutesBefore: Int,
        enabled: Boolean,
        disabledDates: Set<String> = emptySet(),
        customAlarms: List<CustomAlarm> = emptyList(),
        templates: List<AlarmTemplate> = emptyList(),
        oneTimeTemplate: OneTimeTemplate? = null,
        sleepSettings: bea.l8tenever.com.data.SleepSettings? = null
    ) {
        cancelAlarm(context)

        // Wenn Hauptwecker deaktiviert ist, auch Custom Alarms deaktivieren
        val effectiveCustomAlarms = if (!enabled) {
            customAlarms.map { it.copy(isEnabled = false) }
        } else {
            customAlarms
        }

        if (!enabled && effectiveCustomAlarms.none { it.isEnabled } && templates.none { it.isEnabled }) {
            Log.d(TAG, "Alarm ist deaktiviert und keine Custom-Alarms/Templates aktiv.")
            return
        }

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val dayAfterTomorrow = tomorrow.plusDays(1)

        // RICHTIGE Reihenfolge:
        // 1. Heute prüfen – aber nur wenn Weckzeit noch in der ZUKUNFT liegt
        // 2. Morgen
        // 3. Übermorgen (Fallback)
        fun findLessonAndTime(date: LocalDate): Pair<TimetableEntry, LocalDateTime>? {
            val lesson = findFirstActiveLesson(timetable, date, disabledDates) ?: return null
            // Template für diesen Tag auflösen
            val (effectiveMinutes, _) = resolveEffectiveSettings(
                date, minutesBefore, customAlarms, templates, oneTimeTemplate
            )
            val alarmTime = calculateAlarmTime(lesson, effectiveMinutes) ?: return null
            return if (alarmTime.isAfter(now)) Pair(lesson, alarmTime) else null
        }

        val (firstLesson, alarmTime) = findLessonAndTime(today)
            ?: findLessonAndTime(tomorrow)
            ?: findLessonAndTime(dayAfterTomorrow)
            ?: run {
                Log.d(TAG, "Kein gültiger Alarm-Zeitpunkt gefunden.")
                MainScope().launch {
                    UserPreferences(context).saveNextAlarm("", "")
                    NextLessonWidget().updateAll(context)
                    LessonCountdownWidget().updateAll(context)
                    TimetableListWidget().updateAll(context)
                    AlarmStatusWidget().updateAll(context)
                }
                return
            }

        // Save for widgets to see
        MainScope().launch {
            val dateStr = alarmTime.toLocalDate().toString()
            val timeStr = alarmTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            UserPreferences(context).saveNextAlarm(dateStr, timeStr)
            
            // Trigger all widgets to refresh
            NextLessonWidget().updateAll(context)
            LessonCountdownWidget().updateAll(context)
            TimetableListWidget().updateAll(context)
            AlarmStatusWidget().updateAll(context)
        }

        // Effektive Einstellungen für den gefundenen Tag
        val lessonDate = LocalDate.parse(firstLesson.date)
        val (effectiveMinutes, effectiveResolvedCustomAlarms) = resolveEffectiveSettings(
            lessonDate, minutesBefore, effectiveCustomAlarms, templates, oneTimeTemplate
        )

        Log.d(TAG, "Template-Auflösung für ${firstLesson.date}: ${effectiveMinutes} Min, ${effectiveResolvedCustomAlarms.size} Custom-Alarms")

        // Haupt-Alarm setzen
        if (enabled) {
            setAlarm(context, alarmTime, firstLesson, ALARM_REQUEST_CODE, "Zeit aufzustehen!")

            // Bedtime alarm scheduling
            if (sleepSettings != null) {
                scheduleBedtimeAlarm(context, alarmTime, sleepSettings)
            }
        }

        // Custom-Alarms setzen (aus Template ODER globale Custom-Alarms)
        effectiveResolvedCustomAlarms.filter { it.isEnabled }.forEachIndexed { index, custom ->
            val customTime = calculateAlarmTime(firstLesson, custom.minutesBefore)
            if (customTime != null && customTime.isAfter(now)) {
                setAlarm(context, customTime, firstLesson, 2000 + index, custom.name, custom.showWeather)
                Log.d(TAG, "Custom-Alarm '${custom.name}' gesetzt für: $customTime")
            } else {
                Log.d(TAG, "Custom-Alarm '${custom.name}' übersprungen (Zeit in Vergangenheit: $customTime)")
            }
        }
    }

    /**
     * Findet das aktive Template für einen bestimmten Tag.
     * Priorität: 1. OneTimeTemplate  2. Wochentag-Template (erstes passendes)
     * Falls kein Template: globale Standardwerte zurückgeben.
     */
    fun resolveEffectiveSettings(
        date: LocalDate,
        defaultMinutes: Int,
        defaultCustomAlarms: List<CustomAlarm>,
        templates: List<AlarmTemplate>,
        oneTimeTemplate: OneTimeTemplate?
    ): Pair<Int, List<CustomAlarm>> {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dayOfWeek = date.dayOfWeek.value // 1=Mo ... 7=So

        // 1. Einmalig aktives Template für diesen Tag?
        if (oneTimeTemplate != null && oneTimeTemplate.date == dateStr) {
            val tmpl = templates.find { it.id == oneTimeTemplate.templateId && it.isEnabled }
            if (tmpl != null) {
                Log.d(TAG, "Einmal-Template '${tmpl.name}' aktiv für $dateStr")
                return Pair(tmpl.minutesBefore, tmpl.customAlarms)
            }
        }

        // 2. Wochentag-Template?
        val weekdayTemplate = templates
            .filter { it.isEnabled && dayOfWeek in it.activeDays }
            .firstOrNull()
        if (weekdayTemplate != null) {
            Log.d(TAG, "Wochentag-Template '${weekdayTemplate.name}' aktiv für $dateStr (Tag $dayOfWeek)")
            return Pair(weekdayTemplate.minutesBefore, weekdayTemplate.customAlarms)
        }

        // 3. Globale Standardwerte
        return Pair(defaultMinutes, defaultCustomAlarms)
    }

    fun findFirstActiveLesson(
        timetable: List<TimetableEntry>,
        date: LocalDate,
        disabledDates: Set<String> = emptySet()
    ): TimetableEntry? {
        // Wochenende ausschließen (Wecker niemals an Sa/So)
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return null
        }

        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        if (disabledDates.contains(dateStr)) return null

        return timetable
            .filter { it.date == dateStr && !it.isCancelled && it.subject != "Pause" }
            .minByOrNull { it.startTime }
    }

    private fun calculateAlarmTime(lesson: TimetableEntry, minutesBefore: Int): LocalDateTime? {
        return try {
            val dateParts = lesson.date.split("-")
            val timeParts = lesson.startTime.split(":")
            val lessonStart = LocalDateTime.of(
                dateParts[0].toInt(), dateParts[1].toInt(), dateParts[2].toInt(),
                timeParts[0].toInt(), timeParts[1].toInt()
            )
            lessonStart.minusMinutes(minutesBefore.toLong())
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Berechnen der Alarm-Zeit", e)
            null
        }
    }

    private fun setAlarm(
        context: Context, 
        alarmTime: LocalDateTime, 
        lesson: TimetableEntry, 
        requestCode: Int, 
        title: String,
        showWeather: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("lesson_subject", lesson.subject)
            putExtra("lesson_time", lesson.startTime)
            putExtra("lesson_date", lesson.date)
            putExtra("alarm_title", title)
            putExtra("show_weather", showWeather)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = alarmTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        // Intent für den Klick auf das Wecker-Icon in der Statusleiste (öffnet App)
        val showIntent = Intent(context, bea.l8tenever.com.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "Alarm gesetzt ($title) für: $alarmTime (${lesson.subject} um ${lesson.startTime})")

        try {
            // setAlarmClock ist die einzige Methode, die in Android zuverlässig Doze umgeht
            // und das Wecker-Icon in der Statuszeile anzeigt.
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    // Fallback: Inexakt, wenn keine Berechtigung
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Keine Berechtigung für exakte Alarme", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun scheduleBedtimeAlarm(
        context: Context,
        mainAlarmTime: LocalDateTime?,
        sleepSettings: bea.l8tenever.com.data.SleepSettings
    ) {
        cancelBedtimeAlarm(context)

        if (!sleepSettings.isEnabled || mainAlarmTime == null) {
            Log.d(TAG, "Bedtime alarm disabled or no main alarm scheduled")
            return
        }

        // Calculate bedtime (X hours before main alarm)
        val sleepHoursLong = (sleepSettings.desiredSleepHours * 60).toLong()
        val bedtimeAlarm = mainAlarmTime.minusMinutes(sleepHoursLong)
        val now = LocalDateTime.now()

        // Only schedule if bedtime is in the future and within 24 hours
        if (!bedtimeAlarm.isAfter(now)) {
            Log.d(TAG, "Bedtime already passed: $bedtimeAlarm")
            return
        }

        val hoursToBedtime = java.time.Duration.between(now, bedtimeAlarm).toHours()
        if (hoursToBedtime > 24) {
            Log.d(TAG, "Bedtime too far in future (${hoursToBedtime}h), skipping")
            return
        }

        // Schedule the bedtime alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BedtimeAlarmReceiver::class.java).apply {
            putExtra("main_alarm_time", mainAlarmTime.toString())
            putExtra("sleep_hours", sleepSettings.desiredSleepHours)
            putExtra("fullscreen_enabled", sleepSettings.showFullscreenReminder)
            putExtra("vibrate_only", sleepSettings.vibrateOnly)
            putExtra("auto_dismiss_minutes", sleepSettings.autoDismissMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BEDTIME_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = bedtimeAlarm
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val showIntent = Intent(context, bea.l8tenever.com.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context, BEDTIME_ALARM_REQUEST_CODE, showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
            Log.d(TAG, "Bedtime alarm scheduled for: $bedtimeAlarm")
        } catch (e: SecurityException) {
            Log.e(TAG, "Keine Berechtigung für exakte Alarme (Bedtime)", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelBedtimeAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BedtimeAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BEDTIME_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        Log.d(TAG, "Bedtime alarm cancelled")
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        
        // Haupt-Alarm abbrechen
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        // Alle Custom-Alarms abbrechen (aus Sicherheit 30 durchlaufen)
        for (i in 0..30) {
            val customPending = PendingIntent.getBroadcast(
                context,
                2000 + i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            customPending?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        // Bedtime-Alarm abbrechen
        cancelBedtimeAlarm(context)

        Log.d(TAG, "Alarme abgebrochen.")
    }

    /**
     * Testet den Alarm (Klingelt in 5 Sekunden)
     */
    fun testAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("lesson_subject", "TEST ALARM")
            putExtra("lesson_time", "00:00")
            putExtra("lesson_date", "Heute")
            putExtra("alarm_title", "TEST ALARM!")
        }

        // Andere Request-Code für den Test, damit er den normalen Alarm nicht überschreibt
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = System.currentTimeMillis() + 5000L

        Log.d(TAG, "TEST-Alarm gesetzt in 5 Sekunden")

        try {
            val showIntent = Intent(context, bea.l8tenever.com.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context, ALARM_REQUEST_CODE + 1, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Keine Berechtigung für exakte Alarme (Test)", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }
}
