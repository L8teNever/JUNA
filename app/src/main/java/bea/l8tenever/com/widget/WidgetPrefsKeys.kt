package bea.l8tenever.com.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPrefsKeys {
    // Appearance: "system", "light", "dark"
    val THEME_MODE = stringPreferencesKey("widget_theme_mode")
    
    // For TimetableListWidget: "today", "week" (or number of days)
    val LIST_RANGE = stringPreferencesKey("widget_list_range")
    
    // Whether to use dynamic colors from system if available
    val USE_DYNAMIC_COLORS = booleanPreferencesKey("widget_use_dynamic")
}
