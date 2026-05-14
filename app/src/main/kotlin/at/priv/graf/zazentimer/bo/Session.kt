package at.priv.graf.zazentimer.bo

data class Session(
    @JvmField var description: String? = null,
    @JvmField var id: Int = 0,
    @JvmField var name: String? = null,
    @JvmField var bellVolumes: List<SessionBellVolume> = emptyList(),
) {
    constructor(name: String, description: String) : this(
        description = description,
        id = 0,
        name = name,
    )
}
