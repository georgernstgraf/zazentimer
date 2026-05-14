package at.priv.graf.zazentimer.service

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.util.Log
import at.priv.graf.zazentimer.ZazenTimerActivity
import kotlinx.coroutines.delay

class AudioStateManager(
    private val context: Context,
    private val prefs: SharedPreferences,
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var oldRingerMode: Int = AudioManager.RINGER_MODE_SILENT
    private var oldRingerVolume: Int = 0
    private var mutedRingerMode: Int = RINGER_MODE_NOT_MUTED
    private var savedFilter: Int = INTERRUPTION_FILTER_NOT_SAVED
    private var savedPolicy: NotificationManager.Policy? = null
    private var appliedFilter: Int = INTERRUPTION_FILTER_NOT_SAVED

    suspend fun mutePhone() {
        Log.d(TAG, "muting Phone")
        val vibrateSound =
            prefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                false,
            )
        val vibrate =
            prefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE,
                false,
            )
        val none =
            prefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE,
                true,
            )
        if (!vibrateSound) {
            if (vibrate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    savedFilter = notificationManager.currentInterruptionFilter
                    savedPolicy = notificationManager.notificationPolicy
                }
                oldRingerMode = audioManager.ringerMode
                oldRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                mutedRingerMode = audioManager.ringerMode
            } else if (none) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    notificationManager.isNotificationPolicyAccessGranted()
                ) {
                    savedFilter = notificationManager.currentInterruptionFilter
                    savedPolicy = notificationManager.notificationPolicy
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_NONE,
                    )
                    appliedFilter = notificationManager.currentInterruptionFilter
                } else {
                    oldRingerMode = audioManager.ringerMode
                    oldRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    delay(RINGER_MODE_DELAY_MS)
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    mutedRingerMode = audioManager.ringerMode
                }
            }
        }
    }

    suspend fun unmutePhone() {
        Log.d(TAG, "unmuting Phone")
        val vibrateSound =
            prefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                false,
            )
        val vibrate =
            prefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE,
                false,
            )
        val none =
            prefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE,
                true,
            )
        if (vibrateSound || (!vibrate && !none)) return
        restoreDndFilterIfUnchanged()
        if (!restoreRingerIfUnchanged()) return
        Log.d(TAG, "unmuting: ring=$oldRingerVolume ringerMode=$oldRingerMode")
        audioManager.ringerMode = oldRingerMode
        delay(RINGER_MODE_DELAY_MS)
        if (oldRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                oldRingerVolume,
                0,
            )
        }
        audioManager.ringerMode = oldRingerMode
        mutedRingerMode = RINGER_MODE_NOT_MUTED
    }

    private fun restoreDndFilterIfUnchanged() {
        if (appliedFilter == INTERRUPTION_FILTER_NOT_SAVED) return
        if (notificationManager.currentInterruptionFilter == appliedFilter) {
            notificationManager.setInterruptionFilter(savedFilter)
            savedPolicy?.let { notificationManager.setNotificationPolicy(it) }
        } else {
            Log.d(TAG, "DND filter changed during meditation — skipping restore")
        }
        appliedFilter = INTERRUPTION_FILTER_NOT_SAVED
        savedFilter = INTERRUPTION_FILTER_NOT_SAVED
        savedPolicy = null
    }

    @Suppress("ReturnCount")
    private fun restoreRingerIfUnchanged(): Boolean {
        if (mutedRingerMode == RINGER_MODE_NOT_MUTED) return true
        val currentMode = audioManager.ringerMode
        if (currentMode != mutedRingerMode) {
            Log.d(
                TAG,
                "Ringer mode changed during meditation " +
                    "(was $mutedRingerMode, now $currentMode) — skipping restore",
            )
            mutedRingerMode = RINGER_MODE_NOT_MUTED
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "ZMT_AudioStateManager"
        private const val RINGER_MODE_DELAY_MS = 500L
        private const val RINGER_MODE_NOT_MUTED = -1
        private const val INTERRUPTION_FILTER_NOT_SAVED = -1
    }
}
