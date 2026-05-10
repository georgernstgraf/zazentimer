package at.priv.graf.zazentimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

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

    suspend fun release() {
        Log.d(TAG, "Releasing Audio Instance")
        withContext(Dispatchers.IO) {
            stopAndRelease()
        }
    }

    private fun preparePlayer(
        bell: Bell,
        volume: Int,
    ): MediaPlayer? {
        Log.d(TAG, "preparing Audio Player")
        val uri: Uri = bell.uri
        val mediaPlayer = MediaPlayer()
        var result: MediaPlayer? = null
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
            val f = volume / VOLUME_SCALE
            mediaPlayer.setVolume(f, f)
            result = mediaPlayer
        } catch (e: IOException) {
            Log.e(TAG, "Error creating MediaPlayer", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error creating MediaPlayer", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error creating MediaPlayer", e)
        }
        return result
    }

    private fun stopAndRelease() {
        Log.d(TAG, "stopAndRelease Audio Instance")
        this.player?.let { p ->
            try {
                p.stop()
                p.reset()
            } catch (e: IllegalStateException) {
                Log.d(TAG, e.message ?: "")
            }
            try {
                p.release()
            } catch (e: IllegalStateException) {
                Log.d(TAG, e.message ?: "")
            }
            this.player = null
        }
    }

    suspend fun playAbsVolume(section: Section) {
        playAbsVolume(BellCollection.getBellForSection(section), section.volume)
    }

    suspend fun playAbsVolume(
        bell: Bell?,
        volume: Int,
    ) {
        if (this.player != null) {
            stopAndRelease()
        }
        this.player =
            withContext(Dispatchers.IO) {
                bell?.let { preparePlayer(it, volume) }
            }
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
        val round = Math.round((VOLUME_SCALE * streamVolume) / streamMaxVolume)
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

        private const val VOLUME_SCALE = 100.0f
    }
}
