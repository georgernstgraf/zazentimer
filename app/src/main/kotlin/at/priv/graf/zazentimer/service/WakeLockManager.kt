package at.priv.graf.zazentimer.service

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.database.DbOperations
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeLockManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dbOperations: DbOperations,
        private val dispatchers: CoroutineDispatchers = CoroutineDispatchers(),
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
        private var wakeLock: PowerManager.WakeLock? = null

        fun acquire(
            pref: SharedPreferences,
            selectedSessionId: Int,
        ) {
            val keepScreenOn =
                pref.getBoolean(
                    ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                    ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
                )
            if (!keepScreenOn) {
                return
            }
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            scope.launch {
                val totalSeconds = dbOperations.readSections(selectedSessionId).sumOf { it.duration }
                wakeLock = null
                wakeLock =
                    powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        WAKELOCK_TAG,
                    )
                val timeoutSeconds = totalSeconds + WAKELOCK_TIMEOUT_BUFFER_SECONDS
                wakeLock?.acquire(timeoutSeconds * MILLIS_PER_SECOND)
                Log.i(TAG, "Acquired WakeLock to keep screen on for $timeoutSeconds seconds")
            }
        }

        fun release() {
            wakeLock?.let { lock ->
                try {
                    if (lock.isHeld) {
                        lock.release()
                    }
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: RuntimeException,
                ) {
                    Log.d(TAG, "wakeLock release error", e)
                }
                wakeLock = null
                Log.i(TAG, "ScreenOn-WakeLock released")
            }
        }

        companion object {
            private const val TAG = "ZMT_WakeLockManager"
            private const val WAKELOCK_TAG = "ScreenOnWakeLock"
            private const val WAKELOCK_TIMEOUT_BUFFER_SECONDS = 60
            private const val MILLIS_PER_SECOND = 1000L
        }
    }
