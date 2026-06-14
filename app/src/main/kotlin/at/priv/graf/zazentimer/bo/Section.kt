package at.priv.graf.zazentimer.bo

import java.io.Serializable

data class Section(
    @JvmField var bellId: Int = 0,
    @JvmField var bellcount: Int = 0,
    @JvmField var bellpause: Int = 0,
    @JvmField var duration: Int = 0,
    @JvmField var fkSession: Int = 0,
    @JvmField var id: Int = 0,
    @JvmField var name: String? = null,
    @JvmField var rank: Int = -1,
) : Serializable {
    constructor(name: String, duration: Int) : this(
        bellcount = 1,
        bellpause = 1,
        name = name,
        duration = duration,
        rank = -1,
    )

    fun getDurationString(): String = TimeFormat.mmss(duration)

    companion object {
        private const val serialVersionUID = 1L
    }
}
