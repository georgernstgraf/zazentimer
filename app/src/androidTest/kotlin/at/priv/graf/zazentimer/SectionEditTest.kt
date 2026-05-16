package at.priv.graf.zazentimer

import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SectionEditPage
import at.priv.graf.zazentimer.screens.SessionEditPage
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class SectionEditTest : AbstractZazenTest() {
    @Before
    fun setupPreferences() {
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
        onIdle()
    }

    @Test
    fun testAddNewSection() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        val sessionEditPage =
            SessionEditPage()
                .verifyEditSessionScreen()

        val sectionEditPage = sessionEditPage.clickAddSection()

        sectionEditPage.verifySectionEditScreen()

        onView(withId(R.id.time)).check(matches(withText("01:00")))
    }

    @Test
    fun testEditSectionConfig() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()
            .clickSectionAtPosition(0)

        SectionEditPage()
            .verifySectionEditScreen()
            .tapDurationPicker()

        onIdle()

        onView(withId(android.R.id.button1)).perform(click())

        SectionEditPage()
            .setBellCount(3)
            .setBellGap(8)
            .goBack()

        SessionEditPage().verifyEditSessionScreen()
    }

    @Test
    fun testBellSoundPlayback() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()
            .clickSectionAtPosition(0)

        val sectionEditPage =
            SectionEditPage()
                .verifySectionEditScreen()

        sectionEditPage.clickPlayBell()

        sectionEditPage.verifySectionEditScreen()
    }
}
