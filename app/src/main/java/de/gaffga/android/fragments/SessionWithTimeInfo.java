package de.gaffga.android.fragments;

import de.gaffga.android.zazentimer.bo.Session;

public class SessionWithTimeInfo {
    private final Session session;
    private final int totalTimeSeconds;

    public SessionWithTimeInfo(Session session, int i) {
        this.session = session;
        this.totalTimeSeconds = i;
    }

    public Session getSession() {
        return this.session;
    }

    public int getTotalTimeSeconds() {
        return this.totalTimeSeconds;
    }
}
