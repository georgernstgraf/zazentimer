package at.priv.graf.zazentimer.bo;

import java.io.Serializable;
import java.util.Locale;

public class Section implements Serializable {

    public int bell;

    public String bellUri;

    public int bellcount;

    public int bellpause;

    public int duration;

    public int fkSession;

    public int id;

    public String name;

    public int rank;

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
