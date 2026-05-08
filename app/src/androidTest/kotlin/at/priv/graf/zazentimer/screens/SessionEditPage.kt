package at.priv.graf.zazentimer.screens

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId

import at.priv.graf.zazentimer.R

class SessionEditPage : BasePage() {

    fun verifyEditSessionScreen(): SessionEditPage {
        checkElementIsDisplayed(R.id.text_sitzung_name)
        checkElementIsDisplayed(R.id.but_new_section)
        return this
    }

    fun setSessionName(name: String): SessionEditPage {
        onView(withId(R.id.text_sitzung_name)).perform(typeText(name))
        return this
    }

    fun setSessionDescription(desc: String): SessionEditPage {
        onView(withId(R.id.text_sitzung_beschreibung)).perform(typeText(desc))
        return this
    }

    fun clickAddSection(): SectionEditPage {
        clickOnView(R.id.but_new_section)
        return SectionEditPage()
    }

    fun clickSectionAtPosition(pos: Int): SessionEditPage {
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(pos, click()))
        return this
    }

    fun verifySectionCount(count: Int): SessionEditPage {
        assertRecyclerViewItemCount(R.id.list, count)
        return this
    }

    fun goBack(): MainPage {
        pressBack()
        return MainPage()
    }
}
