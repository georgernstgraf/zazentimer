package de.gaffga.android.zazentimer.audio;

public class VolumeCalc {
    private int getTargetStreamVol(int i, int i2, int i3) {
        while ((i * 100) / i2 <= i3 && i < i2) {
            i++;
        }
        return i;
    }

    private int getTargetPlayerVol(int i, int i2, int i3) {
        return Math.round((i3 * 100.0f) / ((i * 100.0f) / i2));
    }

    public VolumeInfo getVolumeInfo(int i, int i2, int i3) {
        int targetStreamVol = getTargetStreamVol(i, i2, i3);
        return new VolumeInfo(targetStreamVol, getTargetPlayerVol(targetStreamVol, i2, i3));
    }
}
