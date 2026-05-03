package at.priv.graf.zazentimer;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.SystemClock;

import at.priv.graf.zazentimer.screens.MainPage;
import at.priv.graf.zazentimer.screens.MeditationPage;
import at.priv.graf.zazentimer.screens.SessionEditPage;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainScreenNavigationTest {

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
    public void testSessionSelectionHighlight() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .selectSessionByPosition(0)
                .verifySessionSelected(0)
                .selectSessionByPosition(1)
                .verifySessionSelected(1);
    }

    @Test
    public void testAddSessionViaMenu() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_add_session);

        new SessionEditPage()
                .verifyEditSessionScreen();
    }

    @Test
    public void testBackArrowFromMeditation() {
        activityRule.getScenario().onActivity(activity ->
                activity.showMeditationScreen());

        MeditationPage meditationPage = new MeditationPage()
                .verifyMeditationScreenIsDisplayed()
                .verifyPauseButtonDisplayed()
                .verifyStopButtonDisplayed();

        meditationPage.goBack()
                .verifyMainScreenIsDisplayed();
    }

    @Test
    public void testBackArrowFromEdit() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_edit_session);

        new SessionEditPage()
                .verifyEditSessionScreen()
                .goBack()
                .verifyMainScreenIsDisplayed();
    }

    @Test
    public void testScreenRotation() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .selectSessionByPosition(0)
                .verifySessionSelected(0);

        activityRule.getScenario().recreate();

        SystemClock.sleep(500);

        new MainPage()
                .verifyMainScreenIsDisplayed()
                .verifySessionSelected(0);
    }
}
