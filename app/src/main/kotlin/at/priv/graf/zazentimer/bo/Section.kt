package at.priv.graf.zazentimer.bo

import java.io.Serializable
import java.util.Locale

data class Section(
    @JvmField var bell: Int = -2,
    @JvmField var bellUri: String? = null,
    @JvmField var bellcount: Int = 0,
    @JvmField var bellpause: Int = 0,
    @JvmField var duration: Int = 0,
    @JvmField var fkSession: Int = 0,
    @JvmField var id: Int = 0,
    @JvmField var name: String? = null,
    @JvmField var rank: Int = -1,
    @JvmField var volume: Int = 100,
) : Serializable {
    constructor(name: String, duration: Int) : this(
        bell = -2,
        bellcount = 1,
        bellpause = 1,
        name = name,
        duration = duration,
        rank = -1,
        volume = 100,
    )

    fun getDurationString(): String = String.format(Locale.US, "%02d:%02d", duration / 60, duration % 60)
}
