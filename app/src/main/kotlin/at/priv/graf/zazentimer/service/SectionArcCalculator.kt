package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session

object SectionArcCalculator {
    public fun computeIdleState(
        session: Session,
        sections: Array<Section>,
    ): MeditationUiState.Idle {
        val totalSessionTime = MeditationTimer.getTotalSessionTime(sections)
        val nextEndSeconds = if (sections.size > 1) sections[0].duration + sections[1].duration else totalSessionTime
        val nextStartSeconds = sections[0].duration
        val currentSectionName = sections[0].name ?: ""
        val nextSectionName = if (sections.size > 1) sections[1].name ?: "" else ""
        return MeditationUiState.Idle(
            currentStartSeconds = 0,
            totalSessionTime = totalSessionTime,
            nextEndSeconds = nextEndSeconds,
            nextStartSeconds = nextStartSeconds,
            prevStartSeconds = 0,
            sectionElapsedSeconds = 0,
            sessionElapsedSeconds = 0,
            currentSectionName = currentSectionName,
            nextSectionName = nextSectionName,
            sessionName = session.name ?: "",
        )
    }

    public fun emptyState(sessionName: String = ""): MeditationUiState.Idle =
        MeditationUiState.Idle(sessionName = sessionName)
}
