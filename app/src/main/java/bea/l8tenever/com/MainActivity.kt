package bea.l8tenever.com

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import bea.l8tenever.com.alarm.AlarmService
import bea.l8tenever.com.ui.screens.DashboardScreen
import bea.l8tenever.com.ui.screens.LoginScreen
import bea.l8tenever.com.ui.screens.SettingsScreen
import bea.l8tenever.com.ui.screens.TimetableScreen
import bea.l8tenever.com.ui.theme.YunaTheme
import bea.l8tenever.com.viewmodel.MainViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAlarmIntent(intent)
        requestNotificationPermission()
        requestBatteryOptimizationExemption()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayModes = display?.supportedModes
            val maxRefreshRate = displayModes?.maxByOrNull { it.refreshRate }?.refreshRate ?: 60f
            if (maxRefreshRate > 60f) {
                window.attributes = window.attributes.apply { preferredRefreshRate = maxRefreshRate }
            }
        }

        setContent {
            val state by viewModel.state.collectAsState()
            val darkTheme = when(state.themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            YunaTheme(darkTheme = darkTheme, dynamicColor = state.useDynamicColors) {
                YunaApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAlarmIntent(intent)
    }

    private fun handleAlarmIntent(intent: Intent?) {
        if (intent?.getStringExtra("action") == "dismiss_alarm") {
            stopService(Intent(this, AlarmService::class.java))
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) {
                    try { startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) } catch (_: Exception) {}
                }
            }
        }
    }
}

// ─── Haupt-Composable ─────────────────────────────────────────────────────────
@Composable
fun YunaApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()

    // Login-Screen (kein NavHost)
    if (!state.isLoggedIn) {
        LoginScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding),
            enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(300)) },
            exitTransition   = { slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(300)) },
            popEnterTransition  = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(300)) },
            popExitTransition   = { slideOutHorizontally(tween(350)) { it } + fadeOut(tween(300)) }
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel             = viewModel,
                    onNavigateToTimetable = { date ->
                        date?.let { viewModel.setSelectedDate(it) }
                        navController.navigate("timetable")
                    },
                    onNavigateToSettings  = { navController.navigate("settings") }
                )
            }
            composable("timetable") {
                TimetableScreen(
                    viewModel      = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel      = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}