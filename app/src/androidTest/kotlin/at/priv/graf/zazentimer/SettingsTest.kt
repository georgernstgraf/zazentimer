package at.priv.graf.zazentimer

import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SettingsPage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(ZazenTimerActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testOpenSettings() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()
        SettingsPage()
            .goBack()
            .verifyMainScreenIsDisplayed()
    }

    @Test
    fun testThemeToggle() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()

        onView(withText(R.string.theme)).perform(click())
        onView(withText(R.string.theme_dark)).perform(click())

        Espresso.onIdle()

        SettingsPage()
    }

    @Test
    fun testMuteSettings() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()

        onView(withText(R.string.pref_mute_mode_vibrate_sound))
            .perform(scrollTo(), click())

        val prefs = getPrefs()
        assertTrue(prefs.getBoolean("mute_mode_vibrate_sound", false))
        assertFalse(prefs.getBoolean("mute_mode_none", true))

        onView(withText(R.string.pref_mute_mode_none))
            .perform(scrollTo(), click())

        assertTrue(prefs.getBoolean("mute_mode_none", false))
        assertFalse(prefs.getBoolean("mute_mode_vibrate_sound", true))
    }

    @Test
    fun testBrightnessAdjustment() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()

        onView(withText(R.string.checkbox_keep_screen_on))
            .perform(scrollTo(), click())

        onView(withText(R.string.pref_title_brightness))
            .perform(click())

        Espresso.onIdle()

        onView(isAssignableFrom(SeekBar::class.java))
            .perform(setSeekBarProgress(50))

        Espresso.onIdle()
        Espresso.pressBack()
    }

    @Test
    fun testBackup() {
        Intents.init()
        try {
            MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings)
            SettingsPage()
                .clickBackup()

            intended(hasAction(Intent.ACTION_CREATE_DOCUMENT))
        } finally {
            Intents.release()
        }
    }

    @Test
    fun testRestore() {
        Intents.init()
        try {
            MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings)
            SettingsPage()
                .clickRestoreAndConfirm()

            intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        } finally {
            Intents.release()
        }
    }

    private fun getPrefs(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)

    companion object {
        private fun setSeekBarProgress(progress: Int): ViewAction =
            object : ViewAction {
                override fun getConstraints(): Matcher<android.view.View> = isAssignableFrom(SeekBar::class.java)

                override fun getDescription(): String = "Set SeekBar progress to $progress"

                override fun perform(
                    uiController: UiController,
                    view: android.view.View,
                ) {
                    val seekBar = view as SeekBar
                    val range = seekBar.width - seekBar.paddingLeft - seekBar.paddingRight
                    val target = progress.toFloat() / seekBar.max
                    val xPos = seekBar.paddingLeft + target * range
                    val yPos = seekBar.height / 2f

                    val downTime = SystemClock.uptimeMillis()
                    seekBar.dispatchTouchEvent(
                        MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, xPos, yPos, 0),
                    )
                    seekBar.dispatchTouchEvent(
                        MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_MOVE, xPos, yPos, 0),
                    )
                    seekBar.dispatchTouchEvent(
                        MotionEvent.obtain(downTime, downTime + 100, MotionEvent.ACTION_UP, xPos, yPos, 0),
                    )
                }
            }
    }
}
