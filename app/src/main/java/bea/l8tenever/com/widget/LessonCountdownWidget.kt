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
import androidx.glance.appwidget.updateAll
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
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.*

class LessonCountdownWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.dataStore.data.first()
        val json = prefs[androidx.datastore.preferences.core.stringPreferencesKey("cached_timetable")]
        
        val timetable: List<TimetableEntry> = if (json.isNullOrBlank()) emptyList() else {
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

        provideContent {
            val widgetPrefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val themeMode = widgetPrefs[WidgetPrefsKeys.THEME_MODE] ?: "system"

            GlanceTheme {
                CountdownContent(context, currentLesson, now, themeMode)
            }
        }
    }

    @Composable
    private fun CountdownContent(context: Context, lesson: TimetableEntry?, now: LocalTime, themeMode: String) {
        val backgroundColor = if (themeMode == "light") Color(0xFFF0F0F0) else Color(0xFF1C1A20)
        val accentColor = Color(0xFFD3BAFF)
        val textColor = if (themeMode == "light") Color.Black else Color.White

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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (lesson != null) {
                    val endTime = LocalTime.parse(lesson.endTime)
                    val remaining = Duration.between(now, endTime).toMinutes()
                    
                    Text(
                        text = "NOCH",
                        style = TextStyle(color = ColorProvider(accentColor), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    
                    Text(
                        text = "$remaining MIN",
                        style = TextStyle(color = ColorProvider(textColor), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    )
                    
                    Text(
                        text = lesson.subject,
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.7f)), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                    
                    Text(
                        text = "Raum ${lesson.room}",
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.4f)), fontSize = 11.sp)
                    )
                } else {
                    Text(
                        text = "KEIN UNTERRICHT",
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.5f)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "GENIESS DIE PAUSE",
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.3f)), fontSize = 10.sp)
                    )
                }
            }
        }
    }
}
