package at.priv.graf.zazentimer.service

import android.util.Log
import at.priv.graf.zazentimer.bo.Section
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
            Log.d(TAG, "start(): Meditation already started!")
            return
        }
        started = true
        audioStateManager.mutePhone()
        alarmScheduler.setAlarmForSectionEnd(sections[currentSectionIdx], pauseSectionSeconds)
        startTicker()
        repository.onMeditationStarted(this)
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

    fun stop() {
        if (!started) {
            Log.d(TAG, "stop(): Meditation not yet started!")
        } else {
            scope.launch {
                finishMeditation()
            }
        }
    }

    fun pause() {
        if (!started || stopping) {
            Log.d(TAG, "pause(): Meditation not yet started or already stopped")
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

    private suspend fun finishMeditation() {
        stopping = true
        stopTicker()
        alarmScheduler.cancelAlarm()
        bellPlayer.release()
        audioStateManager.unmutePhone()
        repository.onMeditationStopped()
        fireMeditationEnded()
    }

    fun release() {
        stopTicker()
        scope.cancel()
    }

    private fun fireMeditationEnded() {
        meditationService.onMeditationEnd()
    }

    fun onSectionEnd() {
        alarmScheduler.cancelAlarm()
        if (currentSectionIdx < sections.size - 1) {
            bellPlayer.playBells(sections[currentSectionIdx], { stopping }, null)
            currentSectionIdx++
            pauseSectionSeconds = 0
            alarmScheduler.setAlarmForSectionEnd(sections[currentSectionIdx], pauseSectionSeconds)
            repository.onMeditationUpdated()
            return
        }
        bellPlayer.playBells(sections[currentSectionIdx], { stopping }) {
            scope.launch {
                finishMeditation()
            }
        }
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

    companion object {
        const val INTENT_SECTION_ENDED: String = "at.priv.graf.zazentimer.ACTION_SECTION_ENDED"
        private const val TAG = "ZMT_Meditation"
        private const val TICKER_INTERVAL_MS = 1000L
        private const val MS_PER_SECOND = 1000L
    }
}
