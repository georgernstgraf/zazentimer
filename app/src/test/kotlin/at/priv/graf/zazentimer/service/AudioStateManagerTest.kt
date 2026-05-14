package at.priv.graf.zazentimer.service

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import at.priv.graf.zazentimer.ZazenTimerActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AudioStateManagerTest {
    private lateinit var mockContext: Context
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var manager: AudioStateManager
    private val initialRingerMode = AudioManager.RINGER_MODE_NORMAL
    private val initialVolume = 7

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAudioManager = mockk(relaxed = true)
        mockNotificationManager = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.AUDIO_SERVICE) } returns mockAudioManager
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockAudioManager.ringerMode } returns initialRingerMode
        every { mockAudioManager.getStreamVolume(AudioManager.STREAM_RING) } returns initialVolume

        manager = AudioStateManager(mockContext, mockPrefs)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `mutePhone with vibrate pref sets ringer to VIBRATE`() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                    false,
                )
            } returns false
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE,
                    false,
                )
            } returns true
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE,
                    true,
                )
            } returns false

            manager.mutePhone()

            manager.unmutePhone()

            verify { mockAudioManager.setStreamVolume(AudioManager.STREAM_RING, initialVolume, 0) }
        }

    @Test
    fun `unmutePhone without prior mute does nothing`() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                    false,
                )
            } returns false

            manager.unmutePhone()

            verify(exactly = 0) { mockAudioManager.setRingerMode(any()) }
        }

    @Test
    fun `unmutePhone after none mode restores ringer and volume`() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                    false,
                )
            } returns false
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE,
                    false,
                )
            } returns false
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE,
                    true,
                )
            } returns true

            manager.mutePhone()

            manager.unmutePhone()

            verify { mockAudioManager.setStreamVolume(AudioManager.STREAM_RING, initialVolume, 0) }
            verify(exactly = 2) { mockAudioManager.setStreamVolume(AudioManager.STREAM_RING, any(), any()) }
        }

    @Test
    fun `mutePhone with none mode stores old volume and ringer`() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                    false,
                )
            } returns false
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE,
                    false,
                )
            } returns false
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE,
                    true,
                )
            } returns true

            manager.mutePhone()

            verify { mockAudioManager.getStreamVolume(AudioManager.STREAM_RING) }
        }

    @Test
    fun `unmutePhone when ringer changed externally skips restore`() =
        runTest {
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                    false,
                )
            } returns false
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE,
                    false,
                )
            } returns true
            every {
                mockPrefs.getBoolean(
                    ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE,
                    true,
                )
            } returns false

            manager.mutePhone()
            every { mockAudioManager.ringerMode } returns AudioManager.RINGER_MODE_SILENT

            manager.unmutePhone()

            verify(exactly = 0) { mockAudioManager.setRingerMode(initialRingerMode) }
        }
}
