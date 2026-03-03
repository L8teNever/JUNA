package bea.l8tenever.com.data

data class SleepSettings(
    val isEnabled: Boolean = false,
    val desiredSleepHours: Float = 8.0f,
    val showFullscreenReminder: Boolean = true,
    val vibrateOnly: Boolean = false,
    val autoDismissMinutes: Int = 30,
    val wasEnabledBeforeMainAlarmDisable: Boolean? = null
)
