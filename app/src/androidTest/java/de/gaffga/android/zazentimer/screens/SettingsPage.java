package de.gaffga.android.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import de.gaffga.android.zazentimer.R;

/**
 * Page object for the settings screen.
 */
public class SettingsPage extends BasePage {

    public SettingsPage() {
        // Wait for settings fragment to load and scroll to backup preference
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        try {
            onView(withText(R.string.pref_title_backup)).perform(scrollTo());
            onView(withText(R.string.pref_title_backup)).check(matches(isDisplayed()));
        } catch (Exception e) {
            // May already be visible without scrolling
        }
    }

    /**
     * Taps the backup preference — triggers SAF file picker.
     */
    public SettingsPage clickBackup() {
        try {
            onView(withText(R.string.pref_title_backup)).perform(scrollTo());
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
            onView(withText(R.string.pref_title_restore)).perform(scrollTo());
        } catch (Exception ignored) {}
        onView(withText(R.string.pref_title_restore)).perform(click());
        // Confirm the "are you sure?" dialog
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
