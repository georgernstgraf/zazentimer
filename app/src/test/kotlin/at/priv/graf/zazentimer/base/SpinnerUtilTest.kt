package at.priv.graf.zazentimer.base

import at.priv.graf.zazentimer.bo.Session
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpinnerUtilTest {
    @Test
    fun getPositionById_foundInList_returnsCorrectPosition() {
        val list =
            arrayListOf(
                Session(id = 10, name = "A"),
                Session(id = 20, name = "B"),
                Session(id = 30, name = "C"),
            )
        assertThat(SpinnerUtil.getPositionById(list, 20)).isEqualTo(1)
    }

    @Test
    fun getPositionById_notFound_returnsMinusOne() {
        val list =
            arrayListOf(
                Session(id = 10, name = "A"),
                Session(id = 20, name = "B"),
            )
        assertThat(SpinnerUtil.getPositionById(list, 99)).isEqualTo(-1)
    }

    @Test
    fun getPositionById_emptyList_returnsMinusOne() {
        val list = arrayListOf<Session>()
        assertThat(SpinnerUtil.getPositionById(list, 1)).isEqualTo(-1)
    }

    @Test
    fun getPositionById_firstElement_returnsZero() {
        val list =
            arrayListOf(
                Session(id = 10, name = "A"),
                Session(id = 20, name = "B"),
            )
        assertThat(SpinnerUtil.getPositionById(list, 10)).isEqualTo(0)
    }

    @Test
    fun getPositionById_lastElement_returnsSizeMinusOne() {
        val list =
            arrayListOf(
                Session(id = 10, name = "A"),
                Session(id = 20, name = "B"),
                Session(id = 30, name = "C"),
            )
        assertThat(SpinnerUtil.getPositionById(list, 30)).isEqualTo(2)
    }

    @Test
    fun getPositionById_multipleMatches_returnsFirstMatch() {
        val list =
            arrayListOf(
                Session(id = 10, name = "A"),
                Session(id = 20, name = "B"),
                Session(id = 10, name = "C"),
            )
        assertThat(SpinnerUtil.getPositionById(list, 10)).isEqualTo(0)
    }
}
