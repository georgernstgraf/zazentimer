package at.priv.graf.zazentimer

import android.os.SystemClock
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.MeditationPage
import at.priv.graf.zazentimer.screens.SessionEditPage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainScreenNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(ZazenTimerActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
        var activityRef: ZazenTimerActivity? = null
        activityRule.scenario.onActivity { activity ->
            activityRef = activity
            ZazenTimerActivity.getPreferences(activity)
                .edit()
                .putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                .apply()
        }
        activityRef?.resetDatabaseForTest()
        SystemClock.sleep(2000)
    }

    @Test
    fun testSessionSelectionHighlight() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .selectSessionByPosition(0)
            .verifySessionSelected(0)
            .selectSessionByPosition(1)
            .verifySessionSelected(1)
    }

    @Test
    fun testAddSessionViaMenu() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_add_session)

        SessionEditPage()
            .verifyEditSessionScreen()
    }

    @Test
    fun testBackArrowFromMeditation() {
        activityRule.scenario.onActivity { activity ->
            activity.showMeditationScreen()
        }

        val meditationPage =
            MeditationPage()
                .verifyMeditationScreenIsDisplayed()
                .verifyPauseButtonDisplayed()
                .verifyStopButtonDisplayed()

        meditationPage
            .goBack()
            .verifyMainScreenIsDisplayed()
    }

    @Test
    fun testBackArrowFromEdit() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()
            .goBack()
            .verifyMainScreenIsDisplayed()
    }

    @Test
    fun testScreenRotation() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .selectSessionByPosition(0)
            .verifySessionSelected(0)

        activityRule.scenario.recreate()

        MainPage()
            .verifyMainScreenIsDisplayed()
            .verifySessionSelected(0)
    }
}
