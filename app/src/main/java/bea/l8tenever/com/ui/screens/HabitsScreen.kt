package bea.l8tenever.com.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import bea.l8tenever.com.data.Habit
import bea.l8tenever.com.data.HabitTimeType
import bea.l8tenever.com.data.HabitTrigger
import bea.l8tenever.com.utils.NfcWriter
import bea.l8tenever.com.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var nfcHabitToLink by remember { mutableStateOf<Habit?>(null) }

    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val todayDayOfWeek = today.dayOfWeek.value

    val habitsForToday = state.habits.filter { it.daysOfWeek.contains(todayDayOfWeek) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Aufgaben & Routinen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD3BAFF),
                contentColor = Color(0xFF3B2577)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (state.habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Aufgaben vorhanden.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "Heute anstehend",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    if (habitsForToday.isEmpty()) {
                        item {
                            Text("Heute hast du frei!", color = Color.Gray)
                        }
                    }

                    items(habitsForToday) { habit ->
                        val log = state.habitLogs.find { it.habitId == habit.id && it.date == todayStr }
                        val completedCount = log?.completedCount ?: 0
                        val isDone = completedCount >= habit.timesPerDay

                        HabitItem(
                            habit = habit,
                            completedCount = completedCount,
                            allLogs = state.habitLogs,
                            onIncrement = {
                                viewModel.logHabitCompleted(habit.id, today)
                            },
                            onDecrement = {
                                viewModel.removeHabitLog(habit.id, today)
                            },
                            onDelete = { viewModel.removeHabit(habit.id) },
                            onNfcClick = { nfcHabitToLink = habit }
                        )
                    }

                    val otherHabits = state.habits.filter { !it.daysOfWeek.contains(todayDayOfWeek) }
                    if (otherHabits.isNotEmpty()) {
                        item {
                            Text(
                                text = "Andere Tage",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                            )
                        }

                        items(otherHabits) { habit ->
                            HabitItem(
                                habit = habit,
                                completedCount = 0,
                                allLogs = state.habitLogs,
                                onIncrement = { },
                                onDecrement = { },
                                onDelete = { viewModel.removeHabit(habit.id) },
                                onNfcClick = { nfcHabitToLink = habit }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newHabit ->
                viewModel.addHabit(newHabit)
                showAddDialog = false
            }
        )
    }

    nfcHabitToLink?.let { habit ->
        NfcConfigDialog(
            habit = habit,
            onDismiss = { nfcHabitToLink = null }
        )
    }
}

