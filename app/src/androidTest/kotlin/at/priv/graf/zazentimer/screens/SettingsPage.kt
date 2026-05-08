package at.priv.graf.zazentimer.screens

import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.contrib.RecyclerViewActions

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText

import at.priv.graf.zazentimer.R

class SettingsPage : BasePage() {

    init {
        SystemClock.sleep(1000)
        onView(withId(R.id.recycler_view)).check { _, noViewFoundException ->
            if (noViewFoundException != null) throw noViewFoundException
        }
    }

    private fun scrollPreferencesToTop() {
        try {
            onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            SystemClock.sleep(300)
        } catch (_: Exception) {}
    }

    private fun scrollPreferencesToBottom() {
        try {
            onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(99))
            SystemClock.sleep(500)
        } catch (_: Exception) {}
    }

    private fun scrollToPreference(textResId: Int): Boolean {
        scrollPreferencesToTop()
        for (attempt in 0 until 3) {
            try {
                onView(withText(textResId)).perform(scrollTo())
                return true
            } catch (e: NoMatchingViewException) {
                scrollPreferencesToBottom()
                SystemClock.sleep(300)
            }
        }
        try {
            onView(withText(textResId)).perform(scrollTo())
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun clickBackup(): SettingsPage {
        scrollToPreference(R.string.pref_title_backup)
        onView(withText(R.string.pref_title_backup)).perform(click())
        return this
    }

    fun clickRestoreAndConfirm(): SettingsPage {
        scrollToPreference(R.string.pref_title_restore)
        onView(withText(R.string.pref_title_restore)).perform(click())
        SystemClock.sleep(300)
        onView(withText(R.string.ok)).perform(click())
        return this
    }

    fun goBack(): MainPage {
        pressBack()
        return MainPage()
    }
}
