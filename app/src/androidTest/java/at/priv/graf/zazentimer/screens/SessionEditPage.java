package at.priv.graf.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.os.SystemClock;

import at.priv.graf.zazentimer.R;

public class SessionEditPage extends BasePage {

    public SessionEditPage verifyEditSessionScreen() {
        checkElementIsDisplayed(R.id.text_sitzung_name);
        checkElementIsDisplayed(R.id.but_new_section);
        return this;
    }

    public SessionEditPage setSessionName(String name) {
        onView(withId(R.id.text_sitzung_name)).perform(typeText(name));
        return this;
    }

    public SessionEditPage setSessionDescription(String desc) {
        onView(withId(R.id.text_sitzung_beschreibung)).perform(typeText(desc));
        return this;
    }

    public SectionEditPage clickAddSection() {
        clickOnView(R.id.but_new_section);
        return new SectionEditPage();
    }

    public SessionEditPage clickSectionAtPosition(int pos) {
        onView(withId(R.id.list))
                .perform(actionOnItemAtPosition(pos, click()));
        return this;
    }

    public SessionEditPage verifySectionCount(int count) {
        assertRecyclerViewItemCount(R.id.list, count);
        return this;
    }

    public MainPage goBack() {
        pressBack();
        SystemClock.sleep(800);
        return new MainPage();
    }
}
