package at.priv.graf.zazentimer.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.utils.ScreenRobot
import org.hamcrest.Matchers.containsString

class AboutPage {
    private val robot = ScreenRobot()

    fun verifyAboutScreen(): AboutPage {
        onView(withText(R.string.caption_zazen_meditation)).check(matches(isDisplayed()))
        return this
    }

    fun verifyGitHash(hash: String): AboutPage {
        onView(withText(containsString("Commit: $hash"))).check(matches(isDisplayed()))
        return this
    }

    fun verifyVersion(version: String): AboutPage {
        onView(withText(containsString("Version: $version"))).check(matches(isDisplayed()))
        return this
    }

    fun clickOk(): MainPage {
        robot.clickDialogButton(R.string.privacy_ok)
        return MainPage()
    }
}
