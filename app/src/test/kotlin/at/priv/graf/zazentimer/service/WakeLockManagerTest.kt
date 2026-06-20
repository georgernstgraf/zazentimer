package at.priv.graf.zazentimer.service

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.database.SectionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class WakeLockManagerTest {
    private lateinit var mockContext: Context
    private lateinit var mockPowerManager: PowerManager
    private lateinit var mockSectionRepository: SectionRepository
    private lateinit var mockPrefs: SharedPreferences

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPowerManager = mockk(relaxed = true)
        mockSectionRepository = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun acquire_whenScreenOnPrefTrueCreatesWakeLock() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                    ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
                )
            } returns true
            coEvery { mockSectionRepository.readSections(1) } returns emptyArray()

            val manager =
                WakeLockManager(
                    mockContext,
                    mockSectionRepository,
                    CoroutineDispatchers(main = testDispatcher),
                )
            manager.acquire(mockPrefs, selectedSessionId = 1)

            verify { mockPowerManager.newWakeLock(any(), any()) }
        }

    @Test
    fun acquire_whenScreenOnPrefFalseDoesNotCreateWakeLock() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                    ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
                )
            } returns false

            val manager =
                WakeLockManager(
                    mockContext,
                    mockSectionRepository,
                    CoroutineDispatchers(main = testDispatcher),
                )
            manager.acquire(mockPrefs, selectedSessionId = 1)

            verify(exactly = 0) { mockPowerManager.newWakeLock(any(), any()) }
        }

    @Test
    fun release_doesNotCrashWhenNoLockAcquired() {
        val manager =
            WakeLockManager(
                mockContext,
                mockSectionRepository,
                CoroutineDispatchers(main = testDispatcher),
            )
        manager.release()
    }

    @Test
    fun release_afterAcquireReleasesTheHeldLock() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                    ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
                )
            } returns true
            coEvery { mockSectionRepository.readSections(1) } returns emptyArray()
            val mockWakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
            every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock
            every { mockWakeLock.isHeld } returns true

            val manager =
                WakeLockManager(
                    mockContext,
                    mockSectionRepository,
                    CoroutineDispatchers(main = testDispatcher),
                )
            manager.acquire(mockPrefs, selectedSessionId = 1)

            manager.release()

            verify { mockWakeLock.release() }
        }

    @Test
    fun acquire_twiceCreatesNewWakeLockEachTime() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                    ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
                )
            } returns true
            coEvery { mockSectionRepository.readSections(1) } returns emptyArray()

            val manager =
                WakeLockManager(
                    mockContext,
                    mockSectionRepository,
                    CoroutineDispatchers(main = testDispatcher),
                )
            manager.acquire(mockPrefs, selectedSessionId = 1)
            manager.acquire(mockPrefs, selectedSessionId = 1)

            verify(exactly = 2) { mockPowerManager.newWakeLock(any(), any()) }
        }

    @Test
    fun acquire_calculatesWakeLockTimeoutFromSectionDurations() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                    ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
                )
            } returns true
            coEvery { mockSectionRepository.readSections(1) } returns
                arrayOf(Section(duration = 600), Section(duration = 300))
            val mockWakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
            every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock

            val manager =
                WakeLockManager(
                    mockContext,
                    mockSectionRepository,
                    CoroutineDispatchers(main = testDispatcher),
                )
            manager.acquire(mockPrefs, selectedSessionId = 1)

            val totalSeconds = 600L + 300L
            val expectedTimeout = (totalSeconds + 60L) * 1000L
            verify { mockWakeLock.acquire(expectedTimeout) }
        }
}