@Composable
private fun HabitItem(
    habit: Habit,
    completedCount: Int,
    allLogs: List<bea.l8tenever.com.data.HabitLog>,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
    onNfcClick: () -> Unit
) {
    val isDone = completedCount >= habit.timesPerDay
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFF2E312F) else Color(0xFF262329)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) Color.Gray else Color.White
                )
                
                val details = buildList {
                    if (habit.timesPerDay > 1) {
                        add("${completedCount}/${habit.timesPerDay} mal")
                    }
                    if (habit.triggers.isNotEmpty()) {
                        add(formatTrigger(habit.triggers.first()))
                    }
                }
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.joinToString(" • "),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNfcClick) {
                    Icon(Icons.Default.Nfc, contentDescription = "NFC Tag", tint = Color.Gray.copy(alpha = 0.8f))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red.copy(alpha = 0.6f))
                }
                
                if (completedCount > 0 && habit.timesPerDay > 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B2577))
                            .clickable { onDecrement() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Rückgängig", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDone) Color(0xFF4CAF50) else Color.DarkGray)
                        .clickable { 
                            if (isDone) onDecrement() else onIncrement()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, contentDescription = "Erledigt", tint = Color.White)
                    } else if (habit.timesPerDay > 1) {
                        Icon(Icons.Default.Add, contentDescription = "Hinzufügen", tint = Color.White)
                    }
                }
            }
        }

        // Verlauf der letzten 7 Tage
        var expanded by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Verlauf einklappen" else "Verlauf ansehen",
                    fontSize = 12.sp,
                    color = Color(0xFFD3BAFF)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val today = LocalDate.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    for (i in 6 downTo 0) {
                        val date = today.minusDays(i.toLong())
                        val dateStr = date.format(formatter)
                        val logForDate = allLogs.find { it.habitId == habit.id && it.date == dateStr }
                        val dayCompleted = (logForDate?.completedCount ?: 0) >= habit.timesPerDay
                        val isDayRelevant = habit.daysOfWeek.contains(date.dayOfWeek.value)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date.dayOfWeek.name.take(2),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!isDayRelevant) Color.Transparent
                                        else if (dayCompleted) Color(0xFF4CAF50)
                                        else Color.DarkGray
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDayRelevant && dayCompleted) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                } else if (!isDayRelevant) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.DarkGray.copy(alpha=0.5f)))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun formatTrigger(trigger: HabitTrigger): String {
    return when (trigger.type) {
        HabitTimeType.SPECIFIC_TIME -> "${trigger.time} Uhr"
        HabitTimeType.BEFORE_FIRST_LESSON -> "${trigger.offsetMinutes} Min vor Stunde"
        HabitTimeType.BEFORE_BEDTIME -> "${trigger.offsetMinutes} Min vor Schlafen"
        HabitTimeType.AFTER_BEDTIME -> "${trigger.offsetMinutes} Min nach Aufwachen" // Assume after bedtime means morning/wakeup
        HabitTimeType.ANYTIME -> "Jederzeit"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (Habit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var timesPerDay by remember { mutableStateOf("1") }
    var triggerType by remember { mutableStateOf(HabitTimeType.ANYTIME) }
    var offsetMinutes by remember { mutableStateOf("20") }
    var specificTime by remember { mutableStateOf("08:00") }
    
    // Days
    val allDays = (1..7).toList()
    var selectedDays by remember { mutableStateOf(allDays.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Aufgabe") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name der Aufgabe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = timesPerDay,
                    onValueChange = { timesPerDay = it },
                    label = { Text("Wie oft am Tag?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tage", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                    allDays.forEachIndexed { index, day ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFD3BAFF) else Color.DarkGray)
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNames[index],
                                color = if (isSelected) Color(0xFF3B2577) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text("Zeitpunkt (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                
                // Dropdown for Type
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = formatTriggerType(triggerType),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        HabitTimeType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(formatTriggerType(t)) },
                                onClick = {
                                    triggerType = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (triggerType == HabitTimeType.SPECIFIC_TIME) {
                    OutlinedTextField(
                        value = specificTime,
                        onValueChange = { specificTime = it },
                        label = { Text("Zeit (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (triggerType != HabitTimeType.ANYTIME) {
                    OutlinedTextField(
                        value = offsetMinutes,
                        onValueChange = { offsetMinutes = it },
                        label = { Text("Minuten (z.B. 20)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val times = timesPerDay.toIntOrNull() ?: 1
                    val offset = offsetMinutes.toIntOrNull() ?: 0
                    
                    val trigger = HabitTrigger(
                        type = triggerType,
                        time = if (triggerType == HabitTimeType.SPECIFIC_TIME) specificTime else null,
                        offsetMinutes = offset
                    )
                    
                    val habit = Habit(
                        id = UUID.randomUUID().toString(),
                        name = name.ifBlank { "Unbenannt" },
                        daysOfWeek = selectedDays.ifEmpty { allDays.toSet() },
                        timesPerDay = times,
                        triggers = if (triggerType == HabitTimeType.ANYTIME) emptyList() else listOf(trigger)
                    )
                    onAdd(habit)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun formatTriggerType(type: HabitTimeType): String {
    return when (type) {
        HabitTimeType.ANYTIME -> "Jederzeit"
        HabitTimeType.SPECIFIC_TIME -> "Feste Uhrzeit"
        HabitTimeType.BEFORE_FIRST_LESSON -> "Vor der ersten Stunde"
        HabitTimeType.BEFORE_BEDTIME -> "Vor dem Schlafen"
        HabitTimeType.AFTER_BEDTIME -> "Nach dem Schlafen"
    }
}

@Composable
fun NfcConfigDialog(
    habit: Habit,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var isWriting by remember { mutableStateOf(false) }
    var writeMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    
    val nfcWriter = remember(activity) { activity?.let { NfcWriter(it) } }

    DisposableEffect(nfcWriter) {
        onDispose {
            nfcWriter?.stopWriting()
        }
    }

    if (isWriting) {
        AlertDialog(
            onDismissRequest = {
                nfcWriter?.stopWriting()
                onDismiss()
            },
            title = { Text("NFC Tag scannen") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (writeMessage == null) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color(0xFFD3BAFF))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bitte halte ein NFC Tag an die Rückseite deines Geräts.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else {
                        Text(writeMessage ?: "", color = if (writeMessage?.contains("Fehler") == true) Color.Red else Color.Green)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    nfcWriter?.stopWriting()
                    onDismiss()
                }) {
                    Text(if (writeMessage != null) "Fertig" else "Abbrechen")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("NFC Tag einrichten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Was soll passieren, wenn du den NFC Tag scannst?", color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    NfcActionItem(
                        text = "App öffnen",
                        description = "Öffnet die App und zeigt die Aufgaben",
                        selected = selectedAction == "open",
                        onClick = { selectedAction = "open" }
                    )
                    
                    NfcActionItem(
                        text = "Einmal erledigen",
                        description = "Aufgabe einmal für heute abhaken",
                        selected = selectedAction == "increment",
                        onClick = { selectedAction = "increment" }
                    )
                    
                    NfcActionItem(
                        text = "Komplett abschließen",
                        description = "Aufgabe für heute komplett als erledigt markieren",
                        selected = selectedAction == "complete",
                        onClick = { selectedAction = "complete" }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val act = selectedAction
                        if (act != null && nfcWriter != null) {
                            isWriting = true
                            nfcWriter.startWriting("juna://nfc/habit/${habit.id}?action=$act") { success, msg ->
                                writeMessage = msg
                            }
                        }
                    },
                    enabled = selectedAction != null
                ) {
                    Text("Weiter")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun NfcActionItem(
    text: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF533F85) else Color(0xFF262329)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
