package at.priv.graf.zazentimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.Section

class Audio(
    private val context: Context,
) : MediaPlayer.OnCompletionListener {
    @Volatile
    private var playing: Boolean = false
    private var player: MediaPlayer? = null
    private val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        Log.d(TAG, "New Audio Instance")
    }

    fun release() {
        Log.d(TAG, "Releasing Audio Instance")
        stopAndRelease()
    }

    private fun preparePlayer(
        bell: Bell,
        volume: Int,
    ): MediaPlayer? {
        Log.d(TAG, "preparing Audio Player")
        val uri: Uri = bell.uri
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mediaPlayer.setDataSource(this.context, uri)
            mediaPlayer.prepare()
            val f = volume / 100.0f
            mediaPlayer.setVolume(f, f)
            return mediaPlayer
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MediaPlayer", e)
            return null
        }
    }

    private fun stopAndRelease() {
        Log.d(TAG, "stopAndRelease Audio Instance")
        this.player?.let { p ->
            try {
                p.stop()
                p.reset()
            } catch (e: Exception) {
                Log.d(TAG, e.message ?: "")
            }
            try {
                p.release()
            } catch (e: Exception) {
                Log.d(TAG, e.message ?: "")
            }
            this.player = null
        }
    }

    fun playAbsVolume(section: Section) {
        playAbsVolume(BellCollection.getBellForSection(section), section.volume)
    }

    fun playAbsVolume(
        bell: Bell?,
        volume: Int,
    ) {
        if (this.player != null) {
            stopAndRelease()
        }
        this.player = bell?.let { preparePlayer(it, volume) }
        this.player?.let { p ->
            this.playing = true
            Log.d(TAG, "Start playing Bell")
            p.isLooping = false
            p.start()
            p.setOnCompletionListener(this)
            return
        }
        Log.e(TAG, "Could not preparePlayer")
    }

    fun isPlaying(): Boolean = this.playing

    fun getStreamVolume(): Int {
        val streamMaxVolume = this.manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val streamVolume = this.manager.getStreamVolume(AudioManager.STREAM_ALARM).toFloat()
        val round = Math.round((100.0f * streamVolume) / streamMaxVolume)
        Log.d(TAG, "getStreamVolume: vol=$streamVolume max=$streamMaxVolume = $round")
        return round
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        Log.d(TAG, "onCompletion called")
        this.playing = false
        stopAndRelease()
    }

    companion object {
        private const val TAG = "ZMT_Audio"
    }
}
