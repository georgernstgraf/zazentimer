package at.priv.graf.zazentimer.screens

import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import at.priv.graf.zazentimer.R
import org.hamcrest.Matcher

class MainPage : BasePage() {
    fun verifyMainScreenIsDisplayed(): MainPage {
        checkElementIsDisplayed(R.id.my_toolbar)
        checkElementIsDisplayed(R.id.but_start)
        checkElementIsDisplayed(R.id.recycler_sessions)
        return this
    }

    fun verifyDefaultSessionsAreVisible(): MainPage {
        onView(withId(R.id.recycler_sessions)).check(matches(isDisplayed()))
        onView(withId(R.id.but_start)).check(matches(isDisplayed()))
        return this
    }

    fun selectSessionByPosition(position: Int): MainPage {
        onView(withId(R.id.recycler_sessions))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))
        return this
    }

    fun clickStartMeditation(): MeditationPage {
        clickOnView(R.id.but_start)
        return MeditationPage()
    }

    fun verifySessionSelected(position: Int): MainPage {
        onView(withId(R.id.recycler_sessions))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
        onView(withId(R.id.recycler_sessions))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    object : androidx.test.espresso.ViewAction {
                        override fun getConstraints(): Matcher<View> = isDisplayed()

                        override fun getDescription(): String = "check selected at position $position"

                        override fun perform(
                            uiController: UiController,
                            view: View,
                        ) {
                            if (!view.isSelected && !view.isActivated) {
                                throw AssertionError("View at position $position is not selected/activated")
                            }
                        }
                    },
                ),
            )
        return this
    }

    fun clickSessionOverflowAtPosition(position: Int): MainPage {
        onView(withId(R.id.recycler_sessions))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, clickChildViewWithId(R.id.sessionOverflow)),
            )
        SystemClock.sleep(500)
        return this
    }

    fun clickSessionOverflowAction(
        position: Int,
        textResId: Int,
    ): MainPage {
        clickSessionOverflowAtPosition(position)
        onView(withText(textResId)).perform(click())
        return this
    }

    override fun clickToolbarOverflowItem(textResId: Int): MainPage {
        super.clickToolbarOverflowItem(textResId)
        return this
    }
}
