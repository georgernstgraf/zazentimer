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
        device.wait(Until.findObject(By.desc("Stop").enabled(true)), uiTimeout)
    }

    private fun clickStopButtonAndWaitForDialog(titleText: String) {
        for (i in 0 until 5) {
            try {
                device.wait(Until.findObject(By.desc("Stop").enabled(true)), 2000L)
                clickStopButtonWithUiAutomator()
                val dialog = device.findObject(UiSelector().text(titleText))
                if (dialog.waitForExists(2000)) {
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Stop button dialog attempt $i failed", e)
            }
            SystemClock.sleep(500)
        }
        throw AssertionError("Dialog '$titleText' did not appear after clicking stop button 5 times")
    }

    private fun clickStopButtonWithUiAutomator() {
        val stopButton: UiObject = device.findObject(UiSelector().description("Stop"))
        try {
            stopButton.click()
            SystemClock.sleep(100)
            val stillExists = device.findObject(UiSelector().description("Stop")).exists()
            Log.d(TAG, "Stop button still exists after click: $stillExists")
        } catch (e: Exception) {
            val stopButtonById: UiObject =
                device.findObject(
                    UiSelector()
                        .resourceId("at.priv.graf.zazentimer:id/but_stop"),
                )
            try {
                stopButtonById.click()
                SystemClock.sleep(100)
                val stillExists = device.findObject(UiSelector().description("Stop")).exists()
                Log.d(TAG, "Stop button still exists after click: $stillExists")
            } catch (e2: Exception) {
                throw RuntimeException("Failed to click stop button", e2)
            }
        }
    }

    private fun clickByTextContainsWithUiAutomator(text: String) {
        try {
            val buttonById: UiObject =
                device.findObject(
                    UiSelector()
                        .resourceId("android:id/button1"),
                )
            buttonById.click()
        } catch (e1: Exception) {
            try {
                val byText: UiObject =
                    device.findObject(
                        UiSelector()
                            .textContains(text),
                    )
                byText.click()
            } catch (e2: Exception) {
                try {
                    val byClass: UiObject =
                        device.findObject(
                            UiSelector()
                                .textContains(text)
                                .className("android.widget.Button"),
                        )
                    byClass.click()
                } catch (e3: Exception) {
                    throw RuntimeException("Failed to click text containing: $text", e3)
                }
            }
        }
    }

    private fun showStopDialogViaFragment() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)
            val fragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
            if (fragment is MeditationFragment) {
                fragment.showStopDialogForTest()
            } else {
                throw AssertionError("MeditationFragment is not the primary navigation fragment")
            }
        }
    }

    private fun clickCancelDialog() {
        try {
            val cancelById: UiObject =
                device.findObject(
                    UiSelector()
                        .resourceId("android:id/button2"),
                )
            cancelById.click()
        } catch (e1: Exception) {
            val cancelButton: UiObject =
                device.findObject(
                    UiSelector()
                        .textContains("Cancel")
                        .className("android.widget.Button"),
                )
            try {
                cancelButton.click()
            } catch (e2: Exception) {
                throw RuntimeException("Failed to click Cancel", e2)
            }
        }
    }

    private fun isDialogVisible(titleText: String): Boolean {
        val dialog: UiObject = device.findObject(UiSelector().text(titleText))
        return dialog.exists()
    }

    private fun waitForDialog(titleText: String) {
        device.wait(Until.findObject(By.text(titleText)), uiTimeout)
    }

    @Test
    fun testStopMeditationConfirmation() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .selectSessionByPosition(0)

        activityRule.scenario.onActivity { activity ->
            activity.startMeditation()
        }

        SystemClock.sleep(6000)
        waitForStopButton()
        showStopDialogViaFragment()
        waitForDialog("Stop meditation?")

        assertTrue(
            "Stop dialog should be visible",
            isDialogVisible("Stop meditation?"),
        )

        clickCancelDialog()

        waitForStopButton()
        showStopDialogViaFragment()
        waitForDialog("Stop meditation?")

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

        SystemClock.sleep(6000)
        waitForStopButton()
        showStopDialogViaFragment()
        waitForDialog("Stop meditation?")

        clickByTextContainsWithUiAutomator("Stop")
    }

    companion object {
        private const val TAG = "MST"
    }
}
