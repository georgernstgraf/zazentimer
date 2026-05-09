package at.priv.graf.zazentimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.Section
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 80)

        assertThat(audio.isPlaying()).isTrue()
        verify { anyConstructed<MediaPlayer>().setVolume(0.8f, 0.8f) }
        verify { anyConstructed<MediaPlayer>().start() }
    }

    @Test
    fun playAbsVolume_fullVolume() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 100)

        verify { anyConstructed<MediaPlayer>().setVolume(1.0f, 1.0f) }
    }

    @Test
    fun playAbsVolume_zeroVolume() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 0)

        verify { anyConstructed<MediaPlayer>().setVolume(0.0f, 0.0f) }
    }

    @Test
    fun playAbsVolume_halfVolume() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 50)

        verify { anyConstructed<MediaPlayer>().setVolume(0.5f, 0.5f) }
    }

    @Test
    fun playAbsVolume_setsAlarmAudioAttributes() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 100)

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
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 100)

        verify { anyConstructed<MediaPlayer>().setDataSource(context, uri) }
        verify { anyConstructed<MediaPlayer>().prepare() }
    }

    @Test
    fun playAbsVolume_nullBell_doesNotPlay() {
        val audio = Audio(context)
        audio.playAbsVolume(null as Bell?, 100)

        assertThat(audio.isPlaying()).isFalse()
    }

    @Test
    fun playAbsVolume_mediaPlayerFailure_handledGracefully() {
        every { anyConstructed<MediaPlayer>().prepare() } throws RuntimeException("prepare failed")

        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 100)

        assertThat(audio.isPlaying()).isFalse()
    }

    @Test
    fun playAbsVolume_withSection_delegatesToBellCollection() {
        BellCollection.initialize(context)
        val bell = BellCollection.getBell(0)!!
        val section = Section(volume = 70)
        section.bellUri = bell.uri.toString()

        val audio = Audio(context)
        audio.playAbsVolume(section)

        assertThat(audio.isPlaying()).isTrue()
        verify { anyConstructed<MediaPlayer>().setVolume(0.7f, 0.7f) }
        BellCollection.release()
    }

    @Test
    fun release_stopsAndReleasesPlayer() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 100)

        audio.release()

        verify { anyConstructed<MediaPlayer>().stop() }
        verify { anyConstructed<MediaPlayer>().reset() }
        verify { anyConstructed<MediaPlayer>().release() }
    }

    @Test
    fun release_whenNoPlayer_doesNotThrow() {
        val audio = Audio(context)
        audio.release()
    }

    @Test
    fun onCompletion_setsPlayingFalse() {
        val uri = Uri.parse("android.resource://at.priv.graf.zazentimer/1234")
        val bell = Bell(uri, "Test Bell")

        val audio = Audio(context)
        audio.playAbsVolume(bell, 100)
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
        val bell1 = Bell(uri1, "Bell 1")
        val bell2 = Bell(uri2, "Bell 2")

        val audio = Audio(context)
        audio.playAbsVolume(bell1, 100)
        audio.playAbsVolume(bell2, 50)

        verify { anyConstructed<MediaPlayer>().stop() }
        verify { anyConstructed<MediaPlayer>().reset() }
        verify { anyConstructed<MediaPlayer>().setVolume(0.5f, 0.5f) }
        assertThat(audio.isPlaying()).isTrue()
    }
}
