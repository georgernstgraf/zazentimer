package at.priv.graf.zazentimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class AudioTest {
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var mediaPlayer: MediaPlayer

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaPlayer = mockk<MediaPlayer>(relaxed = true)

        mockkConstructor(MediaPlayer::class)
        every { anyConstructed<MediaPlayer>().setAudioAttributes(any()) } just runs
        every { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any<Uri>()) } just runs
        every { anyConstructed<MediaPlayer>().prepare() } just runs
        every { anyConstructed<MediaPlayer>().setVolume(any(), any()) } just runs
        every { anyConstructed<MediaPlayer>().start() } just runs
        every { anyConstructed<MediaPlayer>().setOnCompletionListener(any()) } just runs
        every { anyConstructed<MediaPlayer>().isLooping = any() } just runs
        every { anyConstructed<MediaPlayer>().stop() } just runs
        every { anyConstructed<MediaPlayer>().reset() } just runs
        every { anyConstructed<MediaPlayer>().release() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isPlaying_initiallyFalse() {
        val audio = Audio(context)
        assertThat(audio.isPlaying()).isFalse()
    }

    @Test
    fun playAbsVolume_setsPlaying() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 80) }

        assertThat(audio.isPlaying()).isTrue()
        verify { anyConstructed<MediaPlayer>().setVolume(0.8f, 0.8f) }
        verify { anyConstructed<MediaPlayer>().start() }
    }

    @Test
    fun playAbsVolume_fullVolume() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 100) }

        verify { anyConstructed<MediaPlayer>().setVolume(1.0f, 1.0f) }
    }

    @Test
    fun playAbsVolume_zeroVolume() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 0) }

        verify { anyConstructed<MediaPlayer>().setVolume(0.0f, 0.0f) }
    }

    @Test
    fun playAbsVolume_halfVolume() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 50) }

        verify { anyConstructed<MediaPlayer>().setVolume(0.5f, 0.5f) }
    }

    @Test
    fun playAbsVolume_setsAlarmAudioAttributes() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 100) }

        verify {
            anyConstructed<MediaPlayer>().setAudioAttributes(
                match { attrs ->
                    attrs.usage == AudioAttributes.USAGE_ALARM &&
                        attrs.contentType == AudioAttributes.CONTENT_TYPE_SONIFICATION
                },
            )
        }
    }

    @Test
    fun playAbsVolume_setsDataSourceAndPrepares() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 100) }

        verify { anyConstructed<MediaPlayer>().setDataSource(context, uri) }
        verify { anyConstructed<MediaPlayer>().prepare() }
    }

    @Test
    fun playAbsVolume_nullUri_doesNotPlay() {
        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(null, 100) }

        assertThat(audio.isPlaying()).isFalse()
    }

    @Test
    fun playAbsVolume_mediaPlayerFailure_throws() {
        every { anyConstructed<MediaPlayer>().prepare() } throws java.io.IOException("prepare failed")

        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        val exception =
            assertThrows(IOException::class.java) {
                runBlocking { audio.playAbsVolume(uri, 100) }
            }

        assertThat(exception).hasMessageThat().contains("prepare failed")
    }

    @Test
    fun release_stopsAndReleasesPlayer() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking {
            audio.playAbsVolume(uri, 100)
            audio.release()
        }

        verify { anyConstructed<MediaPlayer>().stop() }
        verify { anyConstructed<MediaPlayer>().reset() }
        verify { anyConstructed<MediaPlayer>().release() }
    }

    @Test
    fun release_whenNoPlayer_doesNotThrow() {
        val audio = Audio(context)
        runBlocking { audio.release() }
    }

    @Test
    fun onCompletion_setsPlayingFalse() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")

        val audio = Audio(context)
        runBlocking { audio.playAbsVolume(uri, 100) }
        assertThat(audio.isPlaying()).isTrue()

        val listeners = mutableListOf<MediaPlayer.OnCompletionListener>()
        verify { anyConstructed<MediaPlayer>().setOnCompletionListener(capture(listeners)) }
        listeners.first().onCompletion(mockk())

        assertThat(audio.isPlaying()).isFalse()
    }

    @Test
    fun getStreamVolume_returnsCalculatedVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val expected = Math.round(100.0f * currentVolume.toFloat() / maxVolume)

        val audio = Audio(context)
        assertThat(audio.getStreamVolume()).isEqualTo(expected)
    }

    @Test
    fun playAbsVolume_replacesPreviousPlayer() {
        val uri1 = Uri.parse("android.resource://at.priv.graf.zazentimer/1111")
        val uri2 = Uri.parse("android.resource://at.priv.graf.zazentimer/2222")

        val audio = Audio(context)
        runBlocking {
            audio.playAbsVolume(uri1, 100)
            audio.playAbsVolume(uri2, 50)
        }

        verify { anyConstructed<MediaPlayer>().reset() }
        verify { anyConstructed<MediaPlayer>().setVolume(0.5f, 0.5f) }
        assertThat(audio.isPlaying()).isTrue()
    }
}
