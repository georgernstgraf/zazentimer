package at.priv.graf.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.contrib.RecyclerViewActions;

import at.priv.graf.zazentimer.R;

public class SettingsPage extends BasePage {

    public SettingsPage() {
        onView(withId(android.R.id.list)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) throw noViewFoundException;
        });
    }

    private void scrollPreferencesToTop() {
        try {
            onView(withId(android.R.id.list))
                    .perform(RecyclerViewActions.scrollToPosition(0));
        } catch (Exception ignored) {}
    }

    private void scrollPreferencesToBottom() {
        try {
            onView(withId(android.R.id.list))
                    .perform(RecyclerViewActions.scrollToPosition(99));
        } catch (Exception ignored) {}
    }

    private boolean scrollToPreference(int textResId) {
        scrollPreferencesToTop();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                onView(withText(textResId)).perform(scrollTo());
                return true;
            } catch (NoMatchingViewException e) {
                scrollPreferencesToBottom();
            }
        }
        try {
            onView(withText(textResId)).perform(scrollTo());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public SettingsPage clickBackup() {
        scrollToPreference(R.string.pref_title_backup);
        onView(withText(R.string.pref_title_backup)).perform(click());
        return this;
    }

    public SettingsPage clickRestoreAndConfirm() {
        scrollToPreference(R.string.pref_title_restore);
        onView(withText(R.string.pref_title_restore)).perform(click());
        onView(withText(R.string.ok)).perform(click());
        return this;
    }

    public MainPage goBack() {
        pressBack();
        return new MainPage();
    }
}
