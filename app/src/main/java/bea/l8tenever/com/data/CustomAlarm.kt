package bea.l8tenever.com.data

import java.util.UUID

data class CustomAlarm(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val minutesBefore: Int,
    val isEnabled: Boolean = true,
    val showWeather: Boolean = false,
    val wasEnabledBeforeDisable: Boolean? = null // Speichert den Status vor Deaktivierung
)
