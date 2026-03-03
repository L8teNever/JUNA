package bea.l8tenever.com.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bea.l8tenever.com.ui.theme.*
import bea.l8tenever.com.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var currentCategory by remember { mutableStateOf(SettingsCategory.MAIN) }
    var alarmEnabled by remember(state.alarmEnabled) { mutableStateOf(state.alarmEnabled) }
    var alarmMinutes by remember(state.alarmMinutes) { mutableStateOf(state.alarmMinutes) }
    var sliderValue  by remember(state.alarmMinutes) { mutableFloatStateOf(state.alarmMinutes.toFloat()) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddCustomAlarmDialog by remember { mutableStateOf(false) }
    var autoDndEnabled by remember(state.autoDndEnabled) { mutableStateOf(state.autoDndEnabled) }
    var autoVolumeEnabled by remember(state.autoVolumeEnabled) { mutableStateOf(state.autoVolumeEnabled) }
    var autoVolumeRing by remember(state.autoVolumeRing) { mutableStateOf(state.autoVolumeRing) }
    var autoVolumeNotification by remember(state.autoVolumeNotification) { mutableStateOf(state.autoVolumeNotification) }
    var autoVolumeMedia by remember(state.autoVolumeMedia) { mutableStateOf(state.autoVolumeMedia) }
    var dndPauseBehavior by remember(state.dndPauseBehavior) { mutableStateOf(state.dndPauseBehavior) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Berechtigungs-Status live prÃ¼fen
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    val isBatteryOptimizationIgnored = remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else true
        )
    }
    val hasExactAlarmPermission = remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager)
                    .canScheduleExactAlarms()
            } else true
        )
    }

    // Status neu prÃ¼fen wenn User von den Android-Einstellungen zurÃ¼ckkommt
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    isBatteryOptimizationIgnored.value = pm.isIgnoringBatteryOptimizations(context.packageName)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    hasExactAlarmPermission.value = (context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close Button
                IconButton(
                    onClick = {
                        if (currentCategory == SettingsCategory.MAIN) {
                            onNavigateBack()
                        } else {
                            currentCategory = SettingsCategory.MAIN
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1A20))
                ) {
                    Icon(
                        imageVector = if (currentCategory == SettingsCategory.MAIN) Icons.Default.Close else Icons.Default.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = currentCategory.title.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                // Invisible spacer for centering
                Spacer(modifier = Modifier.size(42.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                            when (currentCategory) {
                    SettingsCategory.MAIN -> {
                        SettingsMenuCard(
                            title = "Weck-Einstellungen",
                            icon = Icons.Outlined.Alarm,
                            onClick = { currentCategory = SettingsCategory.ALARM }
                        )
                        SettingsMenuCard(
                            title = "Benachrichtigungen",
                            icon = Icons.Outlined.NotificationsActive,
                            onClick = { currentCategory = SettingsCategory.NOTIFICATIONS }
                        )
                        SettingsMenuCard(
                            title = "System & Berechtigungen",
                            icon = Icons.Outlined.Security,
                            onClick = { currentCategory = SettingsCategory.SYSTEM }
                        )
                        SettingsMenuCard(
                            title = "Design & Optik",
                            icon = Icons.Outlined.Palette,
                            onClick = { currentCategory = SettingsCategory.DESIGN }
                        )
                        SettingsMenuCard(
                            title = "Konto & Info",
                            icon = Icons.Outlined.Person,
                            onClick = { currentCategory = SettingsCategory.ACCOUNT }
                        )
                    }
                    SettingsCategory.ALARM -> {
// --- ALARM SEKTION ---
            SettingsSectionHeader("Wach-Logik", Icons.Outlined.Timer)

            SettingsFeatureCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Alarm, null, tint = Color(0xFFD3BAFF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Unterrichts-Wecker",
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            fontSize   = 16.sp
                        )
                        Text(
                            "Klingelt passend zur 1. Stunde",
                            color   = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked         = alarmEnabled,
                        onCheckedChange = {
                            alarmEnabled = it
                            viewModel.updateAlarmSettings(it, alarmMinutes)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor       = Color(0xFFD3BAFF),
                            checkedTrackColor       = Color(0xFF533F85).copy(alpha = 0.6f),
                            uncheckedThumbColor     = Color.LightGray,
                            uncheckedTrackColor     = Color.DarkGray
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }

                AnimatedVisibility(visible = alarmEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Aufweckzeit vor Schulbeginn",
                                color     = Color.White,
                                fontSize  = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            // GroÃŸer Minuten-Anzeiger
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF3B2577))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text       = "${alarmMinutes} Min",
                                    color      = Color(0xFFD3BAFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Slider(
                            value         = sliderValue,
                            onValueChange = {
                                sliderValue  = kotlin.math.round(it)
                                alarmMinutes = sliderValue.toInt()
                            },
                            onValueChangeFinished = {
                                viewModel.updateAlarmSettings(alarmEnabled, alarmMinutes)
                            },
                            valueRange = 1f..120f,
                            colors     = SliderDefaults.colors(
                                thumbColor       = Color(0xFFD3BAFF),
                                activeTrackColor = Color(0xFF533F85),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )

                        // Schnellauswahl-Chips
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 5, 15, 30, 45, 60, 90).forEach { min ->
                                val selected = alarmMinutes == min
                                FilterChip(
                                    selected = selected,
                                    onClick  = {
                                        alarmMinutes = min
                                        sliderValue  = min.toFloat()
                                        viewModel.updateAlarmSettings(alarmEnabled, min)
                                    },
                                    label = { Text("${min}m", fontSize = 12.sp, color = if (selected) Color(0xFF3B2577) else Color.White) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD3BAFF),
                                        containerColor         = Color(0xFF1C1A20),
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        enabled = true,
                                        selected = selected
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ErklÃ¤rung
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    null,
                                    tint     = Color(0xFFD3BAFF).copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text    = "Der Wecker klingelt $alarmMinutes Minuten vor der ersten Schulstunde.\n" +
                                              "Wenn die erste Stunde ausfällt, wird er automatisch zur nächsten Stunde verschoben.",
                                    color   = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test-Button
                        OutlinedButton(
                            onClick = { 
                                bea.l8tenever.com.alarm.AlarmScheduler.testAlarm(context) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD3BAFF)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF533F85))
                        ) {
                            Icon(Icons.Outlined.NotificationsActive, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alarm testen (in 5 Sek.)", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // --- NEXT ALARM INFO ---
            if (state.nextAlarmInfo.isNotBlank()) {
                SettingsFeatureCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AlarmOn,
                            null,
                            tint     = Color(0xFFD3BAFF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Nächster Alarm",
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                                fontSize   = 14.sp
                            )
                            Text(
                                state.nextAlarmInfo,
                                color   = Color(0xFFD3BAFF),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // --- WECKER-TEMPLATES ---
            var showTemplateDialog by remember { mutableStateOf(false) }
            var editingTemplate by remember { mutableStateOf<bea.l8tenever.com.data.AlarmTemplate?>(null) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Style, null, tint = Color(0xFFD3BAFF), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Wecker-Templates", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp)
                }
                IconButton(onClick = { editingTemplate = null; showTemplateDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, null, tint = Color(0xFFD3BAFF), modifier = Modifier.size(20.dp))
                }
            }

            if (state.templates.isEmpty()) {
                SettingsFeatureCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Noch keine Templates.\nErstelle ein Template mit dem + Button.",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            } else {
                SettingsFeatureCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        state.templates.forEachIndexed { index, tmpl ->
                            if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle
                                Switch(
                                    checked = tmpl.isEnabled,
                                    onCheckedChange = { viewModel.toggleTemplate(tmpl.id, it) },
                                    modifier = Modifier.scale(0.85f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD3BAFF),
                                        checkedTrackColor = Color(0xFF533F85).copy(alpha = 0.6f),
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.DarkGray
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tmpl.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        // Wochentag-Badges
                                        val dayNames = mapOf(1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do", 5 to "Fr")
                                        if (tmpl.activeDays.isNotEmpty()) {
                                            (1..5).forEach { d ->
                                                val active = d in tmpl.activeDays
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (active) Color(0xFF3B2577).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                                                ) {
                                                    Text(dayNames[d] ?: "", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        color = if (active) Color(0xFFD3BAFF) else Color.White.copy(alpha = 0.3f),
                                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            }
                                            Spacer(Modifier.width(2.dp))
                                        } else {
                                            Text("Nur manuell", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.05f)) {
                                            Text("â° ${tmpl.minutesBefore} Min", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                color = Color.White.copy(alpha = 0.5f))
                                        }
                                        if (tmpl.customAlarms.isNotEmpty()) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFD3BAFF).copy(alpha = 0.12f)) {
                                                Text("+${tmpl.customAlarms.size} Zusatz", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    color = Color(0xFFD3BAFF))
                                            }
                                        }
                                    }
                                }
                                // Bearbeiten
                                IconButton(onClick = { editingTemplate = tmpl; showTemplateDialog = true }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Outlined.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                                // Löschen
                                IconButton(onClick = { viewModel.removeTemplate(tmpl.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF8A80).copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Template-Dialog
            if (showTemplateDialog) {
                TemplateEditDialog(
                    initial = editingTemplate,
                    onDismiss = { showTemplateDialog = false },
                    onSave = { tmpl ->
                        viewModel.saveTemplate(tmpl)
                        showTemplateDialog = false
                    }
                )
            }

            // --- ZUSATZWECKER ---
            SettingsSectionHeader("Zusatz-Timer", Icons.Outlined.NotificationsActive)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.customAlarms.forEach { customAlarm ->
                    SettingsFeatureCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customAlarm.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${customAlarm.minutesBefore} Min. vor Unterricht" + if(customAlarm.showWeather) " • Wetter" else "",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = customAlarm.isEnabled,
                                onCheckedChange = { viewModel.toggleCustomAlarm(customAlarm.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD3BAFF),
                                    checkedTrackColor = Color(0xFF533F85).copy(alpha = 0.6f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.DarkGray
                                ),
                        modifier = Modifier.scale(0.85f)
                            )
                            IconButton(onClick = { viewModel.removeCustomAlarm(customAlarm.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Löschen", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAddCustomAlarmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1A20), contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Zusatzwecker erstellen", fontWeight = FontWeight.Bold)
                }
            }

            
                    }
                    SettingsCategory.NOTIFICATIONS -> {
            SettingsCategoryTitle("BENACHRICHTIGUNGEN")
// --- LIVE INFO SEKTION ---
            SettingsSectionHeader("Live-Tracker", Icons.Outlined.GraphicEq)

            SettingsFeatureCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.History, null, tint = Color(0xFFD3BAFF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Status-Benachrichtigung",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            "Zeigt verbleibende Zeit im Sperrbildschirm",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = state.liveNotificationEnabled,
                        onCheckedChange = { viewModel.toggleLiveNotification(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFD3BAFF),
                            checkedTrackColor = Color(0xFF533F85).copy(alpha = 0.6f),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            SettingsSectionHeader("Schul-Modus", Icons.Outlined.School)

            SettingsFeatureCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DoNotDisturb, null, tint = Color(0xFFD3BAFF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nicht Stören während Unterricht",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            "Aktiviert DND automatisch in Schulstunden",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoDndEnabled,
                        onCheckedChange = {
                            autoDndEnabled = it
                            viewModel.toggleAutoDnd(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFD3BAFF),
                            checkedTrackColor = Color(0xFF533F85).copy(alpha = 0.6f),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            SettingsFeatureCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.VolumeOff, null, tint = Color(0xFFD3BAFF))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Lautstärke auf 0 während Unterricht",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                "Automatische Stummschaltung",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = autoVolumeEnabled,
                            onCheckedChange = {
                                autoVolumeEnabled = it
                                viewModel.toggleAutoVolume(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD3BAFF),
                                checkedTrackColor = Color(0xFF533F85).copy(alpha = 0.6f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    AnimatedVisibility(visible = autoVolumeEnabled) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))

                            CheckboxRow(
                                label = "Klingelton (Anrufe)",
                                checked = autoVolumeRing,
                                onCheckedChange = {
                                    autoVolumeRing = it
                                    viewModel.updateVolumeSettings(ring = it)
                                }
                            )

                            CheckboxRow(
                                label = "Benachrichtigungen",
                                checked = autoVolumeNotification,
                                onCheckedChange = {
                                    autoVolumeNotification = it
                                    viewModel.updateVolumeSettings(notification = it)
                                }
                            )

                            CheckboxRow(
                                label = "Medien (Musik/Videos)",
                                checked = autoVolumeMedia,
                                onCheckedChange = {
                                    autoVolumeMedia = it
                                    viewModel.updateVolumeSettings(media = it)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            SettingsFeatureCard {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Verhalten während Pausen",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    RadioButtonRow(
                        label = "Während Pausen deaktivieren",
                        description = "Stummschaltung wird in Pausen aufgehoben",
                        selected = dndPauseBehavior == "deactivate",
                        onClick = {
                            dndPauseBehavior = "deactivate"
                            viewModel.updatePauseBehavior("deactivate")
                        }
                    )

                    RadioButtonRow(
                        label = "Durchgehend aktiv bis Schulende",
                        description = "Bleibt aktiv auch in Pausen",
                        selected = dndPauseBehavior == "keep_active",
                        onClick = {
                            dndPauseBehavior = "keep_active"
                            viewModel.updatePauseBehavior("keep_active")
                        }
                    )
                }
            }




                    }
                    SettingsCategory.SYSTEM -> {
            SettingsCategoryTitle("SYSTEM & BERECHTIGUNGEN")
// --- HINTERGRUND-BERECHTIGUNGEN ---

            SettingsSectionHeader("Hintergrund-Wecker", Icons.Outlined.Security)

            SettingsFeatureCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 16.dp)) {

                    // 1. Batterie-Optimierung
                    val battOk = isBatteryOptimizationIgnored.value
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (battOk) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    else Color(0xFFFF5252).copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (battOk) Icons.Outlined.BatteryFull else Icons.Outlined.BatteryAlert,
                                null,
                                tint = if (battOk) Color(0xFF4CAF50) else Color(0xFFFF8A80),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Batterie-Optimierung",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                if (battOk) "✓ Ausgeschaltet – Wecker funktioniert" else "⚠ Aktiv – Wecker könnte nicht klingeln!",
                                fontSize = 11.sp,
                                color = if (battOk) Color(0xFF4CAF50) else Color(0xFFFF8A80)
                            )
                        }
                        if (!battOk) {
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                        isBatteryOptimizationIgnored.value = true
                                    } catch (_: Exception) {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8A80))
                            ) {
                                Text("Fixen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // 2. Exakte Alarme (Android 12+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                        val exactOk = hasExactAlarmPermission.value
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (exactOk) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else Color(0xFFFF5252).copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (exactOk) Icons.Outlined.Alarm else Icons.Outlined.AlarmOff,
                                    null,
                                    tint = if (exactOk) Color(0xFF4CAF50) else Color(0xFFFF8A80),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Exakte Alarme",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    if (exactOk) "✓ Erlaubt – Wecker klingelt pünktlich" else "⚠ Verweigert – Wecker könnte zu spät klingeln!",
                                    fontSize = 11.sp,
                                    color = if (exactOk) Color(0xFF4CAF50) else Color(0xFFFF8A80)
                                )
                            }
                            if (!exactOk) {
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                            hasExactAlarmPermission.value = true
                                        } catch (_: Exception) { }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8A80))
                                ) {
                                    Text("Fixen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Nicht stören-Zugriff
            SettingsSectionHeader("DND-Steuerung", Icons.Outlined.NotificationsActive)
            SettingsFeatureCard {
                val dndOk = state.hasDndPermission
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("SettingsScreen", "Fehler beim Öffnen der DND Settings", e)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (dndOk) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else Color(0xFFFF5252).copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (dndOk) Icons.Outlined.CheckCircle else Icons.Outlined.DoNotDisturbOn,
                            null,
                            tint = if (dndOk) Color(0xFF4CAF50) else Color(0xFFFF8A80),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nicht stören-Zugriff",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            if (dndOk) "✓ Erlaubt – Schulmodus bereit" else "⚠ Erforderlich für automatische Stummschaltung",
                            fontSize = 11.sp,
                            color = if (dndOk) Color(0xFF4CAF50) else Color(0xFFFF8A80)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        SettingsCategory.DESIGN -> {
            SettingsCategoryTitle("DESIGN & OPTIK")
            
            SettingsSectionHeader("Material You", Icons.Outlined.ColorLens)
            SettingsFeatureCard {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamische Farben", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                            Text("Farben an System/Wallpaper anpassen", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = state.useDynamicColors,
                            onCheckedChange = { viewModel.updateThemeSettings(state.themeMode, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD3BAFF))
                        )
                    }
                }
            }

            SettingsSectionHeader("Erscheinungsbild", Icons.Outlined.DarkMode)
            SettingsFeatureCard {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    RadioButtonRow(
                        label = "Systemstandard",
                        description = "Folgt den Android-Systemeinstellungen",
                        selected = state.themeMode == "system",
                        onClick = { viewModel.updateThemeSettings("system", state.useDynamicColors) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    RadioButtonRow(
                        label = "Hell",
                        description = "Klassisches helles Design",
                        selected = state.themeMode == "light",
                        onClick = { viewModel.updateThemeSettings("light", state.useDynamicColors) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    RadioButtonRow(
                        label = "Dunkel",
                        description = "Schonendes dunkles Design",
                        selected = state.themeMode == "dark",
                        onClick = { viewModel.updateThemeSettings("dark", state.useDynamicColors) }
                    )
                }
            }
        }
        SettingsCategory.ACCOUNT -> {
            SettingsCategoryTitle("KONTO & INFO")
// --- KONTO SEKTION ---
            SettingsSectionHeader("Account-Link", Icons.Outlined.AccountCircle)

            SettingsFeatureCard {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                    // Aktuell eingeloggt als...
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, null, tint = Color(0xFFD3BAFF))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                state.username.ifBlank { "â€”" },
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                                fontSize   = 15.sp
                            )
                            Text(
                                "${state.school} · ${state.serverUrl.removePrefix("https://").removePrefix("http://").takeLast(30)}",
                                color    = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Ausloggen Button
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = CircleShape,
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor  = Color(0xFFFF8A80)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abmelden", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // --- APP INFO ---

            SettingsSectionHeader("YUNA Info", Icons.Outlined.Info)
            SettingsFeatureCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(20.dp)) {
                    InfoRow("Inhaber", state.username.ifBlank { "Unbekannt" })
                    InfoRow("Entwicklung", "L8teNever Software")
                    InfoRow("Version", "1.0.4 Premium-Ready")
                    InfoRow("Frequenz", "120Hz Hyper-Smooth")
                }
            }
                    }
                                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout-BestÃ¤tigungs-Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon             = { Icon(Icons.Outlined.Logout, null, tint = Color(0xFFFF8A80)) },
            title            = { Text("Abmelden?", fontWeight = FontWeight.Bold, color = Color.White) },
            text             = { Text("Alle gespeicherten Daten und der Alarm werden gelöscht.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton    = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) { Text("Abmelden", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Abbrechen", color = Color.White.copy(alpha = 0.5f))
                }
            },
            containerColor = Color(0xFF1C1A20)
        )
    }

    // Custom Alarm erstellen Dialog
    if (showAddCustomAlarmDialog) {
        var newName by remember { mutableStateOf("") }
        var newMinutes by remember { mutableFloatStateOf(10f) }
        var showWeather by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCustomAlarmDialog = false },
            title = { Text("Neuer Zusatzwecker", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name (z.B. Zähneputzen)", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFFD3BAFF),
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
                            cursorColor = Color(0xFFD3BAFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Klingeln: ${newMinutes.toInt()} Min vor Schulbeginn", color = Color.White)
                    Slider(
                        value = newMinutes,
                        onValueChange = { newMinutes = kotlin.math.round(it) },
                        valueRange = 1f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD3BAFF),
                            activeTrackColor = Color(0xFF533F85),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showWeather = !showWeather },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showWeather,
                            onCheckedChange = { showWeather = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD3BAFF), uncheckedColor = Color.White.copy(alpha = 0.5f))
                        )
                        Text("Zusätzlich aktuelles Wetter anzeigen", color = Color.White, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCustomAlarm(newName.trim(), newMinutes.toInt(), showWeather)
                        }
                        showAddCustomAlarmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2577))
                ) {
                    Text("Hinzufügen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomAlarmDialog = false }) {
                    Text("Abbrechen", color = Color.White.copy(alpha = 0.5f))
                }
            },
            containerColor = Color(0xFF1C1A20)
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            null, 
            modifier = Modifier.size(18.dp), 
            tint = Color(0xFFD3BAFF)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = title.uppercase(),
            fontSize   = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color(0xFFD3BAFF),
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun SettingsFeatureCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        content = content
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TemplateEditDialog(
    initial: bea.l8tenever.com.data.AlarmTemplate?,
    onDismiss: () -> Unit,
    onSave: (bea.l8tenever.com.data.AlarmTemplate) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var minutes by remember { mutableFloatStateOf((initial?.minutesBefore ?: 60).toFloat()) }
    var activeDays by remember { mutableStateOf(initial?.activeDays ?: emptySet()) }
    var customAlarms by remember { mutableStateOf(initial?.customAlarms ?: emptyList()) }
    var newCustomName by remember { mutableStateOf("") }
    var newCustomMinutes by remember { mutableStateOf("60") }
    var newCustomShowWeather by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                if (initial == null) "Template erstellen" else "Template bearbeiten",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template-Name", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    placeholder = { Text("z.B. Dusch-Tag", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Haupt-Wecker Minuten
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Haupt-Wecker", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("${minutes.toInt()} Min vor 1. Stunde", fontSize = 12.sp, color = Color(0xFFD3BAFF), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minutes,
                        onValueChange = { minutes = it },
                        valueRange = 10f..180f,
                        steps = 33,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD3BAFF),
                            activeTrackColor = Color(0xFF533F85),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Wochentage
                Column {
                    Text("Automatisch aktiv an:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val dayLabels = listOf(1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do", 5 to "Fr")
                        dayLabels.forEach { (day, label) ->
                            val isActive = day in activeDays
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) Color(0xFF3B2577) else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        activeDays = if (isActive) activeDays - day else activeDays + day
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(label, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) Color(0xFFD3BAFF) else Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                    Text("Leer lassen = nur manuell aktivierbar", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                }

                // Zusatz-Wecker
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Text("Zusatz-Wecker", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                customAlarms.forEach { ca ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ca.name, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("${ca.minutesBefore} Min vor 1. Stunde" + if(ca.showWeather) " • Wetter" else "", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        IconButton(onClick = { customAlarms = customAlarms.filter { it.id != ca.id } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF8A80).copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Neuen Zusatz-Alarm hinzufÃ¼gen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCustomName,
                        onValueChange = { newCustomName = it },
                        label = { Text("Name", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD3BAFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = newCustomMinutes,
                        onValueChange = { newCustomMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Min", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD3BAFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Checkbox(
                        checked = newCustomShowWeather,
                        onCheckedChange = { newCustomShowWeather = it },
                        modifier = Modifier.size(32.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD3BAFF), uncheckedColor = Color.White.copy(alpha = 0.5f))
                    )
                    IconButton(
                        onClick = {
                            val m = newCustomMinutes.toIntOrNull() ?: return@IconButton
                            if (newCustomName.isNotBlank()) {
                                customAlarms = customAlarms + bea.l8tenever.com.data.CustomAlarm(
                                    name = newCustomName.trim(), 
                                    minutesBefore = m, 
                                    showWeather = newCustomShowWeather
                                )
                                newCustomName = ""; newCustomMinutes = "60"; newCustomShowWeather = false
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color(0xFFD3BAFF))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val tmpl = (initial ?: bea.l8tenever.com.data.AlarmTemplate(name = "")).copy(
                            name = name.trim(),
                            minutesBefore = minutes.toInt(),
                            activeDays = activeDays,
                            customAlarms = customAlarms
                        )
                        onSave(tmpl)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2577))
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF533F85),
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp, start = 8.dp)
    )
}

enum class SettingsCategory(val title: String) {
    MAIN("SETUP"),
    ALARM("WECK-EINSTELLUNGEN"),
    NOTIFICATIONS("BENACHRICHTIGUNGEN"),
    SYSTEM("SYSTEM & BERECHTIGUNGEN"),
    DESIGN("DESIGN & OPTIK"),
    ACCOUNT("KONTO & INFO")
}

@Composable
fun SettingsMenuCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF262329))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color(0xFFD3BAFF), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Öffnen",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFD3BAFF),
                uncheckedColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RadioButtonRow(label: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFD3BAFF),
                unselectedColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}



