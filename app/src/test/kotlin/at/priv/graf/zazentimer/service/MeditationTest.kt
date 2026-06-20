package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.Constants
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
import at.priv.graf.zazentimer.database.BellEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeditationTest {
    private class FakeMeditationRepository(
        override val clock: ZazenClock,
    ) : MeditationRepository {
        var startedCount = 0
        var updatedCount = 0
        var stoppedCount = 0
        val startedMeditations = mutableListOf<Meditation>()
        private val _meditationState = MutableStateFlow<MeditationUiState>(MeditationUiState.Idle())
        override val meditationState: StateFlow<MeditationUiState> = _meditationState

        override fun onMeditationStarted(meditation: Meditation) {
            startedCount++
            startedMeditations.add(meditation)
        }

        override fun onMeditationUpdated() {
            updatedCount++
        }

        override fun onMeditationStopped() {
            stoppedCount++
        }

        override suspend fun readSession(id: Int): Session? = null

        override suspend fun readSections(id: Int): Array<Section> = emptyArray()

        override suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume> = emptyList()

        override suspend fun getBellById(id: Int): BellEntity? = null
    }

    private class FakeAlarmScheduler : AlarmScheduler {
        override var sectionStartTime: Long = 0L
        var cancelAlarmCount = 0
        val alarmCalls = mutableListOf<AlarmCall>()

        data class AlarmCall(
            val section: Section,
            val pauseSectionSeconds: Int,
        )

        override fun setAlarmForSectionEnd(
            section: Section,
            pauseSectionSeconds: Int,
        ) {
            alarmCalls.add(AlarmCall(section, pauseSectionSeconds))
        }

        override fun cancelAlarm() {
            cancelAlarmCount++
        }
    }

    private class FakeClock : ZazenClock {
        var now: Long = 0L

        override fun now(): Long = now

        override fun elapsedRealtime(): Long = now
    }

    private lateinit var mockRepository: FakeMeditationRepository
    private lateinit var mockAlarmScheduler: FakeAlarmScheduler
    private lateinit var mockClock: FakeClock
    private lateinit var fakeBellPlayer: FakeBellPlayer
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var meditation: Meditation
    private var serviceEnded = false

    private val zazen = Section(name = "Zazen", duration = 600)
    private val kinhin = Section(name = "Kinhin", duration = 300)
    private val sections = arrayOf(zazen, kinhin)

    @Before
    fun setUp() {
        mockClock = FakeClock()
        mockRepository = FakeMeditationRepository(mockClock)
        mockAlarmScheduler = FakeAlarmScheduler()
        fakeBellPlayer = FakeBellPlayer()
        testDispatcher = StandardTestDispatcher()
        serviceEnded = false

        zazen.bellId = 0

        meditation = createMeditation()
    }

    private fun createMeditation(
        sections: Array<Section> = this.sections,
        bellVolumes: List<SessionBellVolume> = emptyList(),
        sessionName: String = "Test Session",
    ) = Meditation(
        repository = mockRepository,
        sessionName = sessionName,
        sections = sections,
        bellVolumes = bellVolumes,
        dispatchers = CoroutineDispatchers(main = testDispatcher),
        alarmScheduler = mockAlarmScheduler,
        bellPlayer = fakeBellPlayer,
        onMeditationEnd = { serviceEnded = true },
    )

    @Test
    fun initialState_hasCorrectTotalSessionTime() {
        assertEquals(900, meditation.getTotalSessionTime())
        assertEquals(0, meditation.getCurrentSessionTime())
        assertFalse(meditation.isPaused())
        assertFalse(meditation.isStopped())
    }

    @Test
    fun start_setsAlarmForFirstSectionAndNotifiesRepository() =
        runTest(testDispatcher) {
            meditation.start()

            assertEquals(1, mockRepository.startedCount)
            assertEquals(meditation, mockRepository.startedMeditations[0])
            assertEquals(1, mockAlarmScheduler.alarmCalls.size)
            assertEquals(zazen, mockAlarmScheduler.alarmCalls[0].section)
            assertEquals(0, mockAlarmScheduler.alarmCalls[0].pauseSectionSeconds)
            meditation.release()
        }

    @Test
    fun start_isIdempotent() =
        runTest(testDispatcher) {
            meditation.start()
            meditation.start()

            assertEquals(1, mockRepository.startedCount)
            assertEquals(1, mockAlarmScheduler.alarmCalls.size)
            meditation.release()
        }

    @Test
    fun ticker_firesRepositoryUpdateEverySecondAfterStart() =
        runTest(testDispatcher) {
            meditation.start()
            advanceTimeBy(1999)

            assertEquals(2, mockRepository.updatedCount)
            meditation.release()
        }

    @Test
    fun pause_cancelsAlarmAndSetsPausedFlag() =
        runTest(testDispatcher) {
            meditation.start()
            meditation.pause()

            assertEquals(1, mockAlarmScheduler.cancelAlarmCount)
            assertTrue(meditation.isPaused())
            meditation.release()
        }

    @Test
    fun pausing_stopsTheTicker() =
        runTest(testDispatcher) {
            meditation.start()
            advanceTimeBy(2000)
            val ticksBeforePause = mockRepository.updatedCount
            meditation.pause()

            advanceTimeBy(5000)
            assertEquals(ticksBeforePause + 1, mockRepository.updatedCount)
            meditation.release()
        }

    @Test
    fun resume_restartsTickerAndReArmsAlarmWithCorrectOffset() =
        runTest(testDispatcher) {
            meditation.start()
            mockClock.now = 2000L
            meditation.pause()
            runCurrent()

            val previousAlarmCount = mockAlarmScheduler.alarmCalls.size
            meditation.pause()

            assertFalse(meditation.isPaused())
            assertEquals(previousAlarmCount + 1, mockAlarmScheduler.alarmCalls.size)
            val lastCall = mockAlarmScheduler.alarmCalls.last()
            assertEquals(zazen, lastCall.section)
            assertEquals(2, lastCall.pauseSectionSeconds)
            meditation.release()
        }

    @Test
    fun resume_restartsTickerAfterPause() =
        runTest(testDispatcher) {
            meditation.start()
            advanceTimeBy(2000)
            val ticksBeforePause = mockRepository.updatedCount
            meditation.pause()
            runCurrent()
            meditation.pause()

            advanceTimeBy(2000)
            // At 2000ms ticksBeforePause was X.
            // pause() makes it X + 1.
            // resume() body makes it X + 2.
            // new tickerJob starts immediately at time 2000, calling onMeditationUpdated -> X + 3.
            // advanceTimeBy(2000) runs virtual time from 2000 to 4000.
            // ticker delays 1000ms: at time 3000, calls onMeditationUpdated -> X + 4.
            // So total expected is ticksBeforePause + 4!
            assertEquals(ticksBeforePause + 4, mockRepository.updatedCount)
            meditation.release()
        }

    @Test
    fun intermediateSectionEnd_advancesToNextSectionAndPlaysBells() {
        meditation.onSectionEnd()

        assertEquals(1, mockAlarmScheduler.cancelAlarmCount)
        assertEquals(kinhin, meditation.getCurrentSection())
        assertEquals(1, mockAlarmScheduler.alarmCalls.size)
        assertEquals(kinhin, mockAlarmScheduler.alarmCalls[0].section)
        assertEquals(0, mockAlarmScheduler.alarmCalls[0].pauseSectionSeconds)

        assertEquals(1, fakeBellPlayer.playedBells.size)
        val call = fakeBellPlayer.playedBells[0]
        assertEquals(zazen, call.section)
        assertEquals(Constants.DEFAULT_BELL_VOLUME, call.volume)
    }

    @Test
    fun finalSectionEnd_triggersCleanupAfterBellCallback() =
        runTest(testDispatcher) {
            val singleSection = arrayOf(zazen)
            meditation = createMeditation(sections = singleSection)

            meditation.onSectionEnd()

            assertEquals(1, fakeBellPlayer.playedBells.size)

            advanceUntilIdle()

            assertEquals(2, mockAlarmScheduler.cancelAlarmCount)
            assertEquals(1, mockRepository.stoppedCount)
            assertTrue(fakeBellPlayer.released)
            assertTrue(serviceEnded)
        }

    @Test
    fun bellVolumes_areCorrectlyResolvedFromConfiguration() {
        zazen.bellId = 1
        val bellVolumes =
            listOf(
                SessionBellVolume(bellId = 1, volume = 80),
            )
        meditation = createMeditation(bellVolumes = bellVolumes)

        meditation.onSectionEnd()

        assertEquals(80, fakeBellPlayer.playedBells[0].volume)
    }

    @Test
    fun bellVolume_fallsBackToDefaultWhenNoConfigExists() {
        zazen.bellId = 42

        meditation.onSectionEnd()

        assertEquals(Constants.DEFAULT_BELL_VOLUME, fakeBellPlayer.playedBells[0].volume)
    }

    @Test
    fun stop_triggersCleanupAndReleasesPlayer() =
        runTest(testDispatcher) {
            meditation.start()
            advanceTimeBy(1000)
            meditation.stop()
            advanceUntilIdle()

            assertEquals(1, mockRepository.stoppedCount)
            assertTrue(fakeBellPlayer.released)
            assertTrue(serviceEnded)
            assertTrue(meditation.isStopped())
            meditation.release()
        }

    @Test
    fun stop_isIdempotent_secondCallIsIgnored() =
        runTest(testDispatcher) {
            meditation.start()
            advanceTimeBy(1000)
            meditation.stop()
            advanceUntilIdle()

            fakeBellPlayer.released = false
            serviceEnded = false

            meditation.stop()
            advanceUntilIdle()

            assertEquals(1, mockRepository.stoppedCount)
            assertFalse(fakeBellPlayer.released)
            assertFalse(serviceEnded)
            meditation.release()
        }

    @Test
    fun stopping_preventsFurtherActions() =
        runTest(testDispatcher) {
            meditation.start()
            meditation.stop()
            advanceUntilIdle()

            val cancelCountBefore = mockAlarmScheduler.cancelAlarmCount
            meditation.onSectionEnd()

            assertEquals(cancelCountBefore + 1, mockAlarmScheduler.cancelAlarmCount)
            meditation.release()
        }

    @Test
    fun finishAfterLastBell_doesNothingIfAlreadyStopping() =
        runTest(testDispatcher) {
            val singleSection = arrayOf(zazen)
            meditation = createMeditation(sections = singleSection)

            meditation.start()
            advanceTimeBy(1000)
            meditation.stop()
            advanceUntilIdle()

            serviceEnded = false

            meditation.onSectionEnd()
            advanceUntilIdle()

            assertEquals(1, mockRepository.stoppedCount)
            assertFalse(serviceEnded)
            meditation.release()
        }

    @Test
    fun getCurrentSectionName_returnsEmptyForUnnamedSection() {
        val unnamed = Section(name = null, duration = 100)
        meditation = createMeditation(sections = arrayOf(unnamed))

        assertEquals("", meditation.getCurrentSectionName())
    }

    @Test
    fun getNextSectionName_returnsCorrectName() {
        assertEquals("Kinhin", meditation.getNextSectionName())
    }

    @Test
    fun getNextSectionName_returnsEmptyOnLastSection() =
        runTest(testDispatcher) {
            meditation.onSectionEnd()
            assertEquals("", meditation.getNextSectionName())
        }

    @Test
    fun getNextNextSectionName_returnsEmptyWhenOnlyTwoSections() {
        assertEquals("", meditation.getNextNextSectionName())
    }
}
