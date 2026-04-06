package de.gaffga.android.zazentimer.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import de.gaffga.android.zazentimer.Bell;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;

public class Audio implements MediaPlayer.OnCompletionListener {
    private static final String TAG = "ZMT_Audio";
    private static int numSoundsPlaying;
    private Context context;
    private AudioManager manager;
    private volatile boolean playing;
    private SharedPreferences pref;
    private MediaPlayer player = null;
    private volatile Runnable runOnFinish = null;
    private VolumeCalc volumeCalc = new VolumeCalc();

    static /* synthetic */ int access$010() {
        int i = numSoundsPlaying;
        numSoundsPlaying = i - 1;
        return i;
    }

    public Audio(Context context) {
        this.manager = null;
        Log.d(TAG, "New Audio Instance");
        this.context = context;
        this.pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (this.manager == null) {
            this.manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
    }

    public void release() {
        Log.d(TAG, "Releasing Audio Instance");
        stopAndRelease();
    }

    private MediaPlayer preparePlayer(Bell bell, int i) {
        Log.d(TAG, "preparing Audio Player");
        Uri uri = bell.getUri();
        if (uri == null) {
            return null;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioStreamType(getUsedAudioStream());
            mediaPlayer.setDataSource(this.context, uri);
            mediaPlayer.prepare();
            float f = i / 100.0f;
            mediaPlayer.setVolume(f, f);
            return mediaPlayer;
        } catch (Exception e) {
            Log.e(TAG, "Error creating MediaPlayer", e);
            return null;
        }
    }

    private void stopAndRelease() {
        Log.d(TAG, "stopAndRelease Audio Instance");
        if (this.player != null) {
            try {
                this.player.stop();
                this.player.reset();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
            try {
                this.player.release();
            } catch (Exception e2) {
                Log.d(TAG, e2.getMessage());
            }
            this.player = null;
            if (this.runOnFinish != null) {
                try {
                    this.runOnFinish.run();
                } catch (Exception e3) {
                    Log.d(TAG, e3.getMessage());
                }
                this.runOnFinish = null;
            }
        }
    }

    private int getLogVolume(int i) {
        return (int) (((float) ((Math.exp(i / 100.0f) - 1.0d) / 1.718281828459045d)) * 100.0f);
    }

    public int getUsedAudioStream() {
        return (!this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_OUTPUT_CHANNEL_ALARM, true) && this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_OUTPUT_CHANNEL_MUSIC, false)) ? 3 : 4;
    }

    public void playAbsVolume(Section section) {
        playAbsVolume(BellCollection.getInstance().getBellForSection(section), section.volume);
    }

    public void playAbsVolume(Bell bell, int i) {
        playAbsVolume(bell, i, this.pref.getInt(ZazenTimerActivity.PREF_KEY_VOLUME, 100));
    }

    public class RunOnFinish implements Runnable {
        private int oldVol;

        public RunOnFinish(int i) {
            this.oldVol = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            Log.d(Audio.TAG, "executing runOnFinish");
            Audio.access$010();
            if (Audio.numSoundsPlaying != 0 || Audio.this.manager == null) {
                return;
            }
            Log.d(Audio.TAG, "runOnFinish. Resetting stream volume to " + this.oldVol);
            Audio.this.manager.setStreamVolume(Audio.this.getUsedAudioStream(), this.oldVol, 0);
        }
    }

    public void playAbsVolume(Bell bell, int i, int i2) {
        if (this.player != null) {
            stopAndRelease();
        }
        VolumeInfo volumeInfo = this.volumeCalc.getVolumeInfo(this.manager.getStreamVolume(getUsedAudioStream()), this.manager.getStreamMaxVolume(getUsedAudioStream()), getLogVolume((i * i2) / 100));
        int i3 = volumeInfo.targetStreamVolume;
        int i4 = volumeInfo.targetPlayerVolume;
        int streamVolume = this.manager.getStreamVolume(getUsedAudioStream());
        this.player = preparePlayer(bell, i4);
        if (this.player != null) {
            this.runOnFinish = new RunOnFinish(streamVolume);
            this.manager.setStreamVolume(getUsedAudioStream(), i3, 0);
            for (int i5 = 0; i5 < 30 && this.manager.getStreamVolume(getUsedAudioStream()) != i3; i5++) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException unused) {
                }
            }
            this.playing = true;
            Log.d(TAG, "Start playing Bell");
            this.player.setLooping(false);
            this.player.start();
            this.player.setOnCompletionListener(this);
            numSoundsPlaying++;
            return;
        }
        Log.e(TAG, "Could not preparePlayer");
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public int getStreamVolume() {
        int streamMaxVolume = this.manager.getStreamMaxVolume(getUsedAudioStream());
        float streamVolume = this.manager.getStreamVolume(getUsedAudioStream());
        int round = Math.round((100.0f * streamVolume) / streamMaxVolume);
        Log.d(TAG, "getStreamVolume: vol=" + streamVolume + " max=" + streamMaxVolume + " = " + round);
        return round;
    }

    @Override // android.media.MediaPlayer.OnCompletionListener
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion called");
        this.playing = false;
        stopAndRelease();
    }
}
