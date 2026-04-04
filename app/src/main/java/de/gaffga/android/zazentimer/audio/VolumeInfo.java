package de.gaffga.android.zazentimer.audio;

public class VolumeInfo {
    public int targetPlayerVolume;
    public int targetStreamVolume;

    public VolumeInfo(int i, int i2) {
        this.targetPlayerVolume = i2;
        this.targetStreamVolume = i;
    }
}
