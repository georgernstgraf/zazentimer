package de.gaffga.android.zazentimer.screens;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.contrib.RecyclerViewActions;

import de.gaffga.android.zazentimer.R;

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
}
