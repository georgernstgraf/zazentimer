package at.priv.graf.zazentimer.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BellPlayer(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers(),
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private val audioObjects = ArrayList<Audio>()

    fun playBells(
        section: Section,
        volume: Int,
        stoppingCheck: () -> Boolean,
        onDone: Runnable? = null,
    ) {
        scope.launch {
            Log.d(TAG, "Playing bells in coroutine")
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock =
                powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "zazentimer:PlayBells",
                )
            wakeLock.acquire(
                (section.bellcount * BELL_WAKE_LOCK_MULTIPLIER * MS_PER_SECOND),
            )
            Log.d(TAG, "WakeLock created for playing bells")
            for (i in 0 until section.bellcount) {
                if (stoppingCheck()) break
                playBell(section, volume)
                if (i < section.bellcount - 1) {
                    delay((section.bellpause * MS_PER_SECOND))
                }
            }
            while (isPlaying()) {
                delay(WAIT_FOR_PLAYBACK_MS)
            }
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            onDone?.run()
        }
    }

    suspend fun release() {
        val it = audioObjects.iterator()
        while (it.hasNext()) {
            it.next().release()
        }
        audioObjects.clear()
    }

    fun isPlaying(): Boolean {
        val it = audioObjects.iterator()
        while (it.hasNext()) {
            if (it.next().isPlaying()) {
                return true
            }
        }
        return false
    }

    private suspend fun playBell(
        section: Section,
        volume: Int,
    ) {
        val bell = BellCollection.getBellForSection(section) ?: return

        val it = audioObjects.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (!next.isPlaying()) {
                Log.d(TAG, "Found free Audio Object")
                next.playAbsVolume(bell, volume)
                return
            }
        }
        Log.d(TAG, "Created new Audio Object for new bell")
        val audio = Audio(context)
        audio.playAbsVolume(bell, volume)
        audioObjects.add(audio)
    }

    companion object {
        private const val TAG = "ZMT_BellPlayer"
        private const val BELL_WAKE_LOCK_MULTIPLIER = 25
        private const val MS_PER_SECOND = 1000L
        private const val WAIT_FOR_PLAYBACK_MS = 100L
    }
}
