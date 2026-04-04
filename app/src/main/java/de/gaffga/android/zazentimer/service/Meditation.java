package de.gaffga.android.zazentimer.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import de.gaffga.android.zazentimer.Bell;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.audio.Audio;
import de.gaffga.android.zazentimer.audio.BellCollection;
import de.gaffga.android.zazentimer.bo.Section;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class Meditation {
    private static final String INTENT_SECTION_ENDED = "ZAZENTIMER_SECTION_ENDED";
    private static final String TAG = "ZMT_Meditation";
    private AlarmManager alarmManager;
    private PendingIntent currentSectionEndIntent;
    private int currentSectionIdx;
    private MeditationService meditationService;
    private PowerManager.WakeLock meditationWakeLock;
    private int oldAlarmVolume;
    private int oldMusicVolume;
    private int oldRingerMode;
    private int oldRingerVolume;
    private int pauseSectionSeconds;
    private boolean paused;
    private final SharedPreferences pref;
    private SectionEndReceiver sectionEndReceiver;
    private long sectionStartTime;
    private Section[] sections;
    private boolean stopping;
    private int totalSessionTime;
    private HashSet<Audio> audioObjects = new HashSet<>();
    private boolean started = false;
    private LinkedList<PlayBellsAsync> playBellsAsyncTasks = new LinkedList<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SectionEndReceiver extends BroadcastReceiver {
        private final Meditation meditation;

        SectionEndReceiver(Meditation meditation) {
            this.meditation = meditation;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Log.d(Meditation.TAG, "onReceive: context=" + context.getClass().getName() + " " + intent.getAction());
            this.meditation.onSectionEnd();
        }
    }

    public Meditation(MeditationService meditationService, Section[] sectionArr) {
        this.stopping = false;
        this.paused = false;
        this.totalSessionTime = 0;
        this.currentSectionIdx = -1;
        this.sections = sectionArr;
        this.meditationService = meditationService;
        this.pref = ZazenTimerActivity.getPreferences(meditationService);
        this.alarmManager = (AlarmManager) this.meditationService.getSystemService("alarm");
        this.stopping = false;
        this.paused = false;
        this.currentSectionIdx = 0;
        this.totalSessionTime = 0;
        for (Section section : sectionArr) {
            this.totalSessionTime += section.duration;
        }
    }

    public void start() {
        if (this.started) {
            Log.d(TAG, "start(): Meditation already started!");
            return;
        }
        this.started = true;
        mutePhone();
        installAlarmReceiver();
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

    /* JADX INFO: Access modifiers changed from: private */
    public void finishMeditation() {
        this.stopping = true;
        stopSectionTimer();
        uninstallAlarmReceiver();
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
        PowerManager powerManager = (PowerManager) this.meditationService.getSystemService("power");
        if (powerManager != null) {
            this.meditationWakeLock = powerManager.newWakeLock(1, "MeditationWakeLock");
            this.meditationWakeLock.acquire((this.totalSessionTime + 120) * 1000);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addPlayBellsAsync(PlayBellsAsync playBellsAsync) {
        Log.d(TAG, "adding playing bell task to list");
        this.playBellsAsyncTasks.add(playBellsAsync);
        Log.d(TAG, "  number of playing tasks is now: " + this.playBellsAsyncTasks.size());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removePlayBellsAsync(PlayBellsAsync playBellsAsync) {
        Log.d(TAG, "removing playing bell task from list");
        this.playBellsAsyncTasks.remove(playBellsAsync);
        Log.d(TAG, "  number of playing tasks is now: " + this.playBellsAsyncTasks.size());
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
        this.sectionStartTime = SystemClock.elapsedRealtime();
        long j = this.sectionStartTime + ((section.duration - this.pauseSectionSeconds) * 1000);
        this.currentSectionEndIntent = PendingIntent.getBroadcast(this.meditationService, 0, new Intent(INTENT_SECTION_ENDED), PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 23) {
            this.alarmManager.setExactAndAllowWhileIdle(2, j, this.currentSectionEndIntent);
        } else {
            this.alarmManager.setExact(2, j, this.currentSectionEndIntent);
        }
        Log.d(TAG, "Started Alarm for next section: sectionStartTime=" + this.sectionStartTime + " alertTime=" + j);
    }

    private void installAlarmReceiver() {
        uninstallAlarmReceiver();
        this.sectionEndReceiver = new SectionEndReceiver(this);
        ContextCompat.registerReceiver(this.meditationService, this.sectionEndReceiver, new IntentFilter(INTENT_SECTION_ENDED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void uninstallAlarmReceiver() {
        try {
            try {
                if (this.sectionEndReceiver != null) {
                    this.meditationService.unregisterReceiver(this.sectionEndReceiver);
                }
            } catch (Exception e) {
                Log.e(TAG, "error unregistering Receiver", e);
            }
        } finally {
            this.sectionEndReceiver = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSectionEnd() {
        this.currentSectionEndIntent = null;
        if (this.currentSectionIdx < this.sections.length - 1) {
            new PlayBellsAsync(this, null).execute(this.sections[this.currentSectionIdx]);
            this.currentSectionIdx++;
            this.pauseSectionSeconds = 0;
            startSectionTimer();
            return;
        }
        new PlayBellsAsync(this, new Runnable() { // from class: de.gaffga.android.zazentimer.service.Meditation.1
            @Override // java.lang.Runnable
            public void run() {
                Meditation.this.finishMeditation();
            }
        }).execute(this.sections[this.currentSectionIdx]);
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
            round = Math.round((float) ((SystemClock.elapsedRealtime() / 1000) - (this.sectionStartTime / 1000))) + this.pauseSectionSeconds;
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

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPlaying() {
        Iterator<Audio> it = this.audioObjects.iterator();
        while (it.hasNext()) {
            if (it.next().isPlaying()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
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
        boolean z4 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_ALARM, true);
        boolean z5 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MUSIC, false);
        AudioManager audioManager = (AudioManager) this.meditationService.getSystemService("audio");
        if (!z) {
            if (z2) {
                this.oldRingerMode = audioManager.getRingerMode();
                this.oldRingerVolume = audioManager.getStreamVolume(2);
                audioManager.setRingerMode(1);
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
            }
        }
        if (z4) {
            this.oldAlarmVolume = audioManager.getStreamVolume(4);
            audioManager.setStreamVolume(4, 0, 0);
        }
        if (z5) {
            this.oldMusicVolume = audioManager.getStreamVolume(3);
            audioManager.setStreamVolume(3, 0, 0);
        }
    }

    private void unmutePhone() {
        Log.d(TAG, "unmuting Phone");
        boolean z = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false);
        boolean z2 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE, false);
        boolean z3 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE, true);
        boolean z4 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_ALARM, true);
        boolean z5 = this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MUSIC, false);
        AudioManager audioManager = (AudioManager) this.meditationService.getSystemService("audio");
        if (!z && (z2 || z3)) {
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
        }
        if (z4) {
            Log.d(TAG, "unmuting: alarm=" + this.oldAlarmVolume);
            audioManager.setStreamVolume(4, this.oldAlarmVolume, 0);
        }
        if (z5) {
            Log.d(TAG, "unmuting: music=" + this.oldMusicVolume);
            audioManager.setStreamVolume(3, this.oldMusicVolume, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PlayBellsAsync extends AsyncTask<Section, Void, Void> {
        private final Meditation meditation;
        private final Runnable onDone;

        PlayBellsAsync(Meditation meditation, Runnable runnable) {
            this.meditation = meditation;
            this.onDone = runnable;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Void r1) {
            if (this.onDone != null) {
                this.onDone.run();
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Section... sectionArr) {
            PowerManager.WakeLock wakeLock;
            Log.d(Meditation.TAG, "Playing bells in AsyncTask");
            this.meditation.addPlayBellsAsync(this);
            Section section = sectionArr[0];
            PowerManager powerManager = (PowerManager) this.meditation.meditationService.getSystemService("power");
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(26, "PlayBellsAsync");
                wakeLock.acquire(section.bellcount * 25 * 1000);
                Log.d(Meditation.TAG, "WakeLock created for playing bells");
            } else {
                wakeLock = null;
            }
            for (int i = 0; i < section.bellcount && !this.meditation.isStopped(); i++) {
                this.meditation.playBell(section);
                if (i < section.bellcount - 1) {
                    for (int i2 = 0; i2 < section.bellpause * 2 && !this.meditation.isStopped(); i2++) {
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException unused) {
                        }
                    }
                }
            }
            Log.d(Meditation.TAG, "waiting until the bells have finished playing");
            while (this.meditation.isPlaying() && !this.meditation.isStopped()) {
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
                    Log.d(Meditation.TAG, "wakeLock release error", e);
                }
                Log.d(Meditation.TAG, "WakeLock released for playing bells");
            }
            Log.d(Meditation.TAG, "Done playing bells in AsyncTask");
            this.meditation.removePlayBellsAsync(this);
            return null;
        }
    }
}
