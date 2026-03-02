package bea.l8tenever.com.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bea.l8tenever.com.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Macht die Activity sichtbar auch wenn das Handy gelockt / Display aus ist.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val subject = intent.getStringExtra("lesson_subject") ?: "Unterricht"
        val time    = intent.getStringExtra("lesson_time") ?: "Jetzt"
        val title   = intent.getStringExtra("alarm_title") ?: "⏰ Zeit aufzustehen!"

        setContent {
            YunaTheme {
                AlarmFullScreen(
                    subject = subject,
                    time = time,
                    title = title,
                    onDismiss = {
                        stopService(Intent(this, AlarmService::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AlarmFullScreen(subject: String, time: String, title: String, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    var swipeProgress by remember { mutableFloatStateOf(0f) }

    // Hintergrund-Animation (Farb-Drift)
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "colorShift"
    )

    // Dynamische Werte basierend auf dem Swipe-Fortschritt
    val clockScale = 1f - (swipeProgress * 0.15f)
    val bgGlowAlpha = 0.2f + (swipeProgress * 0.4f)
    val contentAlpha = 1f - (swipeProgress * 0.5f)

    val bgColor = lerp(
        Color.Black,
        Color(0xFF3B2577).copy(alpha = bgGlowAlpha),
        colorShift * (0.5f + swipeProgress * 0.5f) // Beim Swipen wird der Glow-Effekt stärker
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black,
                        bgColor,
                        Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = contentAlpha)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Oberer Bereich: Titel & Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp).graphicsLayer(scaleX = clockScale, scaleY = clockScale)
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color(0xFFD3BAFF)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = subject,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Mittlerer Bereich: RIESIGE UHR
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer(scaleX = clockScale, scaleY = clockScale)
            ) {
                Text(
                    text = time,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "GUTEN MORGEN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Unterer Bereich: Swipe-to-Dismiss
            Box(
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SwipeToDismissButton(
                    onDismiss = onDismiss,
                    onProgress = { swipeProgress = it }
                )
            }
        }
    }
}

@Composable
fun SwipeToDismissButton(onDismiss: () -> Unit, onProgress: (Float) -> Unit) {
    val density = LocalDensity.current
    val trackWidthDp = 300.dp
    val thumbSizeDp = 80.dp
    val trackWidthPx = with(density) { trackWidthDp.toPx() }
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    
    val maxDragPx = trackWidthPx - thumbSizePx
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var dismissed by remember { mutableStateOf(false) }

    val draggableState = rememberDraggableState { delta ->
        if (!dismissed) {
            coroutineScope.launch {
                val nextValue = (offsetX.value + delta).coerceIn(0f, maxDragPx)
                offsetX.snapTo(nextValue)
                onProgress(nextValue / maxDragPx)
            }
        }
    }

    LaunchedEffect(offsetX.value) {
        if (!dismissed && offsetX.value >= maxDragPx * 0.98f) {
            dismissed = true
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .width(trackWidthDp)
            .height(thumbSizeDp)
            .clip(CircleShape)
            .background(Color(0xFF1C1A20).copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        val progress = offsetX.value / maxDragPx
        val textAlpha = (1f - progress).coerceIn(0f, 1f)
        
        Text(
            text = "ZUM BEENDEN WISCHEN ➔",
            color = Color.White.copy(alpha = 0.6f * textAlpha),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Center).offset(x = 10.dp)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSizeDp)
                .padding(4.dp)
                .graphicsLayer(
                    scaleX = 1f + (progress * 0.1f),
                    scaleY = 1f + (progress * 0.1f),
                    rotationZ = progress * 45f
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF3B2577), Color(0xFFD3BAFF))
                    )
                )
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (!dismissed) {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    onProgress(value / maxDragPx)
                                }
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AlarmOff, 
                null, 
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
