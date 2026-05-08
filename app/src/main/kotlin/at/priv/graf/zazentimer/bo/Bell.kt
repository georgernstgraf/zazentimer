package at.priv.graf.zazentimer.bo

import android.net.Uri

data class Bell(
    var uri: Uri,
    private var _name: String
) {
    fun getName(): String = if (_name.startsWith("bell_")) _name.substring(5) else _name
}
