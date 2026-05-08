package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SectionArcCalculatorTest {

    @Test
    fun emptyState_default_hasAllZerosAndEmpty() {
        val state = SectionArcCalculator.emptyState()
        assertThat(state.currentStartSeconds).isEqualTo(0)
        assertThat(state.totalSessionTime).isEqualTo(0)
        assertThat(state.nextEndSeconds).isEqualTo(0)
        assertThat(state.nextStartSeconds).isEqualTo(0)
        assertThat(state.prevStartSeconds).isEqualTo(0)
        assertThat(state.sectionElapsedSeconds).isEqualTo(0)
        assertThat(state.sessionElapsedSeconds).isEqualTo(0)
        assertThat(state.currentSectionName).isEmpty()
        assertThat(state.nextSectionName).isEmpty()
        assertThat(state.nextNextSectionName).isEmpty()
        assertThat(state.sessionName).isEmpty()
        assertThat(state.paused).isFalse()
        assertThat(state.running).isFalse()
    }

    @Test
    fun emptyState_withSessionName_preservesName() {
        val state = SectionArcCalculator.emptyState("My Session")
        assertThat(state.sessionName).isEqualTo("My Session")
    }

    @Test
    fun computeIdleState_singleSection_correctBoundaries() {
        val session = Session("Test", "desc")
        val sections = arrayOf(Section("Zazen", 600))

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.currentStartSeconds).isEqualTo(0)
        assertThat(state.totalSessionTime).isEqualTo(600)
        assertThat(state.nextEndSeconds).isEqualTo(600)
        assertThat(state.nextStartSeconds).isEqualTo(600)
        assertThat(state.prevStartSeconds).isEqualTo(0)
        assertThat(state.sectionElapsedSeconds).isEqualTo(0)
        assertThat(state.sessionElapsedSeconds).isEqualTo(0)
        assertThat(state.currentSectionName).isEqualTo("Zazen")
        assertThat(state.nextSectionName).isEmpty()
        assertThat(state.nextNextSectionName).isEmpty()
        assertThat(state.sessionName).isEqualTo("Test")
        assertThat(state.paused).isFalse()
        assertThat(state.running).isFalse()
    }

    @Test
    fun computeIdleState_twoSections_correctBoundaries() {
        val session = Session("Session", "")
        val sections = arrayOf(
            Section("Zazen", 600),
            Section("Kinhin", 300)
        )

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.currentStartSeconds).isEqualTo(0)
        assertThat(state.totalSessionTime).isEqualTo(900)
        assertThat(state.nextEndSeconds).isEqualTo(900)
        assertThat(state.nextStartSeconds).isEqualTo(600)
        assertThat(state.prevStartSeconds).isEqualTo(0)
        assertThat(state.currentSectionName).isEqualTo("Zazen")
        assertThat(state.nextSectionName).isEqualTo("Kinhin")
        assertThat(state.nextNextSectionName).isEmpty()
    }

    @Test
    fun computeIdleState_threeSections_correctBoundaries() {
        val session = Session("Three Part", "desc")
        val sections = arrayOf(
            Section("Zazen", 600),
            Section("Kinhin", 300),
            Section("Zazen2", 600)
        )

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.currentStartSeconds).isEqualTo(0)
        assertThat(state.totalSessionTime).isEqualTo(1500)
        assertThat(state.nextEndSeconds).isEqualTo(900)
        assertThat(state.nextStartSeconds).isEqualTo(600)
        assertThat(state.prevStartSeconds).isEqualTo(0)
        assertThat(state.currentSectionName).isEqualTo("Zazen")
        assertThat(state.nextSectionName).isEqualTo("Kinhin")
        assertThat(state.nextNextSectionName).isEqualTo("Zazen2")
    }

    @Test
    fun computeIdleState_fourSections_nextNextOnlyShowsThird() {
        val session = Session("Four Part", "")
        val sections = arrayOf(
            Section("A", 100),
            Section("B", 200),
            Section("C", 300),
            Section("D", 400)
        )

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.nextNextSectionName).isEqualTo("C")
    }

    @Test
    fun computeIdleState_zeroDurationSection_totalIncludesZero() {
        val session = Session("Zero", "")
        val sections = arrayOf(
            Section("Empty", 0),
            Section("Full", 300)
        )

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.totalSessionTime).isEqualTo(300)
        assertThat(state.nextEndSeconds).isEqualTo(300)
        assertThat(state.nextStartSeconds).isEqualTo(0)
    }

    @Test
    fun computeIdleState_nullSectionName_treatedAsEmpty() {
        val session = Session(name = null, description = null)
        val sections = arrayOf(Section(name = null, duration = 100))

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.currentSectionName).isEmpty()
        assertThat(state.sessionName).isEmpty()
    }

    @Test
    fun computeIdleState_nullSessionName_treatedAsEmpty() {
        val session = Session(name = null, description = null)
        val sections = arrayOf(Section("A", 100))

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.sessionName).isEmpty()
    }

    @Test
    fun computeIdleState_notRunningAndNotPaused() {
        val session = Session("S", "")
        val sections = arrayOf(Section("A", 100))

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.running).isFalse()
        assertThat(state.paused).isFalse()
    }

    @Test
    fun computeIdleState_elapsedSecondsAlwaysZero() {
        val session = Session("S", "")
        val sections = arrayOf(Section("A", 100))

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.sectionElapsedSeconds).isEqualTo(0)
        assertThat(state.sessionElapsedSeconds).isEqualTo(0)
    }

    @Test
    fun computeIdleState_varyingDurations_totalIsSum() {
        val session = Session("S", "")
        val sections = arrayOf(
            Section("A", 60),
            Section("B", 120),
            Section("C", 180),
            Section("D", 240)
        )

        val state = SectionArcCalculator.computeIdleState(session, sections)

        assertThat(state.totalSessionTime).isEqualTo(600)
        assertThat(state.nextEndSeconds).isEqualTo(180)
        assertThat(state.nextStartSeconds).isEqualTo(60)
    }
}
