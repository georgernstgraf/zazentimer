package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import kotlin.math.min

object MeditationTimer {

    fun getCurrentStartSeconds(sections: Array<Section>, currentSectionIndex: Int): Int {
        var total = 0
        for (i in 0 until currentSectionIndex) {
            total += sections[i].duration
        }
        return total
    }

    fun getNextEndSeconds(sections: Array<Section>, currentSectionIndex: Int): Int {
        var total = 0
        var i = 0
        while (i <= currentSectionIndex + 1 && i < sections.size) {
            total += sections[i].duration
            i++
        }
        return total
    }

    fun getNextStartSeconds(sections: Array<Section>, currentSectionIndex: Int): Int {
        var total = 0
        for (i in 0..currentSectionIndex) {
            total += sections[i].duration
        }
        return total
    }

    fun getPrevStartSeconds(sections: Array<Section>, currentSectionIndex: Int): Int {
        var total = 0
        for (i in 0..currentSectionIndex - 2) {
            total += sections[i].duration
        }
        return total
    }

    fun getTotalSessionTime(sections: Array<Section>): Int {
        var total = 0
        for (section in sections) {
            total += section.duration
        }
        return total
    }

    fun getSectionElapsedSeconds(elapsedSeconds: Int, sectionDuration: Int): Int {
        return min(elapsedSeconds, sectionDuration)
    }

    fun getCurrentSessionTime(currentStartSeconds: Int, sectionElapsedSeconds: Int): Int {
        return currentStartSeconds + sectionElapsedSeconds
    }
}
