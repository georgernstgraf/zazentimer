package de.gaffga.android.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.SystemClock;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.contrib.RecyclerViewActions;

import de.gaffga.android.zazentimer.R;

public class SettingsPage extends BasePage {

    public SettingsPage() {
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    private void scrollPreferencesToBottom() {
        try {
            onView(withId(android.R.id.list))
                    .perform(RecyclerViewActions.scrollToPosition(99));
            SystemClock.sleep(500);
        } catch (Exception ignored) {}
    }

    public SettingsPage clickBackup() {
        scrollPreferencesToBottom();
        try {
            onView(withText(R.string.pref_title_backup)).perform(scrollTo());
        } catch (Exception ignored) {}
        onView(withText(R.string.pref_title_backup)).perform(click());
        return this;
    }

    public SettingsPage clickRestoreAndConfirm() {
        scrollPreferencesToBottom();
        try {
            onView(withText(R.string.pref_title_restore)).perform(scrollTo());
        } catch (Exception ignored) {}
        onView(withText(R.string.pref_title_restore)).perform(click());
        SystemClock.sleep(300);
        onView(withText(R.string.ok)).perform(click());
        return this;
    }

    public MainPage goBack() {
        pressBack();
        return new MainPage();
    }
}
