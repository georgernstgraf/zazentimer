package at.priv.graf.zazentimer.service;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import at.priv.graf.zazentimer.DbOperations;
import at.priv.graf.zazentimer.RunOnConnect;
import at.priv.graf.zazentimer.ZazenTimerActivity;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class MeditationViewModel extends AndroidViewModel {
    private static final String TAG = "ZMT_MeditationViewModel";

    private final MutableLiveData<MeditationUiState> meditationState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> meditationEnded = new MutableLiveData<>();

    private ServCon serviceConnection = null;
    private Handler handler = null;
    private PowerManager.WakeLock wakeLock = null;
    private Intent serviceIntent = null;
    private boolean updateRunning = false;
    private int selectedSessionId = -1;
    private boolean timerViewInitialized = false;
    private final DbOperations dbOperations;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!updateRunning) {
                return;
            }
            pollMeditationState();
            handler.postDelayed(this, 300L);
        }
    };

    @Inject
    public MeditationViewModel(@NonNull Application application, DbOperations dbOperations) {
        super(application);
        this.dbOperations = dbOperations;
        meditationEnded.setValue(false);
        emitIdleState();
    }

    public LiveData<MeditationUiState> getMeditationState() {
        return meditationState;
    }

    public LiveData<Boolean> getMeditationEnded() {
        return meditationEnded;
    }

    public void notifyMeditationEnded() {
        meditationEnded.setValue(true);
    }

    public void consumeMeditationEnded() {
        meditationEnded.setValue(false);
    }

    public void bindToService(Application app, Handler h, Runnable callback) {
        this.handler = h;
        if (this.serviceIntent == null) {
            this.serviceIntent = new Intent(app, MeditationService.class);
        }
        if (this.serviceConnection == null) {
            Log.d(TAG, "serviceConnection is null - making fresh connection service");
            this.serviceConnection = new ServCon(app);
            this.serviceConnection.setRunOnConnect(new RunOnConnect(h, callback));
            app.bindService(this.serviceIntent, this.serviceConnection, Context.BIND_AUTO_CREATE);
            return;
        }
        if (this.serviceConnection.isBound()) {
            Log.d(TAG, "service is already bound");
            h.post(callback);
        } else {
            Log.d(TAG, "service comm existing, but service not bound - rebinding");
            app.bindService(this.serviceIntent, this.serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindFromService(Application app) {
        if (this.serviceConnection != null && this.serviceConnection.isBound()) {
            try {
                app.unbindService(this.serviceConnection);
            } catch (Exception ignored) {
            }
        }
        this.serviceConnection = null;
    }

    public void startMeditation(Application app, int sessionId) {
        this.selectedSessionId = sessionId;
        this.timerViewInitialized = false;
        if (this.serviceIntent == null) {
            this.serviceIntent = new Intent(app, MeditationService.class);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(this.serviceIntent);
        } else {
            app.startService(this.serviceIntent);
        }
        bindToService(app, this.handler != null ? this.handler : new Handler(Looper.getMainLooper()), new Runnable() {
            @Override
            public void run() {
                if (MeditationViewModel.this.serviceConnection != null) {
                    MeditationViewModel.this.serviceConnection.startMeditation(sessionId);
                }
            }
        });
    }

    public void startUpdateThread() {
        if (this.handler == null) {
            this.handler = new Handler(Looper.getMainLooper());
        }
        stopUpdateThread();
        this.updateRunning = true;
        this.timerViewInitialized = false;
        this.handler.postDelayed(this.updateRunnable, 300L);
    }

    public void stopUpdateThread() {
        this.updateRunning = false;
        if (this.handler != null) {
            this.handler.removeCallbacks(this.updateRunnable);
        }
        emitIdleState();
    }

    public void emitIdleState() {
        int sessionId = this.selectedSessionId;
        if (sessionId == -1) {
            SharedPreferences prefs = ZazenTimerActivity.getPreferences(getApplication());
            sessionId = prefs.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1);
        }
        if (sessionId == -1 || dbOperations == null) {
            meditationState.setValue(new MeditationUiState(
                    0, 0, 0, 0, 0, 0, 0,
                    "", "", "",
                    "",
                    false, false
            ));
            return;
        }
        at.priv.graf.zazentimer.bo.Session session = dbOperations.readSession(sessionId);
        if (session == null) {
            meditationState.setValue(new MeditationUiState(
                    0, 0, 0, 0, 0, 0, 0,
                    "", "", "",
                    "",
                    false, false
            ));
            return;
        }
        at.priv.graf.zazentimer.bo.Section[] sections = dbOperations.readSections(sessionId);
        if (sections == null || sections.length == 0) {
            meditationState.setValue(new MeditationUiState(
                    0, 0, 0, 0, 0, 0, 0,
                    "", "", "",
                    session.name,
                    false, false
            ));
            return;
        }
        int totalSessionTime = 0;
        for (at.priv.graf.zazentimer.bo.Section s : sections) {
            totalSessionTime += s.duration;
        }
        int currentStartSeconds = 0;
        int nextEndSeconds = sections.length > 1 ? sections[0].duration + sections[1].duration : totalSessionTime;
        int nextStartSeconds = sections[0].duration;
        int prevStartSeconds = 0;
        String currentSectionName = sections[0].name;
        String nextSectionName = sections.length > 1 ? sections[1].name : "";
        String nextNextSectionName = sections.length > 2 ? sections[2].name : "";
        meditationState.setValue(new MeditationUiState(
                currentStartSeconds,
                totalSessionTime,
                nextEndSeconds,
                nextStartSeconds,
                prevStartSeconds,
                0,
                0,
                currentSectionName,
                nextSectionName,
                nextNextSectionName,
                session.name,
                false,
                false
        ));
    }

    private void pollMeditationState() {
        if (this.serviceConnection == null) {
            return;
        }
        Meditation meditation = this.serviceConnection.getRunningMeditation();
        if (meditation == null) {
            return;
        }
        if (meditation.getCurrentSection() == null) {
            return;
        }
        if (!this.timerViewInitialized) {
            this.timerViewInitialized = true;
            MeditationUiState initState = new MeditationUiState(
                    0,
                    meditation.getTotalSessionTime(),
                    meditation.getNextEndSeconds(),
                    meditation.getNextStartSeconds(),
                    meditation.getPrevStartSeconds(),
                    0,
                    0,
                    meditation.getCurrentSectionName(),
                    meditation.getNextSectionName(),
                    meditation.getNextNextSectionName(),
                    meditation.getSessionName(),
                    meditation.isPaused(),
                    true
            );
            meditationState.setValue(initState);
            return;
        }
        MeditationUiState state = new MeditationUiState(
                meditation.getCurrentStartSeconds(),
                meditation.getTotalSessionTime(),
                meditation.getNextEndSeconds(),
                meditation.getNextStartSeconds(),
                meditation.getPrevStartSeconds(),
                meditation.getSectionElapsedSeconds(),
                meditation.getCurrentSessionTime(),
                meditation.getCurrentSectionName(),
                meditation.getNextSectionName(),
                meditation.getNextNextSectionName(),
                meditation.getSessionName(),
                meditation.isPaused(),
                true
        );
        meditationState.setValue(state);
    }

    public void pauseMeditation() {
        if (this.serviceConnection != null) {
            this.serviceConnection.pauseMeditation();
        }
    }

    public void stopMeditation() {
        if (this.serviceConnection != null) {
            this.serviceConnection.stopMeditation();
        }
    }

    public boolean isPaused() {
        if (this.serviceConnection == null || this.serviceConnection.getRunningMeditation() == null) {
            return false;
        }
        return this.serviceConnection.getRunningMeditation().isPaused();
    }

    public void acquireScreenWakeLock(Application app, SharedPreferences pref) {
        boolean keepScreenOn = pref.getBoolean(ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON, false);
        if (!keepScreenOn) {
            return;
        }
        PowerManager powerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            int totalSeconds = 0;
            for (at.priv.graf.zazentimer.bo.Section section : dbOperations.readSections(selectedSessionId)) {
                totalSeconds += section.duration;
            }
            this.wakeLock = null;
            this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenOnWakeLock");
            int timeoutSeconds = totalSeconds + 60;
            this.wakeLock.acquire(timeoutSeconds * 1000L);
            Log.i(TAG, "Acquired WakeLock to keep screen on for " + timeoutSeconds + " seconds");
        }
    }

    public void releaseScreenWakeLock() {
        if (this.wakeLock != null) {
            try {
                if (this.wakeLock.isHeld()) {
                    this.wakeLock.release();
                }
            } catch (Exception e) {
                Log.d(TAG, "wakeLock release error", e);
            }
            this.wakeLock = null;
            Log.i(TAG, "ScreenOn-WakeLock released");
        }
    }

    public int getSelectedSessionId() {
        return this.selectedSessionId;
    }

    public void setSelectedSessionId(int sessionId) {
        this.selectedSessionId = sessionId;
    }

    public Intent getServiceIntent(Application app) {
        if (this.serviceIntent == null) {
            this.serviceIntent = new Intent(app, MeditationService.class);
        }
        return this.serviceIntent;
    }

    public boolean isServiceConnected() {
        return this.serviceConnection != null && this.serviceConnection.isBound();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopUpdateThread();
        releaseScreenWakeLock();
    }
}
