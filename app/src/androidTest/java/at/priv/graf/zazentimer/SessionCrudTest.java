package at.priv.graf.zazentimer;

import android.os.SystemClock;

import androidx.test.espresso.Espresso;
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

import static org.hamcrest.Matchers.not;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionCrudTest {

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
    public void testOpenEditSession() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_edit_session);

        new SessionEditPage()
                .verifyEditSessionScreen();

        onView(withId(R.id.text_sitzung_name))
                .check(matches(not(withText(""))));
    }

    @Test
    public void testUpdateSessionMetadata() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_edit_session);

        new SessionEditPage()
                .verifyEditSessionScreen();

        onView(withId(R.id.text_sitzung_name))
                .perform(clearText(), typeText("Updated Session Name"));
        onView(withId(R.id.text_sitzung_beschreibung))
                .perform(clearText(), typeText("Updated Description"));

        new SessionEditPage().goBack();

        for (int i = 0; i < 20; i++) {
            SystemClock.sleep(500);
            try {
                new MainPage().verifyMainScreenIsDisplayed();
                break;
            } catch (Exception e) {
                if (i == 19) throw e;
            }
        }

        onView(withText("Updated Session Name")).check(matches(isDisplayed()));
    }

    @Test
    public void testDeleteSession() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_delete_session);

        onView(withText(R.string.title_question_delete_session))
                .check(matches(isDisplayed()));
        SystemClock.sleep(500);
        onView(withText(R.string.ok)).perform(click());

        SystemClock.sleep(500);

        new MainPage()
                .verifyMainScreenIsDisplayed();
    }

    @Test
    public void testDeleteCancel() {
        new MainPage()
                .verifyMainScreenIsDisplayed()
                .clickSessionOverflowAction(0, R.string.menu_delete_session);

        onView(withText(R.string.title_question_delete_session))
                .check(matches(isDisplayed()));
        SystemClock.sleep(500);
        onView(withText(R.string.abbrechen)).perform(click());

        SystemClock.sleep(500);

        new MainPage()
                .verifyMainScreenIsDisplayed();
    }
}
