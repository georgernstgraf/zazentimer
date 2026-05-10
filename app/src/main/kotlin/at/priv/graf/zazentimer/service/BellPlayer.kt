package at.priv.graf.zazentimer.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BellPlayer(
    private val meditationService: MeditationService,
    private val scope: CoroutineScope,
) {
    private val audioObjects = ArrayList<Audio>()

    fun playBells(
        section: Section,
        stoppingCheck: () -> Boolean,
        onDone: Runnable? = null,
    ) {
        scope.launch {
            Log.d(TAG, "Playing bells in coroutine")
            val powerManager =
                meditationService.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock =
                powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PlayBells",
                )
            wakeLock.acquire(
                (section.bellcount * BELL_WAKE_LOCK_MULTIPLIER * MS_PER_SECOND),
            )
            Log.d(TAG, "WakeLock created for playing bells")
            for (i in 0 until section.bellcount) {
                if (stoppingCheck()) break
                val bell = BellCollection.getBellForSection(section)
                if (bell != null) {
                    val audio = Audio(meditationService)
                    audio.playAbsVolume(bell, section.volume)
                    delay(BELL_OVERLAP_MS)
                    audio.release()
                }
                if (i < section.bellcount - 1) {
                    delay((section.bellpause * MS_PER_SECOND))
                }
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

    companion object {
        private const val TAG = "ZMT_BellPlayer"
        private const val BELL_WAKE_LOCK_MULTIPLIER = 25
        private const val BELL_OVERLAP_MS = 500L
        private const val MS_PER_SECOND = 1000L
    }
}
