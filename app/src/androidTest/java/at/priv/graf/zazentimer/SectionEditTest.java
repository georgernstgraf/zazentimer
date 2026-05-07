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
import at.priv.graf.zazentimer.screens.SessionEditPage;
import at.priv.graf.zazentimer.screens.SectionEditPage;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SectionEditTest {

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
    public void testAddNewSection() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_edit_session);

        SessionEditPage sessionEditPage = new SessionEditPage()
                .verifyEditSessionScreen();

        SectionEditPage sectionEditPage = sessionEditPage.clickAddSection();

        sectionEditPage.verifySectionEditScreen();

        onView(withId(R.id.time)).check(matches(withText("01:00")));
    }

    @Test
    public void testEditSectionConfig() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_edit_session);

        new SessionEditPage()
                .verifyEditSessionScreen()
                .clickSectionAtPosition(0);

        new SectionEditPage()
                .verifySectionEditScreen()
                .tapDurationPicker();

        onView(withId(android.R.id.button1)).perform(click());

        new SectionEditPage()
                .setBellCount(3)
                .setBellGap(8)
                .goBack();

        // Verify we're back on session edit screen after goBack
        new SessionEditPage().verifyEditSessionScreen();
    }

    @Test
    public void testBellSoundPlayback() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_edit_session);

        new SessionEditPage()
                .verifyEditSessionScreen()
                .clickSectionAtPosition(0);

        SectionEditPage sectionEditPage = new SectionEditPage()
                .verifySectionEditScreen();

        sectionEditPage.clickPlayBell();

        sectionEditPage.verifySectionEditScreen();
    }
}
