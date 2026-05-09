package at.priv.graf.zazentimer

import android.os.SystemClock
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SessionEditPage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class SessionCrudTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(ZazenTimerActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
        activityRule.scenario.onActivity { activity ->
            activity.resetDatabaseForTest()
            ZazenTimerActivity.getPreferences(activity)
                .edit()
                .putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                .apply()
        }
        SystemClock.sleep(3000)
    }

    @Test
    fun testOpenEditSession() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()

        onView(withId(R.id.text_sitzung_name))
            .check(matches(not(withText(""))))
    }

    @Test
    fun testUpdateSessionMetadata() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()

        onView(withId(R.id.text_sitzung_name))
            .perform(clearText(), typeText("Updated Session Name"))
        onView(withId(R.id.text_sitzung_beschreibung))
            .perform(clearText(), typeText("Updated Description"))

        closeSoftKeyboard()
        SessionEditPage().goBack()

        MainPage().verifySessionNameVisible("Updated Session Name")
    }

    @Test
    fun testDeleteSession() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_delete_session)

        onView(withText(R.string.title_question_delete_session))
            .check(matches(isDisplayed()))
        onView(withText(R.string.ok)).perform(click())

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()
    }

    @Test
    fun testDeleteCancel() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_delete_session)

        onView(withText(R.string.title_question_delete_session))
            .check(matches(isDisplayed()))
        onView(withText(R.string.abbrechen)).perform(click())

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()
    }
}
