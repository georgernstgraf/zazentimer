package at.priv.graf.zazentimer

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.utils.MeditationServiceIdlingResource
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class MeditationServiceTest : AbstractZazenTest() {
    private val uiTimeout = 5000L
    private val meditationIdlingResource = MeditationServiceIdlingResource()
    private lateinit var device: UiDevice

    @Before
    fun setupMeditation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant at.priv.graf.zazentimer.debug android.permission.POST_NOTIFICATIONS",
            )
        }
        IdlingRegistry.getInstance().register(meditationIdlingResource)
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        activityRule.scenario.onActivity { activity ->
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
        val stopButtonById = By.res("at.priv.graf.zazentimer", "but_stop").enabled(true)
        val stopButtonByDesc = By.desc("Stop").enabled(true)
        for (i in 0 until 50) {
            if (device.hasObject(stopButtonById) || device.hasObject(stopButtonByDesc)) {
                return
            }
            SystemClock.sleep(200)
        }
        throw AssertionError("Stop button never became enabled and displayed")
    }

    private fun clickStopButtonAndWaitForDialog(titleText: String) {
        for (i in 0 until 5) {
            try {
                waitForStopButton()
                val stopBtn =
                    device.findObject(
                        UiSelector()
                            .resourceId("at.priv.graf.zazentimer:id/but_stop"),
                    )
                if (stopBtn.exists()) {
                    stopBtn.click()
                } else {
                    val stopDesc =
                        device.findObject(
                            UiSelector()
                                .description("Stop"),
                        )
                    if (stopDesc.exists()) stopDesc.click()
                }

                for (j in 0 until 20) {
                    if (isDialogVisible(titleText)) return
                    SystemClock.sleep(100)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Stop button dialog attempt $i failed", e)
            }
            SystemClock.sleep(500)
        }
        throw AssertionError("Dialog '$titleText' did not appear after clicking stop button 5 times")
    }

    private fun clickStopConfirmButton() {
        androidx.test.espresso.Espresso
            .onView(
                androidx.test.espresso.matcher.ViewMatchers
                    .withId(android.R.id.button1),
            ).perform(
                click(),
            )
    }

    private fun clickCancelDialog() {
        val cancelBtn =
            device.findObject(
                UiSelector()
                    .resourceId("android:id/button2"),
            )
        if (cancelBtn.exists()) {
            cancelBtn.click()
        } else {
            val cancelText =
                device.findObject(
                    UiSelector()
                        .textMatches("(?i)cancel|abbrechen"),
                )
            if (cancelText.exists()) cancelText.click()
        }
    }

    private fun isDialogVisible(titleText: String): Boolean = device.hasObject(By.text(titleText))

    @Test
    fun testStopMeditationConfirmation() {
        var demoSessionName = "Zazen and Kinhin"
        activityRule.scenario.onActivity { demoSessionName = it.getString(R.string.demo_sess1_name) }

        MainPage()
            .verifyMainScreenIsDisplayed()
            .verifySessionNameVisible(demoSessionName)
            .selectSessionByPosition(0)

        activityRule.scenario.onActivity { activity ->
            activity.startMeditation()
        }

        SystemClock.sleep(8000)
        waitForStopButton()
        clickStopButtonAndWaitForDialog("Stop meditation?")

        assert(
            isDialogVisible("Stop meditation?"),
        )

        clickCancelDialog()

        waitForStopButton()
        clickStopButtonAndWaitForDialog("Stop meditation?")

        clickStopConfirmButton()
    }

    @Test
    fun testTimerCountdown() {
        var demoSessionName = "Zazen and Kinhin"
        activityRule.scenario.onActivity { demoSessionName = it.getString(R.string.demo_sess1_name) }

        MainPage()
            .verifyMainScreenIsDisplayed()
            .verifySessionNameVisible(demoSessionName)
            .selectSessionByPosition(0)

        activityRule.scenario.onActivity { activity ->
            activity.startMeditation()
        }

        SystemClock.sleep(8000)
        waitForStopButton()
        clickStopButtonAndWaitForDialog("Stop meditation?")

        clickStopConfirmButton()
    }

    companion object {
        private const val TAG = "MST"
    }
}
