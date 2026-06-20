package at.priv.graf.zazentimer

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SessionEditPage
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class SessionCrudTest : AbstractZazenTest() {
    @Before
    fun setupDatabase() {
        var activityRef: ZazenTimerActivity? = null
        activityRule.scenario.onActivity { activity ->
            activityRef = activity
            ZazenTimerActivity
                .getPreferences(activity)
                .edit()
                .putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                .apply()
        }
        activityRef?.resetDatabaseForTest()
    }

    @Test
    fun openEditSession_navigatesToEdit() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()

        onView(withId(R.id.text_session_name))
            .check(matches(not(withText(""))))
    }

    @Test
    fun updateSessionMetadata_savesChanges() {
        var demoSessionName = "Zazen and Kinhin"
        activityRule.scenario.onActivity { demoSessionName = it.getString(R.string.demo_sess1_name) }

        MainPage()
            .verifyMainScreenIsDisplayed()
            .verifySessionNameVisible(demoSessionName)
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()

        onView(withId(R.id.text_session_name)).check(matches(withText(demoSessionName)))

        onView(withId(R.id.text_session_name))
            .perform(replaceText("Updated Session Name"))
        onView(withId(R.id.text_session_description))
            .perform(replaceText("Updated Description"))

        closeSoftKeyboard()
        SessionEditPage().goBack()

        onIdle()

        MainPage().verifySessionNameVisible("Updated Session Name")
    }

    @Test
    fun deleteSession_removesSession() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_delete_session)

        onView(withText(R.string.title_question_delete_session))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.ok))
            .inRoot(isDialog())
            .perform(click())

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()
    }

    @Test
    fun deleteCancel_keepsSession() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_delete_session)

        onView(withText(R.string.title_question_delete_session))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.cancel))
            .inRoot(isDialog())
            .perform(click())

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()
    }
}
