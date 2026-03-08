package bea.l8tenever.com.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bea.l8tenever.com.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTimetable: (LocalDate?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHabits: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val now = remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now.value = LocalDateTime.now()
        }
    }

    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val currentTime = now.value.toLocalTime()

    val todayEntries = remember(state.timetable, todayStr) {
        state.timetable
            .filter { it.date == todayStr && !it.isCancelled && it.subject != "Pause" }
            .sortedBy { it.startTime }
    }

    val currentLesson = todayEntries.firstOrNull { entry ->
        val start = LocalTime.parse(entry.startTime)
        val end = LocalTime.parse(entry.endTime)
        !currentTime.isBefore(start) && !currentTime.isAfter(end)
    }

    val nextLesson = if (currentLesson != null) {
        todayEntries.firstOrNull { LocalTime.parse(it.startTime).isAfter(currentTime) }
    } else {
        todayEntries.firstOrNull { LocalTime.parse(it.startTime).isAfter(currentTime) }
    }

    val todayEndTime = todayEntries.lastOrNull()?.endTime
    val nextDayFirstLesson = remember(state.timetable, todayStr) {
        state.timetable
            .filter { it.date > todayStr && !it.isCancelled && it.subject != "Pause" }
            .minByOrNull { it.date + " " + it.startTime }
    }

    val allTodayRaw = remember(state.timetable, todayStr) {
        state.timetable.filter { it.date == todayStr }.sortedBy { it.startTime }
    }

    val nextBreak = run {
        var breakFound: String? = null
        for (i in 0 until allTodayRaw.size - 1) {
            val cur = allTodayRaw[i]
            val nxt = allTodayRaw[i + 1]
            if (cur.endTime != nxt.startTime) {
                val breakStart = LocalTime.parse(cur.endTime)
                if (breakStart.isAfter(currentTime)) {
                    breakFound = cur.endTime
                    break
                }
            }
        }
        breakFound
    }

    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, dd. MMM.", Locale.GERMAN))
    
    // Parse alarm string (e.g. "Nächster Wecker: 08:55 Uhr · INFT" or "Wecker deaktiviert")
    val rawAlarmInfo = state.nextAlarmInfo
    var alarmTimeStr = "--:--"
    var alarmDescStr = "KEIN WECKER GEPLANT"
    if (rawAlarmInfo.contains("Uhr")) {
        // Simple extraction for visual flair
        val parts = rawAlarmInfo.removePrefix("Nächster Wecker: ").split(" Uhr")
        if (parts.size >= 2) {
            alarmTimeStr = parts[0].trim()
            val remaining = parts[1].replace("·", "•").trim()
            alarmDescStr = "MORGEN FRÜH $remaining".uppercase(Locale.getDefault())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Use theme background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = dateStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.WbCloudy,
                            contentDescription = "Weather",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "4°C • Winnenden",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Habits Button
                    IconButton(
                        onClick = onNavigateToHabits,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1A20))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Checklist,
                            contentDescription = "Aufgaben & Routinen",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // Settings Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1A20))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Einstellungen",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // --- MAIN CARD (JETZT) ---
            val mainLesson = currentLesson ?: nextLesson
            FeatureCard(
                onClick = { onNavigateToTimetable(today) },
                backgroundColor = Color(0xFFD3BAFF), // Light purple from image
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (currentLesson != null) "JETZT" else if (nextLesson != null) "NÄCHSTER" else "FREI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2577) // Dark purple
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Calendar",
                                tint = Color(0xFF6B58A1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = mainLesson?.subject ?: "Schulfrei",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3B2577),
                        letterSpacing = (-1.5).sp
                    )
                    
                    if (mainLesson != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fachgebiet • Raum ${mainLesson.room}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B58A1)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "Time",
                                tint = Color(0xFF533F85),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val timeStr = if (mainLesson != null) {
                                "${mainLesson.startTime} - ${mainLesson.endTime}"
                            } else {
                                val endedStr = todayEndTime?.let { "Heute aus: $it" }
                                val nextStartStr = nextDayFirstLesson?.let { "Morgen ab: ${it.startTime}" }
                                listOfNotNull(endedStr, nextStartStr).joinToString(" • ")
                            }
                            Text(
                                text = timeStr.ifEmpty { "--:--" },
                                fontSize = if (mainLesson == null) 11.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF533F85)
                            )
                        }
                        
                        if (mainLesson != null) {
                            Text(
                                text = "LEHRER: N.A.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B58A1)
                            )
                        }
                    }
                }
            }

            // --- MIDDLE ROW (NEXT & PAUSE) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Card: Next
                FeatureCard(
                    onClick = { onNavigateToTimetable(today) },
                    backgroundColor = Color(0xFF262329),
                    modifier = Modifier.weight(1f).height(190.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        val secondNext = if (currentLesson != null) {
                            nextLesson 
                        } else if (nextLesson == null) {
                            nextDayFirstLesson
                        } else {
                            null
                        }
                        
                        val nextLabel = if (currentLesson == null && nextLesson == null && nextDayFirstLesson != null) {
                            "MORGEN ERSTE"
                        } else {
                            "NÄCHSTER"
                        }

                        Text(
                            text = nextLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Column {
                            Text(
                                text = secondNext?.subject ?: "--",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 3,
                                lineHeight = 28.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (secondNext != null) "${secondNext.startTime} UHR - ${secondNext.room}" else "--",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Right Side: Pause & Remaining Time
                if (currentLesson != null || nextBreak != null) {
                    Column(
                        modifier = Modifier.weight(1f).height(190.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Right Card: Current Lesson Time Remaining (if any)
                        if (currentLesson != null) {
                            FeatureCard(
                                onClick = { onNavigateToTimetable(today) },
                                backgroundColor = Color(0xFF3B2577),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val endLocal = LocalTime.parse(currentLesson.endTime)
                                    val remain = java.time.Duration.between(currentTime, endLocal).toMinutes().coerceAtLeast(0)
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = "Timer",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${remain}m",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "VERBLEIBEND",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Bottom Right Card: Pause (Blob)
                        if (nextBreak != null) {
                            FeatureCard(
                                onClick = { onNavigateToTimetable(today) },
                                backgroundColor = Color(0xFF302C38),
                                shape = GenericShape { size, _ ->
                                    val w = size.width
                                    val h = size.height
                                    moveTo(w * 0.1f, h * 0.4f)
                                    cubicTo(w * 0.1f, h * 0.05f, w * 0.6f, -h * 0.05f, w * 0.9f, h * 0.2f)
                                    cubicTo(w * 1.1f, h * 0.4f, w * 1.05f, h * 0.8f, w * 0.8f, h * 0.95f)
                                    cubicTo(w * 0.4f, h * 1.1f, w * 0.05f, h * 0.8f, w * 0.1f, h * 0.4f)
                                    close()
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Coffee,
                                        contentDescription = "Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = nextBreak,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "PAUSE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- BOTTOM CARD (WECKER) ---
            FeatureCard(
                onClick = { /* Clicking could quick toggle or open settings */ },
                backgroundColor = Color(0xFF262329),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "WECKER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alarmTimeStr,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alarmDescStr,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = "Wecker",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Switch(
                            checked = state.alarmEnabled,
                            onCheckedChange = { viewModel.updateAlarmSettings(it, state.alarmMinutes) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD3BAFF), // Match main card
                                checkedTrackColor = Color(0xFF533F85).copy(alpha = 0.6f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    onClick: () -> Unit,
    backgroundColor: Color,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(26.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "scale",
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Surface(
        color = backgroundColor,
        shape = shape,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No ripple for clean look
                onClick = onClick
            )
    ) {
        content()
    }
}
