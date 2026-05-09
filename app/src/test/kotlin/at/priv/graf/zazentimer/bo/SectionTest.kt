package at.priv.graf.zazentimer.bo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SectionTest {
    @Test
    fun getDurationString_zeroSeconds_returns00_00() {
        val section = Section(duration = 0)
        assertThat(section.getDurationString()).isEqualTo("00:00")
    }

    @Test
    fun getDurationString_oneSecond_returns00_01() {
        val section = Section(duration = 1)
        assertThat(section.getDurationString()).isEqualTo("00:01")
    }

    @Test
    fun getDurationString_59seconds_returns00_59() {
        val section = Section(duration = 59)
        assertThat(section.getDurationString()).isEqualTo("00:59")
    }

    @Test
    fun getDurationString_60seconds_returns01_00() {
        val section = Section(duration = 60)
        assertThat(section.getDurationString()).isEqualTo("01:00")
    }

    @Test
    fun getDurationString_61seconds_returns01_01() {
        val section = Section(duration = 61)
        assertThat(section.getDurationString()).isEqualTo("01:01")
    }

    @Test
    fun getDurationString_3599seconds_returns59_59() {
        val section = Section(duration = 3599)
        assertThat(section.getDurationString()).isEqualTo("59:59")
    }

    @Test
    fun getDurationString_3600seconds_returns60_00() {
        val section = Section(duration = 3600)
        assertThat(section.getDurationString()).isEqualTo("60:00")
    }

    @Test
    fun getDurationString_7200seconds_returns120_00() {
        val section = Section(duration = 7200)
        assertThat(section.getDurationString()).isEqualTo("120:00")
    }
}
