package at.priv.graf.zazentimer.screens

import android.os.SystemClock
import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import at.priv.graf.zazentimer.R
import org.hamcrest.Matcher

class SectionEditPage {
    private val robot = ScreenRobot()

    fun verifySectionEditScreen(): SectionEditPage {
        SystemClock.sleep(1000)
        robot.checkElementIsDisplayed(R.id.play_gong)
        robot.checkElementIsDisplayed(R.id.sectionGongVolume)
        return this
    }

    fun setSectionName(name: String): SectionEditPage {
        onView(withId(R.id.section_name)).perform(replaceText(name))
        return this
    }

    fun tapDurationPicker(): SectionEditPage {
        robot.clickOnView(R.id.duration)
        return this
    }

    fun setBellCount(count: Int): SectionEditPage {
        val id =
            when (count) {
                1 -> R.id.bellcount1
                2 -> R.id.bellcount2
                3 -> R.id.bellcount3
                4 -> R.id.bellcount4
                5 -> R.id.bellcount5
                else -> R.id.bellcount1
            }
        robot.clickOnView(id)
        return this
    }

    fun setBellGap(gap: Int): SectionEditPage {
        val id =
            when (gap) {
                1 -> R.id.gap1
                2 -> R.id.gap2
                3 -> R.id.gap3
                4 -> R.id.gap4
                5 -> R.id.gap5
                6 -> R.id.gap6
                7 -> R.id.gap7
                8 -> R.id.gap8
                9 -> R.id.gap9
                10 -> R.id.gap10
                11 -> R.id.gap11
                12 -> R.id.gap12
                13 -> R.id.gap13
                14 -> R.id.gap14
                15 -> R.id.gap15
                else -> R.id.gap1
            }
        onView(withId(id)).perform(ViewActions.scrollTo())
        robot.clickOnView(id)
        return this
    }

    fun setVolume(level: Int): SectionEditPage {
        onView(withId(R.id.sectionGongVolume)).perform(
            object : androidx.test.espresso.ViewAction {
                override fun getConstraints(): Matcher<View> = isDisplayed()

                override fun getDescription(): String = "set SeekBar progress to $level"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    if (view is SeekBar) {
                        view.progress = level
                    }
                }
            },
        )
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
