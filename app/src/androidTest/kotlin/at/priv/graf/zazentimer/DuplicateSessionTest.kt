package at.priv.graf.zazentimer

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
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
        SystemClock.sleep(2000)
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

        SystemClock.sleep(1000)

        onView(withText(containsString("Copy of")))
            .check(matches(isDisplayed()))
    }
}
