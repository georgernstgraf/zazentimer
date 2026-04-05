package de.gaffga.android.zazentimer.bo;

import de.gaffga.android.base.annotations.SaveToBundle;

public class Session {

    @SaveToBundle
    public String description;

    @SaveToBundle
    public int id;

    @SaveToBundle
    public String name;

    public Session() {
    }

    public Session(String str, String str2) {
        this.name = str;
        this.description = str2;
    }

    public String toString() {
        return this.name;
    }
}
