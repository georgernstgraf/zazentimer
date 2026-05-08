package at.priv.graf.zazentimer.service;

import android.content.Context;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import at.priv.graf.zazentimer.bo.Bell;
import at.priv.graf.zazentimer.ZazenTimerActivity;
import at.priv.graf.zazentimer.audio.Audio;
import at.priv.graf.zazentimer.audio.BellCollection;
import at.priv.graf.zazentimer.bo.Section;
import java.util.HashSet;
import java.util.Iterator;

public class Meditation {
    private static final String INTENT_SECTION_ENDED = "ZAZENTIMER_SECTION_ENDED";
    private static final String TAG = "ZMT_Meditation";
    private AlarmManager alarmManager;
    private PendingIntent currentSectionEndIntent;
    private int currentSectionIdx;
    private MeditationService meditationService;
    private PowerManager.WakeLock meditationWakeLock;
    private int oldRingerMode;
    private int oldRingerVolume;
    private int mutedRingerMode = -1;
    private int pauseSectionSeconds;
    private boolean paused;
    private final String sessionName;
    private final SharedPreferences pref;
    private long sectionStartTime;
    private Section[] sections;
    private boolean stopping;
    private int totalSessionTime;
    private HashSet<Audio> audioObjects = new HashSet<>();
    private boolean started = false;
    private final ExecutorService bellExecutor = Executors.newSingleThreadExecutor();

    public Meditation(MeditationService meditationService, String sessionName, Section[] sectionArr) {
        this.stopping = false;
        this.paused = false;
        this.totalSessionTime = 0;
        this.currentSectionIdx = -1;
        this.sessionName = sessionName;
        this.sections = sectionArr;
        this.meditationService = meditationService;
        this.pref = ZazenTimerActivity.getPreferences(meditationService);
        this.alarmManager = (AlarmManager) this.meditationService.getSystemService(Context.ALARM_SERVICE);
        this.stopping = false;
        this.paused = false;
        this.currentSectionIdx = 0;
        this.totalSessionTime = 0;
        for (Section section : sectionArr) {
            this.totalSessionTime += section.duration;
        }
    }

    public String getSessionName() { return this.sessionName; }

    public void start() {
        if (this.started) {
            Log.d(TAG, "start(): Meditation already started!");
            return;
        }
        this.started = true;
        mutePhone();
        startSectionTimer();
        if (Build.VERSION.SDK_INT < 23) {
            createMeditationWakeLock();
        }
    }

    public void stop() {
        if (!this.started) {
            Log.d(TAG, "stop(): Meditation not yet started!");
        } else {
            finishMeditation();
        }
    }

    public void pause() {
        if (!this.started || this.stopping) {
            Log.d(TAG, "pause(): Meditation not yet started or already stopped");
            return;
        }
        if (!this.paused) {
            this.pauseSectionSeconds = getSectionElapsedSeconds();
            this.paused = true;
            if (this.currentSectionEndIntent != null) {
                this.alarmManager.cancel(this.currentSectionEndIntent);
                this.currentSectionEndIntent = null;
            }
            if (Build.VERSION.SDK_INT < 23) {
                releaseMeditationWakeLock();
                return;
            }
            return;
        }
        this.paused = false;
        startSectionTimer();
        if (Build.VERSION.SDK_INT < 23) {
            createMeditationWakeLock();
        }
    }

    public void finishMeditation() {
        this.stopping = true;
        stopSectionTimer();
        releaseAudioObjects();
        unmutePhone();
        if (Build.VERSION.SDK_INT < 23) {
            releaseMeditationWakeLock();
        }
        fireMeditationEnded();
    }

    private void releaseMeditationWakeLock() {
        Log.d(TAG, "Releasing meditation wake lock");
        if (this.meditationWakeLock != null) {
            try {
                if (this.meditationWakeLock.isHeld()) {
                    this.meditationWakeLock.release();
                }
            } catch (Exception e) {
                Log.d(TAG, "error releasing wake lock", e);
            }
        }
    }

