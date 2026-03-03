package bea.l8tenever.com.alarm

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import bea.l8tenever.com.data.dataStore
import bea.l8tenever.com.ui.theme.YunaTheme
import bea.l8tenever.com.viewmodel.MainViewModel
import bea.l8tenever.com.worker.SleepReminderWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class SleepReminderActivity : ComponentActivity() {
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val vibrateOnly = intent.getBooleanExtra("vibrate_only", false)

        // Start vibration/sound
        startAlert(vibrateOnly)

        setContent {
            val viewModel: MainViewModel by viewModels()
            val state by viewModel.state.collectAsState()

            YunaTheme(darkTheme = true, dynamicColor = state.useDynamicColors) {
                SleepReminderScreen(
                    onDismiss = {
                        stopAlert()
                        SleepReminderWorker.cancel(this@SleepReminderActivity)
                        finish()
                    }
                )
            }
        }
    }

    private fun startAlert(vibrateOnly: Boolean) {
        // Vibration
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))

        // Sound (unless vibrate-only)
        if (!vibrateOnly) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, notificationUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("SleepReminderActivity", "Sound error", e)
            }
        }
    }

    private fun stopAlert() {
        vibrator?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }
}

@Composable
private fun SleepReminderScreen(onDismiss: () -> Unit) {
    var sleepHours by remember { mutableStateOf("--") }
    var sleepMinutes by remember { mutableStateOf("--") }

    val context = LocalContext.current

    // Update countdown every second
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val prefs = context.dataStore.data.first()
                val mainAlarmTimeStr = prefs[stringPreferencesKey("sleep_main_alarm_time")]

                if (mainAlarmTimeStr != null) {
                    try {
                        val mainAlarmTime = LocalDateTime.parse(mainAlarmTimeStr)
                        val now = LocalDateTime.now()
                        val duration = java.time.Duration.between(now, mainAlarmTime)

                        if (duration.isNegative) {
                            sleepHours = "0"
                            sleepMinutes = "0"
                        } else {
                            sleepHours = duration.toHours().toString()
                            sleepMinutes = (duration.toMinutes() % 60).toString()
                        }
                    } catch (e: Exception) {
                        Log.e("SleepReminderScreen", "Parse error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SleepReminderScreen", "DataStore error", e)
            }

            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1C1A20)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Moon icon
                Icon(
                    imageVector = Icons.Outlined.NightsStay,
                    contentDescription = null,
                    tint = Color(0xFFD3BAFF),
                    modifier = Modifier.height(120.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "ZEIT INS BETT",
                    color = Color(0xFFD3BAFF),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (sleepHours != "--") {
                    Text(
                        text = "Du bekommst $sleepHours Std. $sleepMinutes Min. Schlaf",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "wenn du jetzt schläfst",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD3BAFF)
                    )
                ) {
                    Text(
                        text = "Ich gehe jetzt schlafen",
                        color = Color(0xFF3B2577),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
