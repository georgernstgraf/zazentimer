package at.priv.graf.zazentimer.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

object BellValidator {
    @Suppress("TooGenericExceptionCaught")
    fun validate(
        context: Context,
        uri: Uri,
    ) {
        val player = MediaPlayer()
        try {
            player.setDataSource(context, uri)
            player.setVolume(0f, 0f)
            player.prepare()
            player.start()
        } catch (e: Exception) {
            throw BellImportException(e.message ?: "Unplayable audio file", e)
        } finally {
            try {
                player.stop()
            } catch (_: Exception) {
                // Ignore: stop() fails when prepare()/start() never succeeded.
            }
            player.release()
        }
    }
}
