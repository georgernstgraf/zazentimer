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
    private var activeMuteMode: Int = MUTE_MODE_OFF

    suspend fun mutePhone() {
        Log.d(TAG, "mutePhone")
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
        when {
            vibrateSound -> activeMuteMode = MUTE_MODE_OFF
            vibrate -> {
                activeMuteMode = MUTE_MODE_VIBRATE
                saveAndSetRingerModeVibrate()
            }
            none -> {
                activeMuteMode = MUTE_MODE_SILENT
                saveAndSetDndOrSilent()
            }
            else -> activeMuteMode = MUTE_MODE_OFF
        }
    }

    suspend fun unmutePhone() {
        Log.d(TAG, "unmutePhone: mode=$activeMuteMode")
        when (activeMuteMode) {
            MUTE_MODE_VIBRATE -> restoreRingerMode()
            MUTE_MODE_SILENT -> {
                restoreDndFilterIfUnchanged()
                restoreRingerMode()
            }
            else -> return
        }
        activeMuteMode = MUTE_MODE_OFF
    }

    private fun saveAndSetRingerModeVibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            savedFilter = notificationManager.currentInterruptionFilter
            savedPolicy = notificationManager.notificationPolicy
        }
        oldRingerMode = audioManager.ringerMode
        oldRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        mutedRingerMode = audioManager.ringerMode
        Log.d(TAG, "muteVibrate: saved ringerMode=$oldRingerMode volume=$oldRingerVolume")
    }

    private suspend fun saveAndSetDndOrSilent() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            notificationManager.isNotificationPolicyAccessGranted()
        ) {
            savedFilter = notificationManager.currentInterruptionFilter
            savedPolicy = notificationManager.notificationPolicy
            Log.d(TAG, "muteDND: saved filter=$savedFilter policy=${savedPolicy != null}")
            val priorityPolicy =
                NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                )
            notificationManager.notificationPolicy = priorityPolicy
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            )
            appliedFilter = notificationManager.currentInterruptionFilter
            Log.d(TAG, "muteDND: applied filter=$appliedFilter")
        } else {
            oldRingerMode = audioManager.ringerMode
            oldRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            delay(RINGER_MODE_DELAY_MS)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            mutedRingerMode = audioManager.ringerMode
            Log.d(TAG, "muteSilent: saved ringerMode=$oldRingerMode volume=$oldRingerVolume")
        }
    }

    private fun restoreDndFilterIfUnchanged() {
        if (appliedFilter == INTERRUPTION_FILTER_NOT_SAVED) return
        val currentFilter = notificationManager.currentInterruptionFilter
        Log.d(
            TAG,
            "restoreDND: current=$currentFilter applied=$appliedFilter saved=$savedFilter",
        )
        if (currentFilter == appliedFilter) {
            savedPolicy?.let { notificationManager.setNotificationPolicy(it) }
            notificationManager.setInterruptionFilter(savedFilter)
            Log.d(TAG, "restoreDND: restored filter=$savedFilter")
        } else {
            Log.d(TAG, "restoreDND: filter changed during meditation — skipping restore")
        }
        appliedFilter = INTERRUPTION_FILTER_NOT_SAVED
        savedFilter = INTERRUPTION_FILTER_NOT_SAVED
        savedPolicy = null
    }

    private suspend fun restoreRingerMode() {
        if (mutedRingerMode == RINGER_MODE_NOT_MUTED) return
        val currentMode = audioManager.ringerMode
        if (currentMode != mutedRingerMode) {
            Log.d(
                TAG,
                "restoreRinger: changed during meditation " +
                    "(was $mutedRingerMode, now $currentMode) — skipping restore",
            )
            mutedRingerMode = RINGER_MODE_NOT_MUTED
            return
        }
        Log.d(TAG, "restoreRinger: restoring ringerMode=$oldRingerMode volume=$oldRingerVolume")
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

    companion object {
        private const val TAG = "ZMT_AudioStateManager"
        private const val RINGER_MODE_DELAY_MS = 500L
        private const val RINGER_MODE_NOT_MUTED = -1
        private const val INTERRUPTION_FILTER_NOT_SAVED = -1
        private const val MUTE_MODE_OFF = 0
        private const val MUTE_MODE_VIBRATE = 1
        private const val MUTE_MODE_SILENT = 2
    }
}
