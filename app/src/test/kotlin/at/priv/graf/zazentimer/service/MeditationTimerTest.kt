package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MeditationTimerTest {
    @Test
    fun getTotalSessionTime_emptyArray_returnsZero() {
        val sections = emptyArray<Section>()
        assertThat(MeditationTimer.getTotalSessionTime(sections)).isEqualTo(0)
    }

    @Test
    fun getTotalSessionTime_singleSection_returnsDuration() {
        val sections = arrayOf(Section("A", 300))
        assertThat(MeditationTimer.getTotalSessionTime(sections)).isEqualTo(300)
    }

    @Test
    fun getTotalSessionTime_multipleSections_returnsSum() {
        val sections =
            arrayOf(
                Section("A", 120),
                Section("B", 180),
                Section("C", 60),
            )
        assertThat(MeditationTimer.getTotalSessionTime(sections)).isEqualTo(360)
    }

    @Test
    fun getTotalSessionTime_zeroDurationSection_included() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 0),
                Section("C", 200),
            )
        assertThat(MeditationTimer.getTotalSessionTime(sections)).isEqualTo(300)
    }

    @Test
    fun getCurrentStartSeconds_index0_returnsZero() {
        val sections = arrayOf(Section("A", 300))
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 0)).isEqualTo(0)
    }

    @Test
    fun getCurrentStartSeconds_index1_returnsFirstDuration() {
        val sections =
            arrayOf(
                Section("A", 120),
                Section("B", 180),
            )
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 1)).isEqualTo(120)
    }

    @Test
    fun getCurrentStartSeconds_lastIndex_returnsSumOfAllPrevious() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 2)).isEqualTo(300)
    }

    @Test
    fun getNextEndSeconds_singleSection_returnsTotal() {
        val sections = arrayOf(Section("A", 300))
        assertThat(MeditationTimer.getNextEndSeconds(sections, 0)).isEqualTo(300)
    }

    @Test
    fun getNextEndSeconds_twoSections_atIndex0_returnsSumOfFirstTwo() {
        val sections =
            arrayOf(
                Section("A", 120),
                Section("B", 180),
            )
        assertThat(MeditationTimer.getNextEndSeconds(sections, 0)).isEqualTo(300)
    }

    @Test
    fun getNextEndSeconds_twoSections_atIndex1_returnsTotal() {
        val sections =
            arrayOf(
                Section("A", 120),
                Section("B", 180),
            )
        assertThat(MeditationTimer.getNextEndSeconds(sections, 1)).isEqualTo(300)
    }

    @Test
    fun getNextEndSeconds_threeSections_atIndex0_returnsSumOfFirstTwo() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getNextEndSeconds(sections, 0)).isEqualTo(300)
    }

    @Test
    fun getNextEndSeconds_threeSections_atIndex1_returnsSumOfFirstThree() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getNextEndSeconds(sections, 1)).isEqualTo(600)
    }

    @Test
    fun getNextEndSeconds_threeSections_atLastIndex_returnsTotal() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getNextEndSeconds(sections, 2)).isEqualTo(600)
    }

    @Test
    fun getNextStartSeconds_index0_returnsFirstDuration() {
        val sections =
            arrayOf(
                Section("A", 120),
                Section("B", 180),
            )
        assertThat(MeditationTimer.getNextStartSeconds(sections, 0)).isEqualTo(120)
    }

    @Test
    fun getNextStartSeconds_index1_returnsSumOfFirstTwo() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getNextStartSeconds(sections, 1)).isEqualTo(300)
    }

    @Test
    fun getNextStartSeconds_lastIndex_returnsTotalMinusLastDuration() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getNextStartSeconds(sections, 2)).isEqualTo(600)
    }

    @Test
    fun getPrevStartSeconds_index0_returnsZero() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
            )
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 0)).isEqualTo(0)
    }

    @Test
    fun getPrevStartSeconds_index1_returnsZero() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 1)).isEqualTo(0)
    }

    @Test
    fun getPrevStartSeconds_index2_returnsFirstDuration() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
            )
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 2)).isEqualTo(100)
    }

    @Test
    fun getPrevStartSeconds_index3_returnsSumOfFirstTwo() {
        val sections =
            arrayOf(
                Section("A", 100),
                Section("B", 200),
                Section("C", 300),
                Section("D", 400),
            )
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 3)).isEqualTo(300)
    }

    @Test
    fun getSectionElapsedSeconds_elapsedLessThanDuration_returnsElapsed() {
        assertThat(MeditationTimer.getSectionElapsedSeconds(50, 300)).isEqualTo(50)
    }

    @Test
    fun getSectionElapsedSeconds_elapsedEqualsDuration_returnsDuration() {
        assertThat(MeditationTimer.getSectionElapsedSeconds(300, 300)).isEqualTo(300)
    }

    @Test
    fun getSectionElapsedSeconds_elapsedExceedsDuration_returnsDuration() {
        assertThat(MeditationTimer.getSectionElapsedSeconds(500, 300)).isEqualTo(300)
    }

    @Test
    fun getSectionElapsedSeconds_zeroElapsed_returnsZero() {
        assertThat(MeditationTimer.getSectionElapsedSeconds(0, 300)).isEqualTo(0)
    }

    @Test
    fun getSectionElapsedSeconds_zeroDuration_returnsZero() {
        assertThat(MeditationTimer.getSectionElapsedSeconds(50, 0)).isEqualTo(0)
    }

    @Test
    fun getCurrentSessionTime_sumsStartAndElapsed() {
        assertThat(MeditationTimer.getCurrentSessionTime(120, 45)).isEqualTo(165)
    }

    @Test
    fun getCurrentSessionTime_zeroValues_returnsZero() {
        assertThat(MeditationTimer.getCurrentSessionTime(0, 0)).isEqualTo(0)
    }

    @Test
    fun largeDurations_allCalculationsCorrect() {
        val sections =
            arrayOf(
                Section("A", Int.MAX_VALUE / 3),
                Section("B", Int.MAX_VALUE / 3),
                Section("C", Int.MAX_VALUE / 3),
            )
        val total = MeditationTimer.getTotalSessionTime(sections)
        val currentStart = MeditationTimer.getCurrentStartSeconds(sections, 1)
        assertThat(currentStart).isEqualTo(Int.MAX_VALUE / 3)
        assertThat(total).isEqualTo((Int.MAX_VALUE / 3) * 3)
    }

    @Test
    fun fourSections_boundariesAtEachIndex() {
        val sections =
            arrayOf(
                Section("A", 60),
                Section("B", 120),
                Section("C", 180),
                Section("D", 240),
            )
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 0)).isEqualTo(0)
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 1)).isEqualTo(60)
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 2)).isEqualTo(180)
        assertThat(MeditationTimer.getCurrentStartSeconds(sections, 3)).isEqualTo(360)

        assertThat(MeditationTimer.getNextEndSeconds(sections, 0)).isEqualTo(180)
        assertThat(MeditationTimer.getNextEndSeconds(sections, 1)).isEqualTo(360)
        assertThat(MeditationTimer.getNextEndSeconds(sections, 2)).isEqualTo(600)
        assertThat(MeditationTimer.getNextEndSeconds(sections, 3)).isEqualTo(600)

        assertThat(MeditationTimer.getNextStartSeconds(sections, 0)).isEqualTo(60)
        assertThat(MeditationTimer.getNextStartSeconds(sections, 1)).isEqualTo(180)
        assertThat(MeditationTimer.getNextStartSeconds(sections, 2)).isEqualTo(360)
        assertThat(MeditationTimer.getNextStartSeconds(sections, 3)).isEqualTo(600)

        assertThat(MeditationTimer.getPrevStartSeconds(sections, 0)).isEqualTo(0)
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 1)).isEqualTo(0)
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 2)).isEqualTo(60)
        assertThat(MeditationTimer.getPrevStartSeconds(sections, 3)).isEqualTo(180)
    }
}
