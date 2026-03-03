package bea.l8tenever.com.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WidgetConfigContent(onSave = { theme, range ->
                        saveSettings(theme, range)
                    })
                }
            }
        }
    }

    private fun saveSettings(theme: String, range: String) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
            
            // For general preferences we update global DataStore could be easier.
            // But per-widget preference is what users expect when configure activity opens.
            updateAppWidgetState(this@WidgetConfigurationActivity, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetPrefsKeys.THEME_MODE] = theme
                    this[WidgetPrefsKeys.LIST_RANGE] = range
                }
            }
            
            // Sync with specific widget types to be safe
            NextLessonWidget().update(this@WidgetConfigurationActivity, glanceId)
            LessonCountdownWidget().update(this@WidgetConfigurationActivity, glanceId)
            TimetableListWidget().update(this@WidgetConfigurationActivity, glanceId)
            AlarmStatusWidget().update(this@WidgetConfigurationActivity, glanceId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

@Composable
fun WidgetConfigContent(onSave: (String, String) -> Unit) {
    var theme by remember { mutableStateOf("system") }
    var range by remember { mutableStateOf("today") }

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
        Text("Widget konfigurieren", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))

        Text("Design", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ThemeOption("System-Standard", theme == "system") { theme = "system" }
        ThemeOption("Heller Modus", theme == "light") { theme = "light" }
        ThemeOption("Dunkler Modus", theme == "dark") { theme = "dark" }

        Spacer(Modifier.height(32.dp))
        Text("Stundenplan anzeige", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ThemeOption("Nur heute anzeigen", range == "today") { range = "today" }
        ThemeOption("Nächste 3 Tage anzeigen", range == "next3") { range = "next3" }

        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = { onSave(theme, range) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Konfiguration speichern", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
