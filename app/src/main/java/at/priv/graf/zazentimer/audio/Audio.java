package at.priv.graf.zazentimer.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import at.priv.graf.zazentimer.Bell;
import at.priv.graf.zazentimer.bo.Section;

public class Audio implements MediaPlayer.OnCompletionListener {
    private static final String TAG = "ZMT_Audio";
    private Context context;
    private AudioManager manager;
    private volatile boolean playing;
    private MediaPlayer player = null;

    public Audio(Context context) {
        Log.d(TAG, "New Audio Instance");
        this.context = context;
        this.manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void release() {
        Log.d(TAG, "Releasing Audio Instance");
        stopAndRelease();
    }

    private MediaPlayer preparePlayer(Bell bell, int volume) {
        Log.d(TAG, "preparing Audio Player");
        Uri uri = bell.getUri();
        if (uri == null) {
            return null;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setDataSource(this.context, uri);
            mediaPlayer.prepare();
            float f = volume / 100.0f;
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
        }
    }

    public void playAbsVolume(Section section) {
        playAbsVolume(BellCollection.getInstance().getBellForSection(section), section.volume);
    }

    public void playAbsVolume(Bell bell, int volume) {
        if (this.player != null) {
            stopAndRelease();
        }
        this.player = preparePlayer(bell, volume);
        if (this.player != null) {
            this.playing = true;
            Log.d(TAG, "Start playing Bell");
            this.player.setLooping(false);
            this.player.start();
            this.player.setOnCompletionListener(this);
            return;
        }
        Log.e(TAG, "Could not preparePlayer");
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public int getStreamVolume() {
        int streamMaxVolume = this.manager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        float streamVolume = this.manager.getStreamVolume(AudioManager.STREAM_ALARM);
        int round = Math.round((100.0f * streamVolume) / streamMaxVolume);
        Log.d(TAG, "getStreamVolume: vol=" + streamVolume + " max=" + streamMaxVolume + " = " + round);
        return round;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion called");
        this.playing = false;
        stopAndRelease();
    }
}
