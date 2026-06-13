package at.priv.graf.zazentimer.service

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import at.priv.graf.zazentimer.Constants
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Bell
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
    fun `playBells acquires WakeLock even without bells`() {
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
    fun `playBells releases WakeLock`() {
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
    fun `playBells calls onDone when finished`() {
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
    fun `playBells creates no Audio when BellPlayer has no bells`() {
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()
        }

        coVerify(exactly = 0) { anyConstructed<Audio>().playAbsVolume(any(), any()) }
    }

    @Test
    fun `playBells creates no Audio when BellCollection returns null`() =
        runTest {
            player.playBells(
                Section(name = "Zazen", duration = 600),
                volume = Constants.DEFAULT_BELL_VOLUME,
                stoppingCheck = { false },
                onDone = null,
            )
            advanceUntilIdle()
            coVerify(exactly = 0) { anyConstructed<Audio>().playAbsVolume(any(), any()) }
        }

    @Test
    fun `isPlaying returns false when no bells have been played`() {
        assert(!player.isPlaying())
    }

    @Test
    fun `release does not crash`() =
        runTest {
            player.release()
        }

    @Test
    fun `playBells handles stoppingCheck returning true immediately`() {
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
    fun `playBells spawns concurrent Audio instances when previous gongs are still playing`() =
        runTest {
            val mockBell = mockk<Bell>(relaxed = true)
            val fakeUri = mockk<Uri>(relaxed = true)
            every { mockBell.uri } returns fakeUri
            every { fakeUri.toString() } returns "fake://bell/1"
            val field = BellCollection::class.java.getDeclaredField("bells")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val bells = field.get(BellCollection) as ArrayList<Bell>
            bells.clear()
            bells.add(mockBell)
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
            bells.clear()

            coVerify(exactly = 3) { anyConstructed<Audio>().playAbsVolume(any(), any()) }
        }
}
