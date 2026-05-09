package at.priv.graf.zazentimer.service

sealed class MeditationUiState {
    abstract val sessionName: String
    abstract val currentSectionName: String
    abstract val nextSectionName: String
    abstract val currentStartSeconds: Int
    abstract val totalSessionTime: Int
    abstract val nextEndSeconds: Int
    abstract val nextStartSeconds: Int
    abstract val prevStartSeconds: Int
    abstract val sectionElapsedSeconds: Int
    abstract val sessionElapsedSeconds: Int

    data class Idle(
        override val currentStartSeconds: Int = 0,
        override val totalSessionTime: Int = 0,
        override val nextEndSeconds: Int = 0,
        override val nextStartSeconds: Int = 0,
        override val prevStartSeconds: Int = 0,
        override val sectionElapsedSeconds: Int = 0,
        override val sessionElapsedSeconds: Int = 0,
        override val currentSectionName: String = "",
        override val nextSectionName: String = "",
        override val sessionName: String = "",
    ) : MeditationUiState()

    data class Running(
        override val currentStartSeconds: Int,
        override val totalSessionTime: Int,
        override val nextEndSeconds: Int,
        override val nextStartSeconds: Int,
        override val prevStartSeconds: Int,
        override val sectionElapsedSeconds: Int,
        override val sessionElapsedSeconds: Int,
        override val currentSectionName: String,
        override val nextSectionName: String,
        override val sessionName: String,
        val nextNextSectionName: String = "",
    ) : MeditationUiState()

    data class Paused(
        override val currentStartSeconds: Int,
        override val totalSessionTime: Int,
        override val nextEndSeconds: Int,
        override val nextStartSeconds: Int,
        override val prevStartSeconds: Int,
        override val sectionElapsedSeconds: Int,
        override val sessionElapsedSeconds: Int,
        override val currentSectionName: String,
        override val nextSectionName: String,
        override val sessionName: String,
        val nextNextSectionName: String = "",
    ) : MeditationUiState()
}
