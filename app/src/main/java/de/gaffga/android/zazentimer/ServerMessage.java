package de.gaffga.android.zazentimer;

public class ServerMessage {
    private String message;
    private int nr;
    private String title;

    public ServerMessage(int i, String str, String str2) {
        this.nr = i;
        this.title = str;
        this.message = str2;
    }

    public String getTitle() {
        return this.title;
    }

    public int getNr() {
        return this.nr;
    }

    public String getMessage() {
        return this.message;
    }
}
