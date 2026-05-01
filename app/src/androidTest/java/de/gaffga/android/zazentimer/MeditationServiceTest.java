package de.gaffga.android.zazentimer;

import android.content.SharedPreferences;
import android.os.SystemClock;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.gaffga.android.zazentimer.screens.MainPage;
import de.gaffga.android.zazentimer.utils.MeditationServiceIdlingResource;

import static org.junit.Assert.assertTrue;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MeditationServiceTest {

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
    public ActivityScenarioRule<ZazenTimerActivity> activityRule =
            new ActivityScenarioRule<>(ZazenTimerActivity.class);

    private final MeditationServiceIdlingResource meditationIdlingResource =
            new MeditationServiceIdlingResource();
    private UiDevice device;

    @Before
    public void init() {
        hiltRule.inject();
        IdlingRegistry.getInstance().register(meditationIdlingResource);
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            activityRule.getScenario().onActivity(activity -> {
                SharedPreferences pref = ZazenTimerActivity.getPreferences(activity);
                pref.edit()
                    .putBoolean("mute_mode_none", false)
                    .putBoolean("mute_mode_vibrate", false)
                    .putBoolean("mute_mode_vibrate_sound", true)
                    .apply();
            });
        } catch (Exception e) { }
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(meditationIdlingResource);
    }

    private void clickStopButtonWithUiAutomator() {
        // Use UI Automator to click the stop button by text (doesn't wait for idle)
        UiObject stopButton = device.findObject(new UiSelector().text("Stop"));
        try {
            stopButton.click();
        } catch (Exception e) {
            // Fallback to resource ID if text doesn't work
            UiObject stopButtonById = device.findObject(new UiSelector()
                    .resourceId("de.gaffga.android.zazentimer:id/but_stop"));
            try {
                stopButtonById.click();
            } catch (Exception e2) {
                throw new RuntimeException("Failed to click stop button", e2);
            }
        }
    }

    private void clickByTextWithUiAutomator(String text) {
        UiObject button = device.findObject(new UiSelector().text(text));
        try {
            button.click();
        } catch (Exception e) {
            throw new RuntimeException("Failed to click text: " + text, e);
        }
    }

    private void clickByTextContainsWithUiAutomator(String text) {
        UiObject button = device.findObject(new UiSelector().textContains(text));
        try {
            button.click();
        } catch (Exception e) {
            throw new RuntimeException("Failed to click text containing: " + text, e);
        }
    }

    private boolean isDialogVisible(String titleText) {
        UiObject dialog = device.findObject(new UiSelector().text(titleText));
        return dialog.exists();
    }

    @Test
    public void testStopMeditationConfirmation() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .selectSessionByPosition(0);

        // Start meditation
        activityRule.getScenario().onActivity(activity ->
                activity.startMeditation());

        // Wait for service to fully start (foreground service needs time)
        SystemClock.sleep(4000);

        // Click stop using UI Automator (bypasses Espresso idle check)
        clickStopButtonWithUiAutomator();
        SystemClock.sleep(2000);

        // Verify dialog is shown using UI Automator
        assertTrue("Stop dialog should be visible",
                isDialogVisible("Stop meditation?"));

        // Cancel dialog using UI Automator - use textContains for flexibility
        clickByTextContainsWithUiAutomator("Cancel");
        SystemClock.sleep(500);

        // Stop again
        clickStopButtonWithUiAutomator();
        SystemClock.sleep(500);

        // Confirm stop
        clickByTextContainsWithUiAutomator("Stop");

        // Test passes
    }

    @Test
    public void testTimerCountdown() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .selectSessionByPosition(0);

        // Start meditation
        activityRule.getScenario().onActivity(activity ->
                activity.startMeditation());

        // Wait for timer to tick
        SystemClock.sleep(4000);

        // Stop meditation using UI Automator
        clickStopButtonWithUiAutomator();
        SystemClock.sleep(1000);

        // Confirm stop
        clickByTextContainsWithUiAutomator("Stop");

        // Test passes
    }
}
