package de.gaffga.android.zazentimer.bo;

import de.gaffga.android.base.annotations.SaveToBundle;
import de.gaffga.android.mapping.annotations.DbColumn;
import de.gaffga.android.mapping.annotations.DbPrimaryKey;
import de.gaffga.android.mapping.annotations.DbTable;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import java.io.Serializable;
import java.util.Locale;

@DbTable(name = "sections")
public class Section implements Serializable {

    @SaveToBundle
    @DbColumn(name = "bell")
    public int bell;

    @SaveToBundle
    @DbColumn(name = "belluri")
    public String bellUri;

    @SaveToBundle
    @DbColumn(name = "bellcount")
    public int bellcount;

    @SaveToBundle
    @DbColumn(name = "bellpause")
    public int bellpause;

    @SaveToBundle
    @DbColumn(name = "duration")
    public int duration;

    @SaveToBundle
    @DbColumn(name = "fk_session")
    public int fkSession;

    @DbPrimaryKey
    @SaveToBundle
    @DbColumn(name = "_id")
    public int id;

    @SaveToBundle
    @DbColumn(name = "name")
    public String name;

    @SaveToBundle
    @DbColumn(name = "rank")
    public int rank;

    @SaveToBundle
    @DbColumn(name = ZazenTimerActivity.PREF_KEY_VOLUME)
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
