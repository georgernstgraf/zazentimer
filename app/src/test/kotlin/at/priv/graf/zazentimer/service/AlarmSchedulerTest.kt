package at.priv.graf.zazentimer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AlarmSchedulerTest {
    private lateinit var mockContext: Context
    private lateinit var mockAlarmManager: AlarmManager
    private lateinit var mockClock: ZazenClock
    private lateinit var mockPendingIntent: PendingIntent
    private lateinit var scheduler: AlarmScheduler
    private val section = at.priv.graf.zazentimer.bo.Section("Zazen", 600)

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAlarmManager = mockk(relaxed = true)
        mockClock = mockk()
        mockPendingIntent = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager
        every { mockClock.now() } returns 10000L

        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
        } returns mockPendingIntent

        scheduler = AlarmScheduler(mockContext, mockClock)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setAlarmForSectionEnd records sectionStartTime`() {
        scheduler.setAlarmForSectionEnd(section, pauseSectionSeconds = 0)

        assertThat(scheduler.sectionStartTime).isEqualTo(10000L)
    }

    @Test
    fun `setAlarmForSectionEnd uses clock for start time`() {
        every { mockClock.now() } returns 50000L

        scheduler.setAlarmForSectionEnd(section, pauseSectionSeconds = 0)

        assertThat(scheduler.sectionStartTime).isEqualTo(50000L)
    }

    @Test
    fun `setAlarmForSectionEnd creates PendingIntent with SECTION_ENDED intent`() {
        scheduler.setAlarmForSectionEnd(section, pauseSectionSeconds = 0)

        verify {
            PendingIntent.getBroadcast(
                mockContext,
                0,
                any(),
                PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    @Test
    fun `setAlarmForSectionEnd triggers setAlarmClock on AlarmManager`() {
        scheduler.setAlarmForSectionEnd(section, pauseSectionSeconds = 0)

        verify { mockAlarmManager.setAlarmClock(any(), any()) }
    }

    @Test
    fun `setAlarmForSectionEnd accounts for pause seconds in trigger time`() {
        every { mockClock.now() } returns 10000L

        scheduler.setAlarmForSectionEnd(section, pauseSectionSeconds = 60)

        verify { mockAlarmManager.setAlarmClock(any(), any()) }
    }

    @Test
    fun `cancelAlarm cancels pending intent on AlarmManager`() {
        scheduler.setAlarmForSectionEnd(section, pauseSectionSeconds = 0)

        scheduler.cancelAlarm()

        verify { mockAlarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `cancelAlarm without prior set does not crash`() {
        scheduler.cancelAlarm()
    }
}
