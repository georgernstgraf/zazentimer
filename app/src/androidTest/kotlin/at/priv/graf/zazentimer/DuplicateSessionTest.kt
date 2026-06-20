package at.priv.graf.zazentimer

import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
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
    private lateinit var device: UiDevice

    @Before
    fun setupDatabase() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var activityRef: ZazenTimerActivity? = null
        activityRule.scenario.onActivity { activityRef = it }
        activityRef?.resetDatabaseForTest()
    }

    @Test
    fun duplicateSession_doesNotCrash() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAtPosition(0)
        val copyText = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.menu_copy_session)
        device.findObject(UiSelector().text(copyText)).click()
    }

    @Test
    fun duplicateSession_createsCopyWithPrefix() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAtPosition(0)
        val copyText = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.menu_copy_session)
        device.findObject(UiSelector().text(copyText)).click()

        onIdle()

        onView(withText(containsString("Copy of")))
            .check(matches(isDisplayed()))
    }
}
