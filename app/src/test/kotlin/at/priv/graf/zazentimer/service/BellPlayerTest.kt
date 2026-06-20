package at.priv.graf.zazentimer.service

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import at.priv.graf.zazentimer.Constants
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.database.BellEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class BellPlayerTest {
    private lateinit var mockContext: Context
    private lateinit var mockPowerManager: PowerManager
    private lateinit var mockWakeLock: PowerManager.WakeLock
    private lateinit var player: BellPlayerManager

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPowerManager = mockk(relaxed = true)
        mockWakeLock = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager
        every { mockContext.getSystemService(Context.AUDIO_SERVICE) } returns mockk<AudioManager>(relaxed = true)
        every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock
        every { mockWakeLock.isHeld } returns true
        every { mockContext.packageName } returns "at.priv.graf.zazentimer"

        mockkConstructor(Audio::class)
        coEvery { anyConstructed<Audio>().playAbsVolume(any(), any()) } returns Unit
        coEvery { anyConstructed<Audio>().release() } returns Unit

        player = BellPlayerManager(mockContext, CoroutineDispatchers(main = testDispatcher)) { null }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun playBells_acquiresWakeLockEvenWithoutBells() {
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()
        }

        verify { mockPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zazentimer:PlayBells") }
    }

    @Test
    fun playBells_releasesWakeLock() {
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()
        }

        verify { mockWakeLock.release() }
    }

    @Test
    fun playBells_callsOnDoneWhenFinished() {
        var onDoneCalled = false

        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = Runnable { onDoneCalled = true },
            )
            advanceUntilIdle()
        }

        assert(onDoneCalled)
    }

    @Test
    fun playBells_fallsBackToDemoBellWhenGetBellByIdReturnsNull() {
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { anyConstructed<Audio>().playAbsVolume(any(), any()) }
    }

    @Test
    fun playBells_fallsBackToDemoBellWhenGetBellByIdReturnsNull_explicit() =
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()
            coVerify(exactly = 1) { anyConstructed<Audio>().playAbsVolume(any(), any()) }
        }

    @Test
    fun isPlaying_returnsFalseWhenNoBellsHaveBeenPlayed() {
        assert(!player.isPlaying())
    }

    @Test
    fun release_doesNotCrash() =
        runTest {
            player.release()
        }

    @Test
    fun playBells_handlesStoppingCheckReturningTrueImmediately() {
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { true },
                onDone = null,
            )
            advanceUntilIdle()
        }
    }

    @Test
    fun playBells_spawnsConcurrentAudioInstancesWhenPreviousGongsAreStillPlaying() =
        runTest {
            every { anyConstructed<Audio>().isPlaying() } returns true

            val section = Section(name = "Zazen", duration = 600)
            section.bellId = 1
            section.bellcount = 3
            section.bellpause = 0
            val concurrentPlayer =
                BellPlayerManager(
                    mockContext,
                    CoroutineDispatchers(main = testDispatcher),
                ) {
                    BellEntity(id = 1, uri = "fake://bell/1")
                }
            concurrentPlayer.playBells(
                section,
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()

            coVerify(exactly = 3) { anyConstructed<Audio>().playAbsVolume(any(), any()) }
        }
}
