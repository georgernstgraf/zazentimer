package at.priv.graf.zazentimer

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class DuplicateSessionTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(ZazenTimerActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
        activityRule.scenario.onActivity(ZazenTimerActivity::resetDatabaseForTest)
    }

    @Test
    fun testDuplicateSessionDoesNotCrash() {
        onView(withId(R.id.recycler_sessions))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.sessionOverflow)))

        onView(withText(R.string.menu_copy_session))
            .perform(click())

        onView(withId(R.id.recycler_sessions))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDuplicateSessionCreatesCopyWithPrefix() {
        onView(withId(R.id.recycler_sessions))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.sessionOverflow)))

        onView(withText(R.string.menu_copy_session))
            .perform(click())

        onView(withText(containsString("Copy of")))
            .check(matches(isDisplayed()))
    }

    companion object {
        private fun clickChildViewWithId(id: Int): ViewAction =
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)

                override fun getDescription(): String = "Click child view with id $id"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val child = view.findViewById<View>(id)
                    child?.performClick()
                }
            }
    }
}
