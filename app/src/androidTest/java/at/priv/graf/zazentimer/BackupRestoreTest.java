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
import at.priv.graf.zazentimer.screens.MainPage;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BackupRestoreTest {

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
    public void activityLaunchesSuccessfully() {
        MainPage mainPage = new MainPage();
        mainPage.verifyMainScreenIsDisplayed();
    }
    
    @Test
    public void testFreshAppLaunch() {
        MainPage mainPage = new MainPage();
        mainPage.verifyMainScreenIsDisplayed();
        mainPage.verifyDefaultSessionsAreVisible();
    }
}
