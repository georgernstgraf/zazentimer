package at.priv.graf.zazentimer

import android.os.Build
import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.MeditationPage
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainScreenDeadStateTest : AbstractZazenTest() {
    @Before
    fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant at.priv.graf.zazentimer.debug android.permission.POST_NOTIFICATIONS",
            )
        }
        activityRule.scenario.onActivity { activity ->
            ZazenTimerActivity
                .getPreferences(activity)
                .edit()
                .putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                .apply()
            if (runBlocking { activity.dbOperations.readSessions().isEmpty() } ||
                runBlocking {
                    val sessions = activity.dbOperations.readSessions()
                    sessions.isEmpty() || activity.dbOperations.readSections(sessions[0].id).isEmpty()
                }
            ) {
                activity.resetDatabaseForTest()
            }
        }
        SystemClock.sleep(2000)
    }

    @After
    fun tearDown() {
        activityRule.scenario.onActivity { activity ->
            activity.forceStopMeditationForTest()
        }
    }

    @Test
    fun testButStartEnabledAfterStoppingMeditation() {
        var stopText = "Stop"
        activityRule.scenario.onActivity { stopText = it.getString(R.string.stop_meditation_stop) }

        MainPage()
            .verifyMainScreenIsDisplayed()
            .selectSessionByPosition(0)

        onView(withId(R.id.but_start)).perform(click())

        SystemClock.sleep(5000)

        MeditationPage()
            .verifyMeditationScreenIsDisplayed()
            .verifyStopButtonDisplayed()

        onView(withId(R.id.but_stop)).perform(click())

        for (i in 0 until 20) {
            try {
                onView(withText(stopText)).check(matches(isDisplayed()))
                break
            } catch (_: Throwable) {
                SystemClock.sleep(500)
            }
        }
        onView(withText(stopText)).perform(click())

        SystemClock.sleep(2000)

        MainPage().verifyMainScreenIsDisplayed()

        onView(withId(R.id.but_start)).check(matches(isEnabled()))
    }
}
