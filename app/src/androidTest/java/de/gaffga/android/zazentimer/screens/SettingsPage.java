package de.gaffga.android.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.SystemClock;

import de.gaffga.android.zazentimer.R;

public class SettingsPage extends BasePage {

    private static final int PREF_RECYCLER_ID = android.R.id.list;

    public SettingsPage() {
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        try {
            onView(withText(R.string.pref_title_backup)).perform(scrollTo());
        } catch (Exception e) {
            try {
                onView(withId(PREF_RECYCLER_ID)).perform(scrollToPosition(99));
                SystemClock.sleep(500);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Taps the backup preference — triggers SAF file picker.
     */
    public SettingsPage clickBackup() {
        try {
            onView(withId(PREF_RECYCLER_ID)).perform(scrollToPosition(99));
            SystemClock.sleep(300);
        } catch (Exception ignored) {}
        onView(withText(R.string.pref_title_backup)).perform(click());
        return this;
    }

    /**
     * Taps the restore preference and confirms the warning dialog.
     * After confirmation, SAF file picker opens.
     */
    public SettingsPage clickRestoreAndConfirm() {
        try {
            onView(withId(PREF_RECYCLER_ID)).perform(scrollToPosition(99));
            SystemClock.sleep(300);
        } catch (Exception ignored) {}
        onView(withText(R.string.pref_title_restore)).perform(click());
        SystemClock.sleep(300);
        onView(withText(R.string.ok)).perform(click());
        return this;
    }

    /**
     * Navigates back to the main screen.
     */
    public MainPage goBack() {
        pressBack();
        return new MainPage();
    }
}
