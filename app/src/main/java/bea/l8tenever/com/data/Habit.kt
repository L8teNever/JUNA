package bea.l8tenever.com.data

import java.util.UUID

enum class HabitTimeType {
    SPECIFIC_TIME,
    BEFORE_FIRST_LESSON,
    BEFORE_BEDTIME,
    AFTER_BEDTIME,
    ANYTIME
}

data class HabitTrigger(
    val type: HabitTimeType = HabitTimeType.ANYTIME,
    val time: String? = null, // "HH:mm" for SPECIFIC_TIME
    val offsetMinutes: Int = 0 
)

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // java.time.DayOfWeek values (1=Monday)
    val timesPerDay: Int = 1,
    val triggers: List<HabitTrigger> = emptyList()
)

data class HabitLog(
    val habitId: String,
    val date: String, // "YYYY-MM-DD"
    val completedCount: Int = 0
)
