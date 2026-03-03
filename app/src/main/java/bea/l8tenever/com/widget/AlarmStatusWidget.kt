package bea.l8tenever.com.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.*
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.currentState
import androidx.compose.ui.unit.sp
import bea.l8tenever.com.MainActivity
import bea.l8tenever.com.data.dataStore
import bea.l8tenever.com.data.PrefsKeys
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class AlarmStatusWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.dataStore.data.first()
        val dateStr = prefs[PrefsKeys.NEXT_ALARM_DATE] ?: ""
        val timeStr = prefs[PrefsKeys.NEXT_ALARM_TIME] ?: ""
        
        val isTomorrow = if (dateStr.isNotEmpty()) {
            LocalDate.parse(dateStr) == LocalDate.now().plusDays(1)
        } else false
        
        val isToday = if (dateStr.isNotEmpty()) {
            LocalDate.parse(dateStr) == LocalDate.now()
        } else false

        provideContent {
            val widgetPrefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val themeMode = widgetPrefs[WidgetPrefsKeys.THEME_MODE] ?: "system"
            
            GlanceTheme {
                AlarmStatusContent(dateStr, timeStr, isToday, isTomorrow, themeMode)
            }
        }
    }

    @Composable
    private fun AlarmStatusContent(date: String, time: String, isToday: Boolean, isTomorrow: Boolean, themeMode: String) {
        val backgroundColor = if (themeMode == "light") Color(0xFFF0F0F0) else Color(0xFF141315)
        val accentColor = Color(0xFFD3BAFF)
        val textColor = if (themeMode == "light") Color.Black else Color.White

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.padding(12.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .size(8.dp)
                            .background(if (time.isNotEmpty()) accentColor else Color.Gray.copy(alpha = 0.5f))
                            .cornerRadius(4.dp)
                    ) {}
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = "WECKER STATUS",
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.5f)), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(GlanceModifier.height(8.dp))

                if (time.isNotEmpty()) {
                    val dayLabel = when {
                        isToday -> "HEUTE"
                        isTomorrow -> "MORGEN"
                        else -> date
                    }
                    
                    Text(
                        text = time,
                        style = TextStyle(color = ColorProvider(accentColor), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = dayLabel,
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.7f)), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                } else {
                    Text(
                        text = "DEAKTIVIERT",
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.3f)), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "KEIN ALARM GEPLANT",
                        style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.2f)), fontSize = 10.sp)
                    )
                }
            }
        }
    }
}
