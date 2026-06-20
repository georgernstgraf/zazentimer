package at.priv.graf.zazentimer.screens

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.utils.ScreenRobot

class SessionEditPage {
    private val robot = ScreenRobot()

    fun verifyEditSessionScreen(): SessionEditPage {
        Thread.sleep(1000)
        robot.checkElementIsDisplayed(R.id.text_session_name)
        robot.checkElementIsDisplayed(R.id.section_list)
        return this
    }

    fun setSessionName(name: String): SessionEditPage {
        onView(withId(R.id.text_session_name)).perform(typeText(name))
        return this
    }

    fun setSessionDescription(desc: String): SessionEditPage {
        onView(withId(R.id.text_session_description)).perform(typeText(desc))
        return this
    }

    fun clickAddSection(): SectionEditPage {
        robot.clickToolbarOverflowItem(R.string.menu_add_section)
        Thread.sleep(1500)
        return SectionEditPage()
    }

    fun clickSectionAtPosition(pos: Int): SessionEditPage {
        robot.waitForRecyclerViewToBePopulated(R.id.section_list)
        for (i in 0 until 10) {
            try {
                onView(withId(R.id.section_list))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(pos, click()))
                return this
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        }
        throw AssertionError("Could not click section at position $pos")
    }

    fun verifySectionCount(count: Int): SessionEditPage {
        robot.assertRecyclerViewItemCount(R.id.section_list, count)
        return this
    }

    fun goBack(): MainPage {
        robot.pressBack()
        Thread.sleep(3000)
        return MainPage()
    }
}
