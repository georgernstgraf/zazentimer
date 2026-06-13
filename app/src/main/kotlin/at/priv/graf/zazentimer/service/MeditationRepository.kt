package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.DbOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface MeditationRepository {
    val clock: ZazenClock
    val meditationState: StateFlow<MeditationUiState>

    fun onMeditationStarted(meditation: Meditation)

    fun onMeditationUpdated()

    fun onMeditationStopped()

    suspend fun readSession(id: Int): Session?

    suspend fun readSections(id: Int): Array<Section>

    suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume>

    suspend fun getBellById(id: Int): BellEntity?
}

@Singleton
class DbMeditationRepository
    @Inject
    constructor(
        private val dbOperations: DbOperations,
        override val clock: ZazenClock,
    ) : MeditationRepository {
        private val _meditationState = MutableStateFlow<MeditationUiState>(MeditationUiState.Idle())
        override val meditationState: StateFlow<MeditationUiState> = _meditationState.asStateFlow()

        private var activeMeditation: Meditation? = null

        override fun onMeditationStarted(meditation: Meditation) {
            activeMeditation = meditation
            updateState()
        }

        override fun onMeditationUpdated() {
            updateState()
        }

        override fun onMeditationStopped() {
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

        override suspend fun readSession(id: Int): Session? = dbOperations.readSession(id)

        override suspend fun readSections(id: Int): Array<Section> = dbOperations.readSections(id)

        @Suppress("MaxLineLength")
        override suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume> = dbOperations.readBellVolumes(sessionId)

        override suspend fun getBellById(id: Int): BellEntity? = dbOperations.getBellById(id)
    }
