package bea.l8tenever.com.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bea.l8tenever.com.ui.theme.YunaTheme
import bea.l8tenever.com.viewmodel.MainViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val subject = intent.getStringExtra("lesson_subject") ?: "UNTERRICHT"
        val title   = intent.getStringExtra("alarm_title") ?: "ZEIT FÜR DIE SCHULE"
        val showWeather = intent.getBooleanExtra("show_weather", false)

        setContent {
            val state by viewModel.state.collectAsState()
            val darkTheme = when(state.themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            YunaTheme(darkTheme = darkTheme, dynamicColor = state.useDynamicColors) {
                AlarmFullScreen(
                    subject = subject,
                    title = title,
                    showWeather = showWeather,
                    onStartDismiss = {
                        // Stop vibration and sound immediately
                        stopService(Intent(this, AlarmService::class.java))
                    },
                    onDismissComplete = {
                        // Close activity after the overlay finishes
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AlarmFullScreen(subject: String, title: String, showWeather: Boolean, onStartDismiss: () -> Unit, onDismissComplete: () -> Unit) {
    var currentTime by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var isDismissed by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        while (true) {
            currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            kotlinx.coroutines.delay(1000)
        }
    }

    if (isDismissed) {
        LaunchedEffect(Unit) {
            onStartDismiss() // Stop vibration/sound NOW
            // The activity finish is now handled inside the Overlay's LaunchedEffect
            // to stay synchronized with the exit animation.
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDismissed) 0f else if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "contentAlpha"
    )

    val contentScale by animateFloatAsState(
        targetValue = if (isDismissed) 0.8f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "contentScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 64.dp, top = 32.dp)
                    .graphicsLayer(
                        alpha = contentAlpha,
                        scaleX = contentScale,
                        scaleY = contentScale
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // Clump towards top
            ) {
                // --- TOP SPACER (Fixed gap) ---
                Spacer(modifier = Modifier.height(80.dp))

                // --- HEADER & CLOCK ---
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Text(
                        text = title.uppercase(),
                        color = Color(0xFFD3BAFF).copy(alpha = pulseAlpha),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = currentTime,
                        color = Color.White,
                        fontSize = 130.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-4).sp
                    )
                }

                if (showWeather) {
                    Spacer(modifier = Modifier.height(32.dp))
                    WeatherPillComponent()
                }

                // --- FLEXIBLE SPACER (pushes clock up and slider down) ---
                Spacer(modifier = Modifier.weight(1f))

                // --- SWIPE TO DISMISS ---
                SwipeToDismissSlider(
                    text = "AUSSCHALTEN",
                    onDismiss = { isDismissed = true }
                )

                // --- BOTTOM SPACER (Moves slider up) ---
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // --- SUCCESS OVERLAY ---
        if (isDismissed) {
            val overlayAlpha = remember { Animatable(0f) }
            val overlayScale = remember { Animatable(0.6f) }

            LaunchedEffect(Unit) {
                // 1. Entry Animation
                launch {
                    overlayAlpha.animateTo(1f, tween(600))
                }
                overlayScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
                
                // 2. Stay visible for a moment
                kotlinx.coroutines.delay(1200)
                
                // 3. Exit Animation
                launch {
                    overlayAlpha.animateTo(0f, tween(500))
                }
                overlayScale.animateTo(1.2f, tween(500, easing = FastOutSlowInEasing)) // Zoom out effect
                
                // 4. Finally close the activity
                onDismissComplete()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .alpha(overlayAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer(scaleX = overlayScale.value, scaleY = overlayScale.value)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFD3BAFF),
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "GUTEN MORGEN",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeToDismissSlider(text: String, onDismiss: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Clean pulse animation for the thumb when idle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        label = "offset",
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessLow)
    )

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dragScale"
    )

    // Shimmer effect for text
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(42.dp))
            .background(Color(0xFF1C1A20))
            .padding(8.dp)
    ) {
        val thumbSize = 68.dp
        val maxPx = with(androidx.compose.ui.platform.LocalDensity.current) { (maxWidth - thumbSize - 16.dp).toPx() }
        val progress = (offsetX / maxPx).coerceIn(0f, 1f)

        // Progress Fill (Glow)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { (animatedOffsetX + thumbSize.toPx() / 2).toDp() })
                .alpha((progress * 5f).coerceIn(0f, 1f)) // Hide at start, fade in quickly
                .clip(RoundedCornerShape(42.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3B2577).copy(alpha = 0.6f), Color(0xFFD3BAFF).copy(alpha = 0.3f))
                    )
                )
        )

        // Track text with Shimmer
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = (0.2f * (1f - progress)).coerceAtLeast(0f)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = (0.6f * (1f - progress)).coerceAtLeast(0f)
                }.drawWithContent {
                    val maskWidth = 200f
                    val center = shimmerTranslate
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.White,
                            1f to Color.Transparent,
                            startX = center - maskWidth,
                            endX = center + maskWidth
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
                    )
                }
            )
        }

        // The Sliding Thumb
        val finalScale = if (isDragging) dragScale else pulseScale
        
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .graphicsLayer(scaleX = finalScale, scaleY = finalScale)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color(0xFFD3BAFF))
                .pointerInput(maxPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            if (offsetX >= maxPx * 0.8f) {
                                offsetX = maxPx
                                onDismiss()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxPx)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFF3B2577),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun WeatherPillComponent() {
    var weatherData by remember { mutableStateOf<bea.l8tenever.com.data.WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        weatherData = bea.l8tenever.com.data.WeatherHelper.fetchCurrentWeather()
        isLoading = false
    }

    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wetter laden...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        } else {
            val w = weatherData
            if (w != null) {
                // Determine icon based on WMO code
                val icon = when (w.weatherCode) {
                    0 -> if (w.isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
                    1, 2, 3 -> Icons.Outlined.Cloud
                    61, 63, 65, 80, 81, 82 -> Icons.Outlined.Grain
                    71, 73, 75, 77, 85, 86 -> Icons.Outlined.Grain
                    95, 96, 99 -> Icons.Outlined.Thunderstorm
                    else -> Icons.Outlined.Cloud
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Wetter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${Math.round(w.temperature)}° ${w.description}",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Text(
                    text = "Wetter nicht verfügbar",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
