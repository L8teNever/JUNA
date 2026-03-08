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
import androidx.compose.ui.graphics.Color
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
import bea.l8tenever.com.ui.screens.HabitsScreen
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
        // --- Früher Check auf NFC Intents für Transparenz ---
        checkIfTransparentThemeNeeded(intent)
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAlarmIntent(intent)
        handleNfcIntent(intent)
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
            
            // Handle Transparent Background when Toast is visible via NFC
            val isTransparent = state.isTransparentMode
            
            YunaTheme(darkTheme = darkTheme, dynamicColor = state.useDynamicColors) {
                Surface(
                    color = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    YunaApp(viewModel, this@MainActivity)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAlarmIntent(intent)
        handleNfcIntent(intent)
    }

    private fun checkIfTransparentThemeNeeded(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "juna" && uri.host == "nfc") {
            val action = uri.getQueryParameter("action") ?: "open"
            if (action == "increment" || action == "complete") {
                setTheme(R.style.Theme_BEA_Transparent)
            }
        }
    }

    private fun handleAlarmIntent(intent: Intent?) {
        if (intent?.getStringExtra("action") == "dismiss_alarm") {
            stopService(Intent(this, AlarmService::class.java))
        }
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        var uri: android.net.Uri? = intent.data

        if (uri == null && android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMsgs = intent.getParcelableArrayExtra(android.nfc.NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null && rawMsgs.isNotEmpty()) {
                val msg = rawMsgs[0] as android.nfc.NdefMessage
                val record = msg.records[0]
                if (record.tnf == android.nfc.NdefRecord.TNF_ABSOLUTE_URI || record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN) {
                    uri = record.toUri()
                }
            }
        }

        if (uri != null && uri.scheme == "juna" && uri.host == "nfc") {
            val pathSegments = uri.pathSegments
            if (pathSegments.isNotEmpty() && pathSegments[0] == "habit" && pathSegments.size > 1) {
                val habitId = pathSegments[1]
                val action = uri.getQueryParameter("action") ?: "open"
                viewModel.handleNfcAction(habitId, action)
            }
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
fun YunaApp(viewModel: MainViewModel, activity: ComponentActivity) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()

    // Animation Overlay for NFC Toast
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoggedIn) {
            LaunchedEffect(state.navigateToHabits) {
                if (state.navigateToHabits) {
                    if (navController.currentDestination?.route != "habits") {
                        navController.navigate("habits") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    viewModel.clearNavigateToHabits()
                }
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
                            onNavigateToSettings  = { navController.navigate("settings") },
                            onNavigateToHabits    = { navController.navigate("habits") }
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
                    composable("habits") {
                        HabitsScreen(
                            viewModel      = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        } else {
            // Login-Screen (kein NavHost)
            LoginScreen(viewModel = viewModel)
        }
        
        // --- NFC Habit Toast Animation Overlay ---
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = state.habitToast != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                state.habitToast?.let { toastText ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD3BAFF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF3B2577),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = toastText,
                                color = Color(0xFF3B2577),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.habitToast) {
        if (state.habitToast != null) {
            val wasTransparent = state.isTransparentMode
            kotlinx.coroutines.delay(3000)
            viewModel.clearHabitToast()
            if (wasTransparent) {
                activity.finish() 
            }
        }
    }
}