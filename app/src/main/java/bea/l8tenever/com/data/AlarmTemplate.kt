package bea.l8tenever.com.data

import java.util.UUID

/**
 * Ein Wecker-Template: Enthält einen Haupt-Wecker + beliebig viele Zusatz-Wecker.
 * Kann für bestimmte Wochentage automatisch aktiv sein ODER einmalig für einen Tag aktiviert werden.
 *
 * @param activeDays  Wochentage als ISO-Werte: 1=Montag, 2=Dienstag, ..., 7=Sonntag. Leer = nur manuell.
 */
data class AlarmTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val minutesBefore: Int = 60,         // Haupt-Wecker X Minuten vor 1. Stunde
    val customAlarms: List<CustomAlarm> = emptyList(), // Zusatz-Wecker (z.B. "Duschen" bei 80 Min)
    val activeDays: Set<Int> = emptySet(), // 1=Mo, 2=Di, 3=Mi, 4=Do, 5=Fr – leer = nur manuell
    val isEnabled: Boolean = true
)

/**
 * Einmalige Aktivierung eines Templates für einen bestimmten Tag.
 * Nach diesem Tag wird es automatisch entfernt.
 */
data class OneTimeTemplate(
    val templateId: String,
    val date: String  // yyyy-MM-dd
)
