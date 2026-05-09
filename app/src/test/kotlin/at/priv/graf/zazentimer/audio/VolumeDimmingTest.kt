package at.priv.graf.zazentimer.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeDimmingTest {
    @Test
    fun dimmingZero_mapsToFullVolume() {
        val dimming = 0
        val volume = 100 - dimming
        assertEquals(100, volume)
    }

    @Test
    fun dimmingFull_mapsToZeroVolume() {
        val dimming = 100
        val volume = 100 - dimming
        assertEquals(0, volume)
    }

    @Test
    fun dimmingFifty_mapsToFiftyVolume() {
        val dimming = 50
        val volume = 100 - dimming
        assertEquals(50, volume)
    }

    @Test
    fun volumeToMediaPlayerFloat_fullVolume() {
        val volume = 100
        val f = volume / 100.0f
        assertEquals(1.0f, f, 0.001f)
    }

    @Test
    fun volumeToMediaPlayerFloat_zeroVolume() {
        val volume = 0
        val f = volume / 100.0f
        assertEquals(0.0f, f, 0.001f)
    }

    @Test
    fun volumeToMediaPlayerFloat_halfVolume() {
        val volume = 50
        val f = volume / 100.0f
        assertEquals(0.5f, f, 0.001f)
    }

    @Test
    fun roundTrip_dimmingToVolumeToDimming() {
        for (dimming in 0..100) {
            val volume = 100 - dimming
            val backToDimming = 100 - volume
            assertEquals(dimming, backToDimming)
        }
    }
}
