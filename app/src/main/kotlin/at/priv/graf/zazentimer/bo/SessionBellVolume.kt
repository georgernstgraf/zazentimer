package at.priv.graf.zazentimer.bo

import java.io.Serializable

data class SessionBellVolume(
    @JvmField var id: Int = 0,
    @JvmField var fkSession: Int = 0,
    @JvmField var bell: Int? = null,
    @JvmField var bellUri: String? = null,
    @JvmField var volume: Int = 100,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
