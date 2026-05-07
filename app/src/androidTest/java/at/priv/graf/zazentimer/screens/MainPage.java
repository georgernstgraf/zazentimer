package at.priv.graf.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.SystemClock;

import androidx.test.espresso.contrib.RecyclerViewActions;

import at.priv.graf.zazentimer.R;

public class MainPage extends BasePage {

    public MainPage verifyMainScreenIsDisplayed() {
        checkElementIsDisplayed(R.id.my_toolbar);
        checkElementIsDisplayed(R.id.but_start);
        checkElementIsDisplayed(R.id.recycler_sessions);
        return this;
    }

    public MainPage verifyDefaultSessionsAreVisible() {
        onView(withId(R.id.recycler_sessions)).check(matches(isDisplayed()));
        onView(withId(R.id.but_start)).check(matches(isDisplayed()));
        return this;
    }

    public MainPage selectSessionByPosition(int position) {
        onView(withId(R.id.recycler_sessions))
                .perform(RecyclerViewActions.actionOnItemAtPosition(position, click()));
        return this;
    }

    public MeditationPage clickStartMeditation() {
        clickOnView(R.id.but_start);
        return new MeditationPage();
    }

    public MainPage verifySessionSelected(int position) {
        onView(withId(R.id.recycler_sessions))
                .perform(RecyclerViewActions.scrollToPosition(position));
        onView(withId(R.id.recycler_sessions))
                .perform(RecyclerViewActions.actionOnItemAtPosition(position, new androidx.test.espresso.ViewAction() {
                    @Override
                    public org.hamcrest.Matcher<android.view.View> getConstraints() {
                        return isDisplayed();
                    }

                    @Override
                    public String getDescription() {
                        return "check selected at position " + position;
                    }

                    @Override
                    public void perform(androidx.test.espresso.UiController uiController, android.view.View view) {
                        if (!view.isSelected() && !view.isActivated()) {
                            throw new AssertionError("View at position " + position + " is not selected/activated");
                        }
                    }
                }));
        return this;
    }

    public MainPage clickSessionOverflowAtPosition(int position) {
        onView(withId(R.id.recycler_sessions))
                .perform(RecyclerViewActions.actionOnItemAtPosition(position, clickChildViewWithId(R.id.sessionOverflow)));
        SystemClock.sleep(500);
        return this;
    }

    public MainPage clickSessionOverflowAction(int position, int textResId) {
        clickSessionOverflowAtPosition(position);
        onView(withText(textResId)).perform(click());
        return this;
    }

    public MainPage clickToolbarOverflowItem(int textResId) {
        super.clickToolbarOverflowItem(textResId);
        return this;
    }
}
