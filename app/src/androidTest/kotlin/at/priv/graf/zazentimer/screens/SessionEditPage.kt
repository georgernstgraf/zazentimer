package at.priv.graf.zazentimer.screens

import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import at.priv.graf.zazentimer.R

class SessionEditPage {
    private val robot = ScreenRobot()

    fun verifyEditSessionScreen(): SessionEditPage {
        SystemClock.sleep(1000)
        robot.checkElementIsDisplayed(R.id.text_sitzung_name)
        robot.checkElementIsDisplayed(R.id.but_new_section)
        return this
    }

    fun setSessionName(name: String): SessionEditPage {
        onView(withId(R.id.text_sitzung_name)).perform(typeText(name))
        return this
    }

    fun setSessionDescription(desc: String): SessionEditPage {
        onView(withId(R.id.text_sitzung_beschreibung)).perform(typeText(desc))
        return this
    }

    fun clickAddSection(): SectionEditPage {
        robot.clickOnView(R.id.but_new_section)
        SystemClock.sleep(1500)
        return SectionEditPage()
    }

    fun clickSectionAtPosition(pos: Int): SessionEditPage {
        for (i in 0 until 10) {
            try {
                onView(withId(R.id.list))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(pos, click()))
                return this
            } catch (e: Throwable) {
                SystemClock.sleep(500)
            }
        }
        throw AssertionError("Could not click section at position $pos")
    }

    fun verifySectionCount(count: Int): SessionEditPage {
        robot.assertRecyclerViewItemCount(R.id.list, count)
        return this
    }

    fun goBack(): MainPage {
        robot.pressBack()
        SystemClock.sleep(3000)
        return MainPage()
    }
}
