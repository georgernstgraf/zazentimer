package at.priv.graf.zazentimer

import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class DuplicateSessionTest : AbstractZazenTest() {
    @Before
    fun setupDatabase() {
        var activityRef: ZazenTimerActivity? = null
        activityRule.scenario.onActivity { activityRef = it }
        activityRef?.resetDatabaseForTest()
        onIdle()
    }

    @Test
    fun testDuplicateSessionDoesNotCrash() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_copy_session)
    }

    @Test
    fun testDuplicateSessionCreatesCopyWithPrefix() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_copy_session)

        onIdle()

        onView(withText(containsString("Copy of")))
            .check(matches(isDisplayed()))
    }
}
