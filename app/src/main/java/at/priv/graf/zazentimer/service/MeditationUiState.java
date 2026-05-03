package at.priv.graf.zazentimer.service;

public class MeditationUiState {
    public final int currentStartSeconds;
    public final int totalSessionTime;
    public final int nextEndSeconds;
    public final int nextStartSeconds;
    public final int prevStartSeconds;
    public final int sectionElapsedSeconds;
    public final int sessionElapsedSeconds;
    public final String currentSectionName;
    public final String nextSectionName;
    public final String nextNextSectionName;
    public final String sessionName;
    public final boolean paused;
    public final boolean running;

    public MeditationUiState(int currentStartSeconds, int totalSessionTime,
            int nextEndSeconds, int nextStartSeconds, int prevStartSeconds,
            int sectionElapsedSeconds, int sessionElapsedSeconds,
            String currentSectionName, String nextSectionName, String nextNextSectionName,
            String sessionName,
            boolean paused, boolean running) {
        this.currentStartSeconds = currentStartSeconds;
        this.totalSessionTime = totalSessionTime;
        this.nextEndSeconds = nextEndSeconds;
        this.nextStartSeconds = nextStartSeconds;
        this.prevStartSeconds = prevStartSeconds;
        this.sectionElapsedSeconds = sectionElapsedSeconds;
        this.sessionElapsedSeconds = sessionElapsedSeconds;
        this.currentSectionName = currentSectionName;
        this.nextSectionName = nextSectionName;
        this.nextNextSectionName = nextNextSectionName;
        this.sessionName = sessionName;
        this.paused = paused;
        this.running = running;
    }
}
