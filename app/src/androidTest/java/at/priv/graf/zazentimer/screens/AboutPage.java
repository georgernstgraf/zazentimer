package at.priv.graf.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.containsString;

import at.priv.graf.zazentimer.R;

public class AboutPage extends BasePage {

    public AboutPage verifyAboutScreen() {
        onView(withText(R.string.caption_zazen_meditation)).check(matches(isDisplayed()));
        return this;
    }

    public AboutPage verifyGitHash(String hash) {
        onView(withText(containsString("Commit: " + hash))).check(matches(isDisplayed()));
        return this;
    }

    public MainPage clickOk() {
        clickDialogButton(R.string.privacy_ok);
        return new MainPage();
    }
}
