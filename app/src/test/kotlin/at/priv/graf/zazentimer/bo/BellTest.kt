package at.priv.graf.zazentimer.bo

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BellTest {
    @Test
    fun getName_withBellPrefix_stripsPrefix() {
        val bell = Bell(Uri.parse("content://audio/bell.mp3"), "bell_Tibetan")
        assertThat(bell.getName()).isEqualTo("Tibetan")
    }

    @Test
    fun getName_withoutPrefix_returnsOriginal() {
        val bell = Bell(Uri.parse("content://audio/bell.mp3"), "Singing Bowl")
        assertThat(bell.getName()).isEqualTo("Singing Bowl")
    }

    @Test
    fun getName_emptyString_returnsEmpty() {
        val bell = Bell(Uri.parse("content://audio/bell.mp3"), "")
        assertThat(bell.getName()).isEmpty()
    }

    @Test
    fun getName_bellPrefixOnly_returnsEmpty() {
        val bell = Bell(Uri.parse("content://audio/bell.mp3"), "bell_")
        assertThat(bell.getName()).isEmpty()
    }

    @Test
    fun getName_differentPrefix_notStripped() {
        val bell = Bell(Uri.parse("content://audio/bell.mp3"), "chime_Tibetan")
        assertThat(bell.getName()).isEqualTo("chime_Tibetan")
    }
}
