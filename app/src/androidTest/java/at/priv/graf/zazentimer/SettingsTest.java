package at.priv.graf.zazentimer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.SeekBar;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import at.priv.graf.zazentimer.screens.MainPage;
import at.priv.graf.zazentimer.screens.SettingsPage;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SettingsTest {

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
    public ActivityScenarioRule<ZazenTimerActivity> activityRule =
            new ActivityScenarioRule<>(ZazenTimerActivity.class);

    @Before
    public void init() {
        hiltRule.inject();
    }

    @Test
    public void testOpenSettings() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings);
        new SettingsPage();
        new SettingsPage().goBack()
                .verifyMainScreenIsDisplayed();
    }

    @Test
    public void testThemeToggle() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings);
        new SettingsPage();

        onView(withText(R.string.theme)).perform(click());
        onView(withText(R.string.theme_dark)).perform(click());

        new SettingsPage();
    }

    @Test
    public void testMuteSettings() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings);
        new SettingsPage();

        onView(withText(R.string.pref_mute_mode_vibrate_sound))
                .perform(scrollTo(), click());

        SharedPreferences prefs = getPrefs();
        assertTrue(prefs.getBoolean("mute_mode_vibrate_sound", false));
        assertFalse(prefs.getBoolean("mute_mode_none", true));

        onView(withText(R.string.pref_mute_mode_none))
                .perform(scrollTo(), click());

        assertTrue(prefs.getBoolean("mute_mode_none", false));
        assertFalse(prefs.getBoolean("mute_mode_vibrate_sound", true));
    }

    @Test
    public void testBrightnessAdjustment() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings);
        new SettingsPage();

        onView(withText(R.string.checkbox_keep_screen_on))
                .perform(scrollTo(), click());

        onView(withText(R.string.pref_title_brightness))
                .perform(click());

        onView(isAssignableFrom(SeekBar.class))
                .perform(setSeekBarProgress(50));

        Espresso.pressBack();
    }

    @RequiresDisplay
    @Test
    public void testBackup() {
        Intents.init();
        try {
            new MainPage()
                    .verifyMainScreenIsDisplayed()
                    .clickToolbarOverflowItem(R.string.menu_settings);
            new SettingsPage()
                    .clickBackup();

            intended(hasAction(Intent.ACTION_CREATE_DOCUMENT));
        } finally {
            Intents.release();
        }
    }

    @RequiresDisplay
    @Test
    public void testRestore() {
        Intents.init();
        try {
            new MainPage()
                    .verifyMainScreenIsDisplayed()
                    .clickToolbarOverflowItem(R.string.menu_settings);
            new SettingsPage()
                    .clickRestoreAndConfirm();

            intended(hasAction(Intent.ACTION_OPEN_DOCUMENT));
        } finally {
            Intents.release();
        }
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    private static ViewAction setSeekBarProgress(final int progress) {
        return new ViewAction() {
            @Override
            public Matcher<android.view.View> getConstraints() {
                return isAssignableFrom(SeekBar.class);
            }

            @Override
            public String getDescription() {
                return "Set SeekBar progress to " + progress;
            }

            @Override
            public void perform(UiController uiController, android.view.View view) {
                SeekBar seekBar = (SeekBar) view;
                int range = seekBar.getWidth()
                        - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
                float target = (float) progress / seekBar.getMax();
                float xPos = seekBar.getPaddingLeft() + target * range;
                float yPos = seekBar.getHeight() / 2f;

                long downTime = SystemClock.uptimeMillis();
                seekBar.dispatchTouchEvent(MotionEvent.obtain(
                        downTime, downTime, MotionEvent.ACTION_DOWN, xPos, yPos, 0));
                seekBar.dispatchTouchEvent(MotionEvent.obtain(
                        downTime, downTime + 50, MotionEvent.ACTION_MOVE, xPos, yPos, 0));
                seekBar.dispatchTouchEvent(MotionEvent.obtain(
                        downTime, downTime + 100, MotionEvent.ACTION_UP, xPos, yPos, 0));
            }
        };
    }
}
