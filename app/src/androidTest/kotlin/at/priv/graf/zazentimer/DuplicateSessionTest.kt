package at.priv.graf.zazentimer

import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.ScreenRobot
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
        SystemClock.sleep(2000)
    }

    @Test
    fun testDuplicateSessionDoesNotCrash() {
        onView(withId(R.id.recycler_sessions))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ScreenRobot.clickChildViewWithId(R.id.sessionOverflow)))

        onView(withText(R.string.menu_copy_session))
            .perform(click())

        onView(withId(R.id.recycler_sessions))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDuplicateSessionCreatesCopyWithPrefix() {
        onView(withId(R.id.recycler_sessions))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ScreenRobot.clickChildViewWithId(R.id.sessionOverflow)))

        onView(withText(R.string.menu_copy_session))
            .perform(click())

        SystemClock.sleep(1000)

        onView(withText(containsString("Copy of")))
            .check(matches(isDisplayed()))
    }
}
