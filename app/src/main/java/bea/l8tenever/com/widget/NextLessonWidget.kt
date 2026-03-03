package bea.l8tenever.com.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.*
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.currentState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import bea.l8tenever.com.MainActivity
import bea.l8tenever.com.data.TimetableEntry
import bea.l8tenever.com.data.dataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class NextLessonWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data from DataStore
        val prefs = context.dataStore.data.first()
        val json = prefs[androidx.datastore.preferences.core.stringPreferencesKey("cached_timetable")]
        
        val timetable: List<TimetableEntry> = if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<TimetableEntry>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }

        val now = LocalTime.now()
        val today = LocalDate.now().toString()
        
        val todayEntries = timetable
            .filter { it.date == today && !it.isCancelled && it.subject != "Pause" }
            .sortedBy { it.startTime }

        val currentLesson = todayEntries.firstOrNull { entry ->
            val start = LocalTime.parse(entry.startTime)
            val end = LocalTime.parse(entry.endTime)
            !now.isBefore(start) && !now.isAfter(end)
        }

        val nextLesson = todayEntries.firstOrNull { LocalTime.parse(it.startTime).isAfter(now) }
        val mainLesson = currentLesson ?: nextLesson

        provideContent {
            val widgetPrefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val themeMode = widgetPrefs[WidgetPrefsKeys.THEME_MODE] ?: "system"

            GlanceTheme {
                WidgetContent(context, mainLesson, currentLesson != null, themeMode)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, lesson: TimetableEntry?, isCurrent: Boolean, themeMode: String) {
        val isDarkThemeSystem = androidx.compose.foundation.isSystemInDarkTheme() 
        val useDark = themeMode == "dark" || (themeMode == "system" && isDarkThemeSystem)
        
        val backgroundColor = if (useDark) Color(0xFF1C1A20) else Color(0xFFD3BAFF)
        val pillColor = if (useDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.3f)
        val textColor = if (useDark) Color.White else Color(0xFF3B2577)
        val secondaryTextColor = if (useDark) Color.White.copy(alpha = 0.7f) else Color(0xFF6B58A1)


        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.padding(16.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // Header: JETZT / NÄCHSTER
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = when {
                        isCurrent -> "JETZT"
                        lesson != null -> "NÄCHSTER"
                        else -> "FREI"
                    }
                    Box(
                        modifier = GlanceModifier
                            .background(pillColor)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = ColorProvider(textColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(GlanceModifier.height(8.dp))

                // Subject
                Text(
                    text = lesson?.subject ?: "Schulfrei",
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                if (lesson != null) {
                    Text(
                        text = "Raum ${lesson.room}",
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val timeStr = if (lesson != null) "${lesson.startTime} - ${lesson.endTime}" else "--:--"
                    Text(
                        text = timeStr,
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
