package at.priv.graf.zazentimer.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.utils.ScreenRobot

class SectionEditPage {
    private val robot = ScreenRobot()

    fun verifySectionEditScreen(): SectionEditPage {
        Thread.sleep(1000)
        robot.checkElementIsDisplayed(R.id.play_gong)
        return this
    }

    fun setSectionName(name: String): SectionEditPage {
        onView(withId(R.id.section_name)).perform(replaceText(name))
        return this
    }

    fun tapDurationPicker(): SectionEditPage {
        robot.clickOnView(R.id.section_duration)
        return this
    }

    fun setBellCount(count: Int): SectionEditPage {
        val id =
            when (count) {
                1 -> R.id.bell_count_1
                2 -> R.id.bell_count_2
                3 -> R.id.bell_count_3
                4 -> R.id.bell_count_4
                5 -> R.id.bell_count_5
                else -> R.id.bell_count_1
            }
        robot.clickOnView(id)
        return this
    }

    fun setBellGap(gap: Int): SectionEditPage {
        val id =
            when (gap) {
                1 -> R.id.bell_gap_1
                2 -> R.id.bell_gap_2
                3 -> R.id.bell_gap_3
                4 -> R.id.bell_gap_4
                5 -> R.id.bell_gap_5
                6 -> R.id.bell_gap_6
                7 -> R.id.bell_gap_7
                8 -> R.id.bell_gap_8
                9 -> R.id.bell_gap_9
                10 -> R.id.bell_gap_10
                11 -> R.id.bell_gap_11
                12 -> R.id.bell_gap_12
                13 -> R.id.bell_gap_13
                14 -> R.id.bell_gap_14
                15 -> R.id.bell_gap_15
                else -> R.id.bell_gap_1
            }
        onView(withId(id)).perform(ViewActions.scrollTo())
        robot.clickOnView(id)
        return this
    }

    fun clickPlayBell(): SectionEditPage {
        robot.clickOnView(R.id.play_gong)
        return this
    }

    fun goBack(): SessionEditPage {
        robot.pressBack()
        return SessionEditPage()
    }
}
