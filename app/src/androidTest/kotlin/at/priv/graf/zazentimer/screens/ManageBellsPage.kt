package at.priv.graf.zazentimer.screens

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.utils.ScreenRobot
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class ManageBellsPage {
    private val robot = ScreenRobot()

    init {
        Thread.sleep(1000)
        robot.checkElementIsDisplayed(R.id.import_button)
    }

    fun verifyScreenDisplayed(): ManageBellsPage {
        robot.checkElementIsDisplayed(R.id.import_button)
        return this
    }

    fun verifyEmptyState(): ManageBellsPage {
        for (i in 0 until 20) {
            try {
                robot.checkElementIsDisplayed(R.id.empty_text)
                onView(withId(R.id.empty_text)).check(matches(withText(R.string.no_custom_bells)))
                return this
            } catch (_: Throwable) {
                Thread.sleep(500)
            }
        }
        robot.checkElementIsDisplayed(R.id.empty_text)
        onView(withId(R.id.empty_text)).check(matches(withText(R.string.no_custom_bells)))
        return this
    }

    fun verifyBellListed(name: String): ManageBellsPage {
        for (i in 0 until 20) {
            try {
                onView(withText(name)).check(matches(isDisplayed()))
                return this
            } catch (_: Throwable) {
                Thread.sleep(500)
            }
        }
        onView(withText(name)).check(matches(isDisplayed()))
        return this
    }

    fun verifyBellNotListed(name: String): ManageBellsPage {
        for (i in 0 until 20) {
            val found =
                try {
                    onView(withText(name)).check(matches(isDisplayed()))
                    true
                } catch (_: Throwable) {
                    false
                }
            if (found) {
                throw AssertionError("Bell '$name' was expected to be absent but is displayed")
            }
            Thread.sleep(500)
        }
        return this
    }

    fun clickDeleteForBell(name: String): ManageBellsPage {
        onView(withId(R.id.bell_list))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    withBellName(name),
                    ScreenRobot.clickChildViewWithId(R.id.delete_button),
                ),
            )
        return this
    }

    fun confirmDelete(): ManageBellsPage {
        onView(withText(R.string.action_delete))
            .inRoot(RootMatchers.isDialog())
            .perform(click())
        Thread.sleep(500)
        return this
    }

    fun goBack(): SettingsPage {
        robot.pressBack()
        return SettingsPage()
    }

    private companion object {
        private fun withBellName(name: String): Matcher<View> =
            object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description) {
                    description.appendText("RecyclerView item with bell name: $name")
                }

                override fun matchesSafely(item: View): Boolean {
                    val bellName = item.findViewById<TextView>(R.id.bell_name)
                    return bellName != null && bellName.text.toString() == name
                }
            }
    }
}
