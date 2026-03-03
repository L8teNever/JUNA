package bea.l8tenever.com.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val YunaDarkColorScheme = darkColorScheme(
    primary          = BeaPrimary,
    onPrimary        = Color.White,
    primaryContainer = BeaPrimaryDark,
    secondary        = BeaSecondary,
    onSecondary      = Color(0xFF002116),
    background       = BeaBackground,
    onBackground     = BeaOnBackground,
    surface          = BeaSurface,
    onSurface        = BeaOnSurface,
    surfaceVariant   = BeaSurfaceVariant,
    error            = BeaError,
    onError          = Color.White,
)

private val YunaLightColorScheme = lightColorScheme(
    primary          = BeaPrimary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary        = BeaSecondary,
    onSecondary      = Color.White,
    background       = Color.White,
    onBackground     = Color.Black,
    surface          = Color(0xFFFDF7FF),
    onSurface        = Color.Black,
    surfaceVariant   = Color(0xFFE7E0EB),
    error            = Color(0xFFBA1A1A),
    onError          = Color.White,
)

@Composable
fun YunaTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> YunaDarkColorScheme
        else -> YunaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}