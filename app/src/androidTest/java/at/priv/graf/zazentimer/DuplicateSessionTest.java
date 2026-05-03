package at.priv.graf.zazentimer;

import android.view.View;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import org.hamcrest.Matcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.os.SystemClock;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DuplicateSessionTest {

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
    public ActivityScenarioRule<ZazenTimerActivity> activityRule =
            new ActivityScenarioRule<>(ZazenTimerActivity.class);

    @Before
    public void init() {
        hiltRule.inject();
        activityRule.getScenario().onActivity(ZazenTimerActivity::resetDatabaseForTest);
    }

    @Test
    public void testDuplicateSessionDoesNotCrash() {
        onView(withId(R.id.recycler_sessions))
                .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.sessionOverflow)));

        // Wait for popup menu animation to complete
        SystemClock.sleep(500);

        onView(withText(R.string.menu_copy_session))
                .perform(click());

        onView(withId(R.id.recycler_sessions))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDuplicateSessionCreatesCopyWithPrefix() {
        onView(withId(R.id.recycler_sessions))
                .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.sessionOverflow)));

        SystemClock.sleep(500);

        onView(withText(R.string.menu_copy_session))
                .perform(click());

        onView(withText(containsString("Copy of")))
                .check(matches(isDisplayed()));
    }

    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "Click child view with id " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(id);
                if (child != null) {
                    child.performClick();
                }
            }
        };
    }
}
