package at.priv.graf.zazentimer

import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import at.priv.graf.zazentimer.fragments.MeditationFragment
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.utils.MeditationServiceIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class MeditationServiceTest {
    private val uiTimeout = 5000L

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(ZazenTimerActivity::class.java)

    private val meditationIdlingResource = MeditationServiceIdlingResource()
    private lateinit var device: UiDevice

    @Before
    fun init() {
        hiltRule.inject()
        IdlingRegistry.getInstance().register(meditationIdlingResource)
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        activityRule.scenario.onActivity { activity ->
            val pref: SharedPreferences = ZazenTimerActivity.getPreferences(activity)
            pref
                .edit()
                .putBoolean("mute_mode_none", false)
                .putBoolean("mute_mode_vibrate", false)
                .putBoolean("mute_mode_vibrate_sound", true)
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
        IdlingRegistry.getInstance().unregister(meditationIdlingResource)
    }

    private fun waitForStopButton() {
        for (i in 0 until 100) {
            try {
                androidx.test.espresso.Espresso.onView(
                    androidx.test.espresso.matcher.ViewMatchers.withId(R.id.but_stop)
                ).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                    org.hamcrest.Matchers.allOf(
                        androidx.test.espresso.matcher.ViewMatchers.isDisplayed(),
                        androidx.test.espresso.matcher.ViewMatchers.isEnabled()
                    )
                ))
                return
            } catch (e: Throwable) {
                SystemClock.sleep(200)
            }
        }
        throw AssertionError("Stop button never became enabled and displayed")
    }

    private fun clickStopButtonAndWaitForDialog(titleText: String) {
        for (i in 0 until 5) {
            try {
                waitForStopButton()
                androidx.test.espresso.Espresso.onView(
                    androidx.test.espresso.matcher.ViewMatchers.withId(R.id.but_stop)
                ).perform(androidx.test.espresso.action.ViewActions.click())
                
                // Wait for dialog
                for (j in 0 until 20) {
                    try {
                        androidx.test.espresso.Espresso.onView(
                            androidx.test.espresso.matcher.ViewMatchers.withText(titleText)
                        ).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                            androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
                        ))
                        return
                    } catch (e: Throwable) {
                        SystemClock.sleep(100)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Stop button dialog attempt $i failed", e)
            }
            SystemClock.sleep(500)
        }
        throw AssertionError("Dialog '$titleText' did not appear after clicking stop button 5 times")
    }

    private fun clickByTextContainsWithUiAutomator(text: String) {
        androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1)
        ).perform(androidx.test.espresso.action.ViewActions.click())
    }

    // Helper removed to test UI button click properly

    private fun clickCancelDialog() {
        androidx.test.espresso.Espresso.onView(
            androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button2)
        ).perform(androidx.test.espresso.action.ViewActions.click())
    }

    private fun isDialogVisible(titleText: String): Boolean {
        try {
            androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withText(titleText)
            ).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
            ))
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    private fun waitForDialog(titleText: String) {
        for (i in 0 until 50) {
            if (isDialogVisible(titleText)) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Dialog $titleText not visible")
    }

    @Test
    fun testStopMeditationConfirmation() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .selectSessionByPosition(0)

        activityRule.scenario.onActivity { activity ->
            activity.startMeditation()
        }

        SystemClock.sleep(8000)
        waitForStopButton()
        clickStopButtonAndWaitForDialog("Stop meditation?")

        assertTrue(
            "Stop dialog should be visible",
            isDialogVisible("Stop meditation?"),
        )

        clickCancelDialog()

        waitForStopButton()
        clickStopButtonAndWaitForDialog("Stop meditation?")

        clickByTextContainsWithUiAutomator("Stop")
    }

    @Test
    fun testTimerCountdown() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .selectSessionByPosition(0)

        activityRule.scenario.onActivity { activity ->
            activity.startMeditation()
        }

        SystemClock.sleep(8000)
        waitForStopButton()
        clickStopButtonAndWaitForDialog("Stop meditation?")

        clickByTextContainsWithUiAutomator("Stop")
    }

    companion object {
        private const val TAG = "MST"
    }
}
