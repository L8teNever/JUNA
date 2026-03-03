package bea.l8tenever.com.device

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import bea.l8tenever.com.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * Verwaltet den Gerätezustand während Schulstunden.
 * Speichert aktuelle Einstellungen und stellt sie nach der Schule wieder her.
 */
object DeviceStateManager {

    private const val TAG = "DeviceStateManager"
    private const val PREFS_NAME = "device_state_backup"
    private const val KEY_SCHOOL_MODE_ACTIVE = "school_mode_active"
    private const val KEY_DND_MODE = "saved_dnd_mode"
    private const val KEY_RINGER_MODE = "saved_ringer_mode"
    private const val KEY_RING_VOLUME = "saved_ring_volume"
    private const val KEY_NOTIFICATION_VOLUME = "saved_notification_volume"
    private const val KEY_MUSIC_VOLUME = "saved_music_volume"

    /**
     * Speichert den aktuellen Gerätezustand (DND, Ringer-Modus, Lautstärken)
     */
    suspend fun saveCurrentState(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Aktuellen DND-Status speichern (nur API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                prefs.edit().putInt(
                    KEY_DND_MODE,
                    notificationManager.currentInterruptionFilter
                ).apply()
            }

            // Ringer-Modus speichern
            prefs.edit().putInt(
                KEY_RINGER_MODE,
                audioManager.ringerMode
            ).apply()

            // Alle Lautstärken speichern
            prefs.edit().putInt(
                KEY_RING_VOLUME,
                audioManager.getStreamVolume(AudioManager.STREAM_RING)
            ).apply()

            prefs.edit().putInt(
                KEY_NOTIFICATION_VOLUME,
                audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            ).apply()

            prefs.edit().putInt(
                KEY_MUSIC_VOLUME,
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            ).apply()

            Log.d(TAG, "Gerätezustand gespeichert")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Speichern des Gerätezustands", e)
        }
    }

    /**
     * Aktiviert den Schulmodus basierend auf den Nutzer-Einstellungen
     */
    suspend fun activateSchoolMode(context: Context, userPrefs: UserPreferences) = withContext(Dispatchers.IO) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val dndEnabled = try {
                userPrefs.autoDndEnabled.first()
            } catch (e: Exception) {
                false
            }

            val volumeEnabled = try {
                userPrefs.autoVolumeEnabled.first()
            } catch (e: Exception) {
                false
            }

            val volumeRing = try {
                userPrefs.autoVolumeRing.first()
            } catch (e: Exception) {
                true
            }

            val volumeNotification = try {
                userPrefs.autoVolumeNotification.first()
            } catch (e: Exception) {
                true
            }

            val volumeMedia = try {
                userPrefs.autoVolumeMedia.first()
            } catch (e: Exception) {
                false
            }

            // DND aktivieren wenn gewünscht (nur API 23+)
            if (dndEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        Log.d(TAG, "DND aktiviert")
                    } else {
                        Log.w(TAG, "DND Permission nicht erteilt")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Aktivieren von DND", e)
                }
            }

            // Lautstärken auf 0 setzen wenn gewünscht
            if (volumeEnabled) {
                try {
                    if (volumeRing) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                        Log.d(TAG, "Ring-Lautstärke auf 0")
                    }
                    if (volumeNotification) {
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                        Log.d(TAG, "Notification-Lautstärke auf 0")
                    }
                    if (volumeMedia) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                        Log.d(TAG, "Media-Lautstärke auf 0")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Setzen der Lautstärke", e)
                }
            }

            // Markiere dass wir im Schulmodus sind
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SCHOOL_MODE_ACTIVE, true).apply()

            Log.d(TAG, "Schulmodus aktiviert")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Aktivieren des Schulmodus", e)
        }
    }

    /**
     * Stellt den vorherigen Gerätezustand wieder her
     */
    suspend fun restoreState(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // DND wiederherstellen (nur API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val savedDndMode = prefs.getInt(KEY_DND_MODE, NotificationManager.INTERRUPTION_FILTER_ALL)
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        notificationManager.setInterruptionFilter(savedDndMode)
                        Log.d(TAG, "DND wiederhergestellt: $savedDndMode")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Wiederherstellen von DND", e)
                }
            }

            // Lautstärken wiederherstellen
            try {
                val ringVolume = prefs.getInt(KEY_RING_VOLUME, -1)
                val notificationVolume = prefs.getInt(KEY_NOTIFICATION_VOLUME, -1)
                val musicVolume = prefs.getInt(KEY_MUSIC_VOLUME, -1)

                if (ringVolume >= 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, ringVolume, 0)
                }
                if (notificationVolume >= 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificationVolume, 0)
                }
                if (musicVolume >= 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolume, 0)
                }
                Log.d(TAG, "Lautstärken wiederhergestellt")
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Wiederherstellen der Lautstärke", e)
            }

            // Ringer-Modus wiederherstellen (nur als Fallback, nicht standardmäßig)
            // da AudioManager.setRingerMode() API 23+ erfordert und spezielle Permission braucht

            // Markiere dass wir NICHT mehr im Schulmodus sind
            prefs.edit().putBoolean(KEY_SCHOOL_MODE_ACTIVE, false).apply()

            Log.d(TAG, "Gerätezustand wiederhergestellt")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Wiederherstellen des Gerätezustands", e)
        }
    }

    /**
     * Prüft ob aktuell der Schulmodus aktiv ist
     */
    fun isInSchoolMode(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_SCHOOL_MODE_ACTIVE, false)
        } catch (e: Exception) {
            false
        }
    }
}
