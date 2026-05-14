package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
import at.priv.graf.zazentimer.database.DbOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeditationRepository
    @Inject
    constructor(
        private val dbOperations: DbOperations,
        val clock: ZazenClock,
    ) {
        private val _meditationState = MutableStateFlow<MeditationUiState>(MeditationUiState.Idle())
        val meditationState: StateFlow<MeditationUiState> = _meditationState.asStateFlow()

        private var activeMeditation: Meditation? = null

        fun onMeditationStarted(meditation: Meditation) {
            activeMeditation = meditation
            updateState()
        }

        fun onMeditationUpdated() {
            updateState()
        }

        fun onMeditationStopped() {
            activeMeditation = null
            _meditationState.value = MeditationUiState.Idle()
        }

        private fun updateState() {
            val meditation = activeMeditation ?: return
            val isPaused = meditation.isPaused()

            val state =
                if (isPaused) {
                    MeditationUiState.Paused(
                        currentStartSeconds = meditation.getCurrentStartSeconds(),
                        totalSessionTime = meditation.getTotalSessionTime(),
                        nextEndSeconds = meditation.getNextEndSeconds(),
                        nextStartSeconds = meditation.getNextStartSeconds(),
                        prevStartSeconds = meditation.getPrevStartSeconds(),
                        sectionElapsedSeconds = meditation.getSectionElapsedSeconds(),
                        sessionElapsedSeconds = meditation.getCurrentSessionTime(),
                        currentSectionName = meditation.getCurrentSectionName(),
                        nextSectionName = meditation.getNextSectionName(),
                        sessionName = meditation.getSessionName(),
                        nextNextSectionName = meditation.getNextNextSectionName(),
                    )
                } else {
                    MeditationUiState.Running(
                        currentStartSeconds = meditation.getCurrentStartSeconds(),
                        totalSessionTime = meditation.getTotalSessionTime(),
                        nextEndSeconds = meditation.getNextEndSeconds(),
                        nextStartSeconds = meditation.getNextStartSeconds(),
                        prevStartSeconds = meditation.getPrevStartSeconds(),
                        sectionElapsedSeconds = meditation.getSectionElapsedSeconds(),
                        sessionElapsedSeconds = meditation.getCurrentSessionTime(),
                        currentSectionName = meditation.getCurrentSectionName(),
                        nextSectionName = meditation.getNextSectionName(),
                        sessionName = meditation.getSessionName(),
                        nextNextSectionName = meditation.getNextNextSectionName(),
                    )
                }
            _meditationState.value = state
        }

        suspend fun readSession(id: Int): Session? = dbOperations.readSession(id)

        suspend fun readSections(id: Int): Array<Section> = dbOperations.readSections(id)

        suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume> = dbOperations.readBellVolumes(sessionId)
    }
