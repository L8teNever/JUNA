package bea.l8tenever.com.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.*
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.currentState
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
import java.util.*

class TimetableListWidget : GlanceAppWidget() {
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

        val today = LocalDate.now().toString()
        val todayEntries = timetable.filter { it.date == today }.sortedBy { it.startTime }

        provideContent {
            val widgetPrefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val range = widgetPrefs[WidgetPrefsKeys.LIST_RANGE] ?: "today"
            val themeMode = widgetPrefs[WidgetPrefsKeys.THEME_MODE] ?: "system"
            
            val displayEntries = if (range == "next3") {
                val end = LocalDate.now().plusDays(3).toString()
                timetable.filter { it.date >= today && it.date <= end }.sortedWith(compareBy({ it.date }, { it.startTime }))
            } else {
                todayEntries
            }

            GlanceTheme {
                ListContent(context, displayEntries, themeMode)
            }
        }
    }

    @Composable
    private fun ListContent(context: Context, lessons: List<TimetableEntry>, themeMode: String) {
        val isDark = themeMode == "dark" || (themeMode == "system" && androidx.compose.foundation.isSystemInDarkTheme() /* simplified, usually use GlanceTheme for system */)
        val backgroundColor = if (themeMode == "light") Color(0xFFF0F0F0) else Color(0xFF07070C)
        val cardColor = if (themeMode == "light") Color(0xFFFFFFFF) else Color(0xFF1C1A20)
        val accentColor = Color(0xFFD3BAFF)
        val errorColor = Color(0xFFFF6B6B)
        val mainText = if (themeMode == "light") Color.Black else Color.White

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HEUTIGER PLAN",
                    style = TextStyle(color = ColorProvider(accentColor), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }

            if (lessons.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "KEIN UNTERRICHT", style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.3f)), fontSize = 12.sp))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(lessons) { lesson ->
                        LessonItem(lesson, cardColor, accentColor, errorColor, mainText)
                    }
                }
            }
        }
    }

    @Composable
    private fun LessonItem(lesson: TimetableEntry, cardColor: Color, accentColor: Color, errorColor: Color, mainText: Color) {
        val textColor = if (lesson.isCancelled) errorColor else mainText
        
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(cardColor)
                .cornerRadius(12.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${lesson.subject} (${lesson.date})",
                    style = TextStyle(color = ColorProvider(textColor), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "${lesson.startTime} • Raum ${lesson.room}",
                    style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.5f)), fontSize = 11.sp)
                )
            }
            if (lesson.isCancelled) {
                Text(
                    text = "AUSFALL",
                    style = TextStyle(color = ColorProvider(errorColor), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
