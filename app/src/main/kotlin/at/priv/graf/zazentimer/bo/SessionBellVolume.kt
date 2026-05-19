package at.priv.graf.zazentimer.bo

import at.priv.graf.zazentimer.Constants
import java.io.Serializable

data class SessionBellVolume(
    @JvmField var id: Int = 0,
    @JvmField var fkSession: Int = 0,
    @JvmField var bellId: Int = 0,
    @JvmField var volume: Int = Constants.DEFAULT_BELL_VOLUME,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
