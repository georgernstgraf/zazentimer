package at.priv.graf.zazentimer.bo

import java.io.Serializable
import java.util.Locale

data class Section(
    @JvmField var bell: Int = DEFAULT_BELL,
    @JvmField var bellUri: String? = null,
    @JvmField var bellcount: Int = 0,
    @JvmField var bellpause: Int = 0,
    @JvmField var duration: Int = 0,
    @JvmField var fkSession: Int = 0,
    @JvmField var id: Int = 0,
    @JvmField var name: String? = null,
    @JvmField var rank: Int = -1,
) : Serializable {
    constructor(name: String, duration: Int) : this(
        bell = DEFAULT_BELL,
        bellcount = 1,
        bellpause = 1,
        name = name,
        duration = duration,
        rank = -1,
    )

    fun getDurationString(): String =
        String.format(
            Locale.US,
            "%02d:%02d",
            duration / SECONDS_PER_MINUTE,
            duration % SECONDS_PER_MINUTE,
        )

    companion object {
        private const val serialVersionUID = 1L

        private const val DEFAULT_BELL = -2

        private const val SECONDS_PER_MINUTE = 60
    }
}
