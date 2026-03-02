package bea.l8tenever.com.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
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

@Composable
fun YunaTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> YunaDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}