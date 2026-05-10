package at.priv.graf.zazentimer.bo

import android.net.Uri

data class Bell(
    var uri: Uri,
    private var bellName: String,
) {
    companion object {
        private const val BELL_PREFIX_LENGTH = 5
    }

    fun getName(): String = if (bellName.startsWith("bell_")) bellName.substring(BELL_PREFIX_LENGTH) else bellName
}
