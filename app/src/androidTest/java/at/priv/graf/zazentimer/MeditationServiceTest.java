package at.priv.graf.zazentimer;

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

import at.priv.graf.zazentimer.screens.MainPage;
import at.priv.graf.zazentimer.utils.MeditationServiceIdlingResource;

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
        activityRule.getScenario().onActivity(activity -> {
            SharedPreferences pref = ZazenTimerActivity.getPreferences(activity);
            pref.edit()
                .putBoolean("mute_mode_none", false)
                .putBoolean("mute_mode_vibrate", false)
                .putBoolean("mute_mode_vibrate_sound", true)
                .apply();
            if (activity.dbOperations.readSessions().length == 0
                    || activity.dbOperations.readSections(
                        activity.dbOperations.readSessions()[0].id).length == 0) {
                activity.resetDatabaseForTest();
            }
        });
    }

    @After
    public void tearDown() {
        activityRule.getScenario().onActivity(activity ->
                activity.forceStopMeditationForTest());
        IdlingRegistry.getInstance().unregister(meditationIdlingResource);
    }

    private void clickStopButtonWithUiAutomator() {
        UiObject stopButton = device.findObject(new UiSelector().text("Stop"));
        try {
            stopButton.click();
        } catch (Exception e) {
            UiObject stopButtonById = device.findObject(new UiSelector()
                    .resourceId("at.priv.graf.zazentimer:id/but_stop"));
            try {
                stopButtonById.click();
            } catch (Exception e2) {
                throw new RuntimeException("Failed to click stop button", e2);
            }
        }
    }

    private void clickByTextContainsWithUiAutomator(String text) {
        UiObject button = device.findObject(new UiSelector()
                .textContains(text)
                .className("android.widget.Button"));
        try {
            button.click();
        } catch (Exception e) {
            throw new RuntimeException("Failed to click text containing: " + text, e);
        }
    }

    private void clickCancelDialog() {
        UiObject cancelButton = device.findObject(new UiSelector()
                .textContains("Cancel")
                .className("android.widget.Button"));
        try {
            cancelButton.click();
        } catch (Exception e) {
            throw new RuntimeException("Failed to click Cancel", e);
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

        activityRule.getScenario().onActivity(activity ->
                activity.startMeditation());

        SystemClock.sleep(4000);

        clickStopButtonWithUiAutomator();
        SystemClock.sleep(2000);

        assertTrue("Stop dialog should be visible",
                isDialogVisible("Stop meditation?"));

        clickCancelDialog();
        SystemClock.sleep(500);

        clickStopButtonWithUiAutomator();
        SystemClock.sleep(500);

        clickByTextContainsWithUiAutomator("Stop");

        SystemClock.sleep(2000);
    }

    @Test
    public void testTimerCountdown() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .selectSessionByPosition(0);

        activityRule.getScenario().onActivity(activity ->
                activity.startMeditation());

        SystemClock.sleep(4000);

        clickStopButtonWithUiAutomator();
        SystemClock.sleep(1000);

        clickByTextContainsWithUiAutomator("Stop");

        SystemClock.sleep(2000);
    }
}
