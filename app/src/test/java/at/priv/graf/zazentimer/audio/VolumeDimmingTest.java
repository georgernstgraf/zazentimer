package at.priv.graf.zazentimer.audio;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class VolumeDimmingTest {

    @Test
    public void dimmingZero_mapsToFullVolume() {
        int dimming = 0;
        int volume = 100 - dimming;
        assertEquals(100, volume);
    }

    @Test
    public void dimmingFull_mapsToZeroVolume() {
        int dimming = 100;
        int volume = 100 - dimming;
        assertEquals(0, volume);
    }

    @Test
    public void dimmingFifty_mapsToFiftyVolume() {
        int dimming = 50;
        int volume = 100 - dimming;
        assertEquals(50, volume);
    }

    @Test
    public void volumeToMediaPlayerFloat_fullVolume() {
        int volume = 100;
        float f = volume / 100.0f;
        assertEquals(1.0f, f, 0.001f);
    }

    @Test
    public void volumeToMediaPlayerFloat_zeroVolume() {
        int volume = 0;
        float f = volume / 100.0f;
        assertEquals(0.0f, f, 0.001f);
    }

    @Test
    public void volumeToMediaPlayerFloat_halfVolume() {
        int volume = 50;
        float f = volume / 100.0f;
        assertEquals(0.5f, f, 0.001f);
    }

    @Test
    public void roundTrip_dimmingToVolumeToDimming() {
        for (int dimming = 0; dimming <= 100; dimming++) {
            int volume = 100 - dimming;
            int backToDimming = 100 - volume;
            assertEquals(dimming, backToDimming);
        }
    }
}
