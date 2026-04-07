package de.gaffga.android.zazentimer.bo;

public class Session {

    public String description;

    public int id;

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
