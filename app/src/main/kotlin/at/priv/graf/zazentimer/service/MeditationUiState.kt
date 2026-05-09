package at.priv.graf.zazentimer.service

data class MeditationUiState(
    @JvmField val currentStartSeconds: Int,
    @JvmField val totalSessionTime: Int,
    @JvmField val nextEndSeconds: Int,
    @JvmField val nextStartSeconds: Int,
    @JvmField val prevStartSeconds: Int,
    @JvmField val sectionElapsedSeconds: Int,
    @JvmField val sessionElapsedSeconds: Int,
    @JvmField val currentSectionName: String,
    @JvmField val nextSectionName: String,
    @JvmField val nextNextSectionName: String,
    @JvmField val sessionName: String,
    @JvmField val paused: Boolean,
    @JvmField val running: Boolean,
)
