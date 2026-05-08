package at.priv.graf.zazentimer.screens

import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation

abstract class BasePage {

    fun checkElementIsDisplayed(viewId: Int) {
        onViewWithId(viewId).check(matches(isDisplayed()))
    }

    fun clickOnView(viewId: Int) {
        onViewWithId(viewId).perform(click())
    }

    protected fun onViewWithId(viewId: Int): ViewInteraction {
        return Espresso.onView(ViewMatchers.withId(viewId))
    }

    open fun clickToolbarOverflowItem(textResId: Int): BasePage {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        SystemClock.sleep(500)
        onView(withText(textResId)).perform(click())
        return this
    }

    open fun clickDialogButton(textId: Int): BasePage {
        onView(withText(textId)).perform(click())
        return this
    }

    fun assertRecyclerViewItemCount(recyclerViewId: Int, expectedCount: Int): BasePage {
        onView(withId(recyclerViewId)).check(matches(RecyclerViewItemCountMatcher(expectedCount)))
        return this
    }

    fun scrollToRecyclerViewPosition(recyclerViewId: Int, position: Int): BasePage {
        onView(withId(recyclerViewId))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
        return this
    }

    fun pressBack() {
        Espresso.pressBack()
    }

    companion object {
        fun clickChildViewWithId(id: Int): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isAssignableFrom(View::class.java)
                }

                override fun getDescription(): String {
                    return "Click child view with id $id"
                }

                override fun perform(uiController: UiController, view: View) {
                    val child = view.findViewById<View>(id)
                    child?.performClick()
                }
            }
        }
    }

    private class RecyclerViewItemCountMatcher(private val expectedCount: Int) : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("RecyclerView with item count: $expectedCount")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item is RecyclerView) {
                return item.adapter != null && item.adapter!!.itemCount == expectedCount
            }
            return false
        }
    }
}
