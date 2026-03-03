package bea.l8tenever.com.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import bea.l8tenever.com.data.TimetableEntry
import bea.l8tenever.com.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // 7 days starting from today
    val today = remember(LocalDate.now()) { LocalDate.now() }
    val days = remember(today) { (0..6).map { today.plusDays(it.toLong()) } }

    val currentTime = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = LocalTime.now()
            kotlinx.coroutines.delay(30000) // Update twice a minute
        }
    }

    val datesWithLessons = remember(state.timetable) {
        state.timetable.filter { !it.isCancelled }.map { it.date }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(state.selectedDate) {
                var dragAmountX = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountX += dragAmount
                    },
                    onDragEnd = {
                        if (dragAmountX > 80) {
                            viewModel.setSelectedDate(state.selectedDate.minusDays(1))
                        } else if (dragAmountX < -80) {
                            viewModel.setSelectedDate(state.selectedDate.plusDays(1))
                        }
                    },
                    onDragCancel = {}
                )
            }
    ) {
        val selectedDateStr = state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val selectedHasLessons = datesWithLessons.contains(selectedDateStr)
            val isAlarmDisabledThisDay = state.alarmDisabledDates.contains(selectedDateStr)

            // --- UNIFIED TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1A20))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Day Navigation (Center)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setSelectedDate(state.selectedDate.minusDays(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Zurück", tint = Color.White)
                    }
                    val isToday = state.selectedDate == today
                    val isTomorrow = state.selectedDate == today.plusDays(1)
                    val dateText = when {
                        isToday -> "HEUTE"
                        isTomorrow -> "MORGEN"
                        else -> state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd.MM", Locale.GERMAN)).uppercase()
                    }
                    Text(
                        text = dateText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { viewModel.setSelectedDate(state.selectedDate.plusDays(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Vor", tint = Color.White)
                    }
                }

                // Actions: Alarm & Refresh
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.alarmEnabled && selectedHasLessons) {
                        IconButton(
                            onClick = { viewModel.toggleAlarmForDate(state.selectedDate, !isAlarmDisabledThisDay) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isAlarmDisabledThisDay) Color(0xFF1C1A20) else Color(0xFF3B2577))
                        ) {
                            Icon(
                                imageVector = if (isAlarmDisabledThisDay) Icons.Outlined.AlarmOff else Icons.Outlined.Alarm,
                                contentDescription = "Alarm",
                                tint = if (isAlarmDisabledThisDay) Color.White.copy(alpha = 0.4f) else Color(0xFFD3BAFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        var expanded by remember { mutableStateOf(false) }
                        val activeTemplate = state.oneTimeTemplate?.takeIf { it.date == selectedDateStr }
                        
                        Box {
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (activeTemplate != null) Color(0xFF3B2577) else Color(0xFF1C1A20))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Style,
                                    contentDescription = "Templates",
                                    tint = if (activeTemplate != null) Color(0xFFD3BAFF) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                shape = RoundedCornerShape(20.dp),
                                containerColor = Color(0xFF1C1A20),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.width(200.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Standard-Wecker", color = Color.White) },
                                    onClick = { 
                                        viewModel.clearOneTimeTemplate()
                                        expanded = false 
                                    },
                                    leadingIcon = {
                                        if (activeTemplate == null) {
                                            Icon(Icons.Default.Check, null, tint = Color(0xFFD3BAFF))
                                        }
                                    }
                                )
                                state.templates.forEach { tmpl ->
                                    DropdownMenuItem(
                                        text = { Text(tmpl.name, color = Color.White) },
                                        onClick = {
                                            viewModel.activateTemplateForDate(tmpl.id, state.selectedDate)
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            if (activeTemplate?.templateId == tmpl.id) {
                                                Icon(Icons.Default.Check, null, tint = Color(0xFFD3BAFF))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = { viewModel.fetchTimetable() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1A20))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Aktualisieren",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- LOADING STATE ---
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD3BAFF))
                }
            }

            // --- LIST OF LESSONS (ANIMATED) ---
            AnimatedContent(
                targetState = state.selectedDate,
                transitionSpec = {
                    val forward = targetState.isAfter(initialState)
                    (slideInHorizontally(tween(350)) { if (forward) it else -it } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(350)) { if (forward) -it else it } + fadeOut(tween(300)))
                },
                label = "daySwitchAnim"
            ) { targetDate ->
                val entries = remember(targetDate, state.timetable) {
                    viewModel.getEntriesForDate(targetDate)
                }

                if (entries.isEmpty() && !state.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.EventAvailable,
                            null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "SCHULFREI",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Genieße deinen Tag.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            val isCurrentlyPlaying = targetDate == today && entry.subject != "Pause" && !entry.isCancelled &&
                                    !currentTime.value.isBefore(LocalTime.parse(entry.startTime)) &&
                                    !currentTime.value.isAfter(LocalTime.parse(entry.endTime))
                                    
                            LessonCard(entry = entry, isCurrentlyPlaying = isCurrentlyPlaying)
                        }
                        item { Spacer(Modifier.height(80.dp)) } // Bottom padding
                    }
                }
            }
        }
    }



@Composable
fun LessonCard(entry: TimetableEntry, isCurrentlyPlaying: Boolean) {
    val isPause = entry.subject == "Pause"
    val isCancelled = entry.isCancelled

    val targetBgColor = when {
        isCurrentlyPlaying -> Color(0xFFD3BAFF)
        isPause            -> Color(0xFF302C38)
        isCancelled        -> Color(0xFF3B2020) // Reddish dark
        else               -> Color(0xFF262329) // Normal dark
    }

    val targetTextColor = when {
        isCurrentlyPlaying -> Color(0xFF3B2577)
        else               -> Color.White
    }

    val targetSubColor = when {
        isCurrentlyPlaying -> Color(0xFF6B58A1)
        else               -> Color.White.copy(alpha = 0.4f)
    }

    val bgColor by animateColorAsState(targetBgColor)
    val textColor by animateColorAsState(targetTextColor)
    val subColor by animateColorAsState(targetSubColor)

    TimetableFeatureCard(
        onClick = {},
        backgroundColor = bgColor,
        shape = if (isPause) RoundedCornerShape(50.dp) else RoundedCornerShape(20.dp), // Pill shape for pauses
        modifier = Modifier.fillMaxWidth().then(if (isPause) Modifier.height(64.dp) else Modifier)
    ) {
        if (isPause) {
            // Layout for Break / Pause
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Coffee,
                        contentDescription = "Break",
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "PAUSE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "${entry.startTime} - ${entry.endTime}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = subColor
                )
            }
        } else {
            // Layout for normal subject
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Top Row: Time | Cancelled Tag!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrentlyPlaying) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCurrentlyPlaying) "JETZT" else "${entry.startTime} - ${entry.endTime}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    if (isCancelled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "ENTFÄLLT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    } else if (isCurrentlyPlaying) {
                        Text(
                            text = "${entry.startTime} - ${entry.endTime}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = entry.subject,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCancelled) textColor.copy(alpha = 0.5f) else textColor,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Room",
                            tint = subColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.room,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Teacher",
                            tint = subColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.teacher,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subColor
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun TimetableFeatureCard(
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