    private void createMeditationWakeLock() {
        Log.d(TAG, "Creating meditation wake lock");
        PowerManager powerManager = (PowerManager) this.meditationService.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            this.meditationWakeLock = powerManager.newWakeLock(1, "MeditationWakeLock");
            this.meditationWakeLock.acquire((this.totalSessionTime + 120) * 1000);
        }
    }

    private void fireMeditationEnded() {
        this.meditationService.onMeditationEnd();
    }

    private void stopSectionTimer() {
        if (this.currentSectionEndIntent != null) {
            this.alarmManager.cancel(this.currentSectionEndIntent);
            this.currentSectionEndIntent = null;
        }
    }

    private void startSectionTimer() {
        Section section = this.sections[this.currentSectionIdx];
        this.sectionStartTime = System.currentTimeMillis();
        long triggerTime = this.sectionStartTime + ((section.duration - this.pauseSectionSeconds) * 1000L);
        this.currentSectionEndIntent = PendingIntent.getBroadcast(
            this.meditationService, 0,
            new Intent(INTENT_SECTION_ENDED).setClass(this.meditationService, SectionEndReceiver.class),
            PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerTime, this.currentSectionEndIntent);
        this.alarmManager.setAlarmClock(alarmClockInfo, this.currentSectionEndIntent);
        Log.d(TAG, "Started AlarmClock for next section: triggerTime=" + triggerTime);
    }


    public void onSectionEnd() {
        this.currentSectionEndIntent = null;
        if (this.currentSectionIdx < this.sections.length - 1) {
            playBells(this.sections[this.currentSectionIdx], null);
            this.currentSectionIdx++;
            this.pauseSectionSeconds = 0;
            startSectionTimer();
            return;
        }
        playBells(this.sections[this.currentSectionIdx], () -> Meditation.this.finishMeditation());
    }

    public Section getCurrentSection() {
        return this.sections[this.currentSectionIdx];
    }

    public String getCurrentSectionName() {
        return this.currentSectionIdx >= 0 ? this.sections[this.currentSectionIdx].name : "";
    }

    public int getTotalSessionTime() {
        return this.totalSessionTime;
    }

    public int getCurrentSessionTime() {
        return getCurrentStartSeconds() + getSectionElapsedSeconds();
    }

    public boolean isPaused() {
        return this.paused;
    }

    public boolean isStopped() {
        return this.stopping;
    }

    public int getCurrentStartSeconds() {
        int i = 0;
        for (int i2 = 0; i2 <= this.currentSectionIdx - 1; i2++) {
            i += this.sections[i2].duration;
        }
        return i;
    }

    public int getNextEndSeconds() {
        int i = 0;
        for (int i2 = 0; i2 <= this.currentSectionIdx + 1 && i2 < this.sections.length; i2++) {
            i += this.sections[i2].duration;
        }
        return i;
    }

    public int getNextStartSeconds() {
        int i = 0;
        for (int i2 = 0; i2 <= this.currentSectionIdx; i2++) {
            i += this.sections[i2].duration;
        }
        return i;
    }

    public int getPrevStartSeconds() {
        int i = 0;
        for (int i2 = 0; i2 <= this.currentSectionIdx - 2; i2++) {
            i += this.sections[i2].duration;
        }
        return i;
    }

    public String getNextSectionName() {
        return this.currentSectionIdx < this.sections.length + (-1) ? this.sections[this.currentSectionIdx + 1].name : "";
    }

    public String getNextNextSectionName() {
        return this.currentSectionIdx < this.sections.length + (-2) ? this.sections[this.currentSectionIdx + 2].name : "";
    }

    public int getSectionElapsedSeconds() {
        int round;
        if (this.paused) {
            round = this.pauseSectionSeconds;
        } else {
            round = Math.round((float) ((System.currentTimeMillis() / 1000) - (this.sectionStartTime / 1000))) + this.pauseSectionSeconds;
        }
        return Math.min(round, getCurrentSection().duration);
    }

    private void releaseAudioObjects() {
        Iterator<Audio> it = this.audioObjects.iterator();
        while (it.hasNext()) {
            it.next().release();
        }
        this.audioObjects.clear();
    }

    public boolean isPlaying() {
        Iterator<Audio> it = this.audioObjects.iterator();
        while (it.hasNext()) {
            if (it.next().isPlaying()) {
                return true;
            }
        }
        return false;
    }

    public void playBell(Section section) {
        Bell bellForSection = BellCollection.getInstance().getBellForSection(section);
        if (bellForSection == null) {
            bellForSection = BellCollection.getInstance().getDemoBell();
        }
        boolean z = false;
        Iterator<Audio> it = this.audioObjects.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Audio next = it.next();
            if (!next.isPlaying()) {
                Log.d(TAG, "Found free Audio Object");
                z = true;
                next.playAbsVolume(bellForSection, section.volume);
                break;
            }
        }
        if (z) {
            return;
        }
        Log.d(TAG, "Created new Audio Object for new bell");
        Audio audio = new Audio(this.meditationService);
        audio.playAbsVolume(bellForSection, section.volume);
        this.audioObjects.add(audio);
    }

    private void mutePhone() {
        Log.d(TAG, "muting Phone");
        boolean z = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false);
        boolean z2 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE, false);
        boolean z3 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE, true);
        AudioManager audioManager = (AudioManager) this.meditationService.getSystemService(Context.AUDIO_SERVICE);
        if (!z) {
            if (z2) {
                this.oldRingerMode = audioManager.getRingerMode();
                this.oldRingerVolume = audioManager.getStreamVolume(2);
                audioManager.setRingerMode(1);
                this.mutedRingerMode = audioManager.getRingerMode();
            } else if (z3) {
                this.oldRingerMode = audioManager.getRingerMode();
                this.oldRingerVolume = audioManager.getStreamVolume(2);
                audioManager.setRingerMode(0);
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException unused) {
                }
                audioManager.setStreamVolume(2, 0, 0);
                audioManager.setRingerMode(0);
                this.mutedRingerMode = audioManager.getRingerMode();
            }
        }
    }

    private void unmutePhone() {
        Log.d(TAG, "unmuting Phone");
        boolean z = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false);
        boolean z2 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE, false);
        boolean z3 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE, true);
        AudioManager audioManager = (AudioManager) this.meditationService.getSystemService(Context.AUDIO_SERVICE);
        if (!z && (z2 || z3)) {
            if (this.mutedRingerMode != -1) {
                int currentMode = audioManager.getRingerMode();
                if (currentMode != this.mutedRingerMode) {
                    Log.d(TAG, "Ringer mode changed during meditation (was " + this.mutedRingerMode + ", now " + currentMode + ") — skipping restore");
                    this.mutedRingerMode = -1;
                    return;
                }
            }
            Log.d(TAG, "unmuting: ring=" + this.oldRingerVolume + " ringerMode=" + this.oldRingerMode);
            audioManager.setRingerMode(this.oldRingerMode);
            try {
                Thread.sleep(500L);
            } catch (InterruptedException unused) {
            }
            if (this.oldRingerMode == 2) {
                audioManager.setStreamVolume(2, this.oldRingerVolume, 0);
            }
            audioManager.setRingerMode(this.oldRingerMode);
            this.mutedRingerMode = -1;
        }
    }

    private void playBells(Section section, Runnable onDone) {
        bellExecutor.execute(() -> {
            Log.d(TAG, "Playing bells in background thread");
            PowerManager.WakeLock wakeLock = null;
            PowerManager powerManager = (PowerManager) this.meditationService.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(26, "PlayBells");
                wakeLock.acquire(section.bellcount * 25 * 1000);
                Log.d(TAG, "WakeLock created for playing bells");
            }
            for (int i = 0; i < section.bellcount && !isStopped(); i++) {
                playBell(section);
                if (i < section.bellcount - 1) {
                    for (int i2 = 0; i2 < section.bellpause * 2 && !isStopped(); i2++) {
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException unused) {
                        }
                    }
                }
            }
            Log.d(TAG, "waiting until the bells have finished playing");
            while (isPlaying() && !isStopped()) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException unused2) {
                }
            }
            if (wakeLock != null) {
                try {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "wakeLock release error", e);
                }
                Log.d(TAG, "WakeLock released for playing bells");
            }
            Log.d(TAG, "Done playing bells");
            if (onDone != null) {
                onDone.run();
            }
        });
    }
}
