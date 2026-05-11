package at.priv.graf.zazentimer.service

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.database.DbOperations
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
    private lateinit var mockDbOperations: DbOperations
    private lateinit var mockPrefs: SharedPreferences

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPowerManager = mockk(relaxed = true)
        mockDbOperations = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `acquire when screen on pref true creates wake lock`() = runTest {
        every {
            mockPrefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
            )
        } returns true
        coEvery { mockDbOperations.readSections(1) } returns emptyArray()

        val manager = WakeLockManager(
            mockContext,
            mockDbOperations,
            CoroutineDispatchers(main = testDispatcher),
        )
        manager.acquire(mockPrefs, selectedSessionId = 1)

        verify { mockPowerManager.newWakeLock(any(), any()) }
    }

    @Test
    fun `acquire when screen on pref false does not create wake lock`() = runTest {
        every {
            mockPrefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
            )
        } returns false

        val manager = WakeLockManager(
            mockContext,
            mockDbOperations,
            CoroutineDispatchers(main = testDispatcher),
        )
        manager.acquire(mockPrefs, selectedSessionId = 1)

        verify(exactly = 0) { mockPowerManager.newWakeLock(any(), any()) }
    }

    @Test
    fun `release does not crash when no lock acquired`() {
        val manager = WakeLockManager(
            mockContext,
            mockDbOperations,
            CoroutineDispatchers(main = testDispatcher),
        )
        manager.release()
    }

    @Test
    fun `release after acquire releases the held lock`() = runTest {
        every {
            mockPrefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
            )
        } returns true
        coEvery { mockDbOperations.readSections(1) } returns emptyArray()
        val mockWakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
        every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock
        every { mockWakeLock.isHeld } returns true

        val manager = WakeLockManager(
            mockContext,
            mockDbOperations,
            CoroutineDispatchers(main = testDispatcher),
        )
        manager.acquire(mockPrefs, selectedSessionId = 1)

        manager.release()

        verify { mockWakeLock.release() }
    }

    @Test
    fun `acquire twice creates new wake lock each time`() = runTest {
        every {
            mockPrefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
            )
        } returns true
        coEvery { mockDbOperations.readSections(1) } returns emptyArray()

        val manager = WakeLockManager(
            mockContext,
            mockDbOperations,
            CoroutineDispatchers(main = testDispatcher),
        )
        manager.acquire(mockPrefs, selectedSessionId = 1)
        manager.acquire(mockPrefs, selectedSessionId = 1)

        verify(exactly = 2) { mockPowerManager.newWakeLock(any(), any()) }
    }

    @Test
    fun `acquire calculates wake lock timeout from section durations`() = runTest {
        every {
            mockPrefs.getBoolean(
                ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON,
                ZazenTimerActivity.PREF_DEFAULT_KEEP_SCREEN_ON,
            )
        } returns true
        coEvery { mockDbOperations.readSections(1) } returns
            arrayOf(Section(duration = 600), Section(duration = 300))
        val mockWakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
        every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock

        val manager = WakeLockManager(
            mockContext,
            mockDbOperations,
            CoroutineDispatchers(main = testDispatcher),
        )
        manager.acquire(mockPrefs, selectedSessionId = 1)

        val totalSeconds = 600L + 300L
        val expectedTimeout = (totalSeconds + 60L) * 1000L
        verify { mockWakeLock.acquire(expectedTimeout) }
    }
}
