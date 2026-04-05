package de.gaffga.android.zazentimer.bo;

import de.gaffga.android.base.annotations.SaveToBundle;
import java.io.Serializable;
import java.util.Locale;

public class Section implements Serializable {

    @SaveToBundle
    public int bell;

    @SaveToBundle
    public String bellUri;

    @SaveToBundle
    public int bellcount;

    @SaveToBundle
    public int bellpause;

    @SaveToBundle
    public int duration;

    @SaveToBundle
    public int fkSession;

    @SaveToBundle
    public int id;

    @SaveToBundle
    public String name;

    @SaveToBundle
    public int rank;

    @SaveToBundle
    public int volume;

    public Section() {
        this.rank = -1;
        this.volume = 100;
        this.bell = -2;
    }

    public Section(String str, int i) {
        this.rank = -1;
        this.volume = 100;
        this.bell = -2;
        this.id = 0;
        this.name = str;
        this.duration = i;
        this.bell = -2;
        this.bellcount = 1;
        this.bellpause = 1;
    }

    public String getDurationString() {
        return String.format(Locale.US, "%02d:%02d", Integer.valueOf(this.duration / 60), Integer.valueOf(this.duration % 60));
    }

    public String toString() {
        return this.name;
    }
}
