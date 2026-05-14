package at.priv.graf.zazentimer.service

import android.util.Log
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.SessionBellVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList")
class Meditation(
    private val meditationService: MeditationService,
    private val repository: MeditationRepository,
    private val sessionName: String,
    private val sections: Array<Section>,
    private val bellVolumes: List<SessionBellVolume>,
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers(),
    private val audioStateManager: AudioStateManager,
    private val alarmScheduler: AlarmScheduler,
    private val bellPlayer: BellPlayer,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private val clock = repository.clock
    private var currentSectionIdx: Int = 0
    private var pauseSectionSeconds: Int = 0
    private var totalSessionTime: Int = 0
    private var tickerJob: Job? = null

    @Volatile
    private var paused: Boolean = false

    @Volatile
    private var started: Boolean = false

    @Volatile
    private var stopping: Boolean = false

    init {
        this.totalSessionTime = MeditationTimer.getTotalSessionTime(sections)
    }

    suspend fun start() {
        if (started) {
            Log.d(TAG, "start(): already started")
            return
        }
        started = true
        audioStateManager.mutePhone()
        alarmScheduler.setAlarmForSectionEnd(sections[currentSectionIdx], pauseSectionSeconds)
        startTicker()
        repository.onMeditationStarted(this)
    }

    fun stop() {
        if (!started) {
            Log.d(TAG, "stop(): not started")
            return
        }
        if (stopping) {
            Log.d(TAG, "stop(): already stopping")
            return
        }
        scope.launch {
            stopImmediate()
        }
    }

    fun pause() {
        if (!started || stopping) {
            Log.d(TAG, "pause(): not started or already stopping")
            return
        }
        if (!paused) {
            pauseSectionSeconds = getSectionElapsedSeconds()
            paused = true
            stopTicker()
            alarmScheduler.cancelAlarm()
            repository.onMeditationUpdated()
        } else {
            paused = false
            alarmScheduler.setAlarmForSectionEnd(sections[currentSectionIdx], pauseSectionSeconds)
            startTicker()
            repository.onMeditationUpdated()
        }
    }

    fun onSectionEnd() {
        alarmScheduler.cancelAlarm()
        if (currentSectionIdx < sections.size - 1) {
            bellPlayer.playBells(
                sections[currentSectionIdx],
                getVolumeForSection(sections[currentSectionIdx]),
                { stopping },
                null,
            )
            currentSectionIdx++
            pauseSectionSeconds = 0
            alarmScheduler.setAlarmForSectionEnd(sections[currentSectionIdx], pauseSectionSeconds)
            repository.onMeditationUpdated()
            return
        }
        val volume = getVolumeForSection(sections[currentSectionIdx])
        bellPlayer.playBells(sections[currentSectionIdx], volume, { stopping }) {
            scope.launch {
                finishAfterLastBell()
            }
        }
    }

    fun release() {
        stopTicker()
        scope.cancel()
    }

    fun getCurrentSection(): Section = sections[currentSectionIdx]

    fun getCurrentSectionName(): String = if (currentSectionIdx >= 0) sections[currentSectionIdx].name ?: "" else ""

    fun getTotalSessionTime(): Int = totalSessionTime

    fun getCurrentSessionTime(): Int =
        MeditationTimer.getCurrentSessionTime(
            getCurrentStartSeconds(),
            getSectionElapsedSeconds(),
        )

    fun isPaused(): Boolean = paused

    fun isStopped(): Boolean = stopping

    fun getCurrentStartSeconds(): Int = MeditationTimer.getCurrentStartSeconds(sections, currentSectionIdx)

    fun getNextEndSeconds(): Int = MeditationTimer.getNextEndSeconds(sections, currentSectionIdx)

    fun getNextStartSeconds(): Int = MeditationTimer.getNextStartSeconds(sections, currentSectionIdx)

    fun getPrevStartSeconds(): Int = MeditationTimer.getPrevStartSeconds(sections, currentSectionIdx)

    fun getNextSectionName(): String =
        if (currentSectionIdx < sections.size - 1) {
            sections[currentSectionIdx + 1].name ?: ""
        } else {
            ""
        }

    fun getNextNextSectionName(): String =
        if (currentSectionIdx < sections.size - 2) {
            sections[currentSectionIdx + 2].name ?: ""
        } else {
            ""
        }

    fun getSectionElapsedSeconds(): Int {
        val raw: Int =
            if (paused) {
                pauseSectionSeconds
            } else {
                Math.round(
                    (
                        (clock.now() / MS_PER_SECOND) -
                            (alarmScheduler.sectionStartTime / MS_PER_SECOND)
                    ).toFloat(),
                ) + pauseSectionSeconds
            }
        return MeditationTimer.getSectionElapsedSeconds(raw, getCurrentSection().duration)
    }

    fun getSessionName(): String = sessionName

    private suspend fun stopImmediate() {
        Log.d(TAG, "stopImmediate: cancelling bells and cleaning up")
        stopping = true
        cleanup()
    }

    private suspend fun finishAfterLastBell() {
        if (stopping) {
            Log.d(TAG, "finishAfterLastBell: already stopping — skipping")
            return
        }
        Log.d(TAG, "finishAfterLastBell: bells finished, cleaning up")
        stopping = true
        cleanup()
    }

    private suspend fun cleanup() {
        stopTicker()
        alarmScheduler.cancelAlarm()
        MeditationService.setRunning(false)
        repository.onMeditationStopped()
        bellPlayer.release()
        audioStateManager.unmutePhone()
        meditationService.onMeditationEnd()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob =
            scope.launch {
                while (isActive) {
                    repository.onMeditationUpdated()
                    delay(TICKER_INTERVAL_MS)
                }
            }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun getVolumeForSection(section: Section): Int {
        val match =
            bellVolumes.find { bv ->
                (bv.bell != null && bv.bell == section.bell) ||
                    (bv.bellUri != null && bv.bellUri == section.bellUri)
            }
        return match?.volume ?: DEFAULT_BELL_VOLUME
    }

    companion object {
        const val INTENT_SECTION_ENDED: String = "at.priv.graf.zazentimer.ACTION_SECTION_ENDED"
        private const val TAG = "ZMT_Meditation"
        private const val TICKER_INTERVAL_MS = 1000L
        private const val MS_PER_SECOND = 1000L
        private const val DEFAULT_BELL_VOLUME = 100
    }
}
