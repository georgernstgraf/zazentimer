package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session

object SectionArcCalculator {
    fun computeIdleState(
        session: Session,
        sections: Array<Section>,
    ): MeditationUiState {
        val totalSessionTime = MeditationTimer.getTotalSessionTime(sections)
        val currentStartSeconds = 0
        val nextEndSeconds = if (sections.size > 1) sections[0].duration + sections[1].duration else totalSessionTime
        val nextStartSeconds = sections[0].duration
        val prevStartSeconds = 0
        val currentSectionName = sections[0].name ?: ""
        val nextSectionName = if (sections.size > 1) sections[1].name ?: "" else ""
        val nextNextSectionName = if (sections.size > 2) sections[2].name ?: "" else ""
        return MeditationUiState(
            currentStartSeconds,
            totalSessionTime,
            nextEndSeconds,
            nextStartSeconds,
            prevStartSeconds,
            0,
            0,
            currentSectionName,
            nextSectionName,
            nextNextSectionName,
            session.name ?: "",
            false,
            false,
        )
    }

    fun emptyState(sessionName: String = ""): MeditationUiState =
        MeditationUiState(0, 0, 0, 0, 0, 0, 0, "", "", "", sessionName, false, false)
}
