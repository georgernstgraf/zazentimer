package at.priv.graf.zazentimer

import android.graphics.Point
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SectionEditPage
import at.priv.graf.zazentimer.screens.SessionEditPage
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SessionRankPersistenceTest : AbstractZazenTest() {
    @Before
    fun setupDatabase() {
        var activityRef: ZazenTimerActivity? = null
        activityRule.scenario.onActivity { activity ->
            activityRef = activity
            ZazenTimerActivity
                .getPreferences(activity)
                .edit()
                .putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                .apply()
        }
        activityRef?.resetDatabaseForTest()
    }

    @Test
    fun dragReorder_persistsAfterNavigationAndEdit() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()
            .clickAddSection()

        SectionEditPage()
            .goBack()

        SessionEditPage()
            .goBack()

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()

        onView(withId(R.id.recycler_sessions))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    dragToPosition(1),
                ),
            )

        onIdle()

        MainPage()
            .clickSessionOverflowAction(0, R.string.menu_edit_session)

        SessionEditPage()
            .goBack()

        onIdle()

        onView(withId(R.id.recycler_sessions))
            .check(
                matches(
                    atPosition(
                        0,
                        hasDescendant(withId(R.id.sessionName)),
                    ),
                ),
            )

        MainPage()
            .clickSessionOverflowAction(1, R.string.menu_edit_session)

        SessionEditPage()
            .verifyEditSessionScreen()

        onView(withId(R.id.text_session_name))
            .perform(replaceText("Renamed Session"))
        closeSoftKeyboard()

        SessionEditPage()
            .goBack()

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()
    }

    private fun dragToPosition(targetPosition: Int): ViewAction =
        object : ViewAction {
            override fun getConstraints(): Matcher<android.view.View> = isDisplayed()

            override fun getDescription(): String = "drag item to position $targetPosition"

            override fun perform(
                uiController: UiController,
                view: android.view.View,
            ) {
                val recyclerView = view.parent as RecyclerView
                val targetHolder = recyclerView.findViewHolderForAdapterPosition(targetPosition)
                if (targetHolder == null) {
                    throw AssertionError("Target ViewHolder at position $targetPosition not found")
                }
                val sourcePoint = Point(view.width / 2, view.height / 2)
                val targetPoint =
                    Point(
                        targetHolder.itemView.width / 2,
                        targetHolder.itemView.height / 2,
                    )
                val steps = 20
                for (i in 1..steps) {
                    val x = sourcePoint.x + (targetPoint.x - sourcePoint.x) * i / steps
                    val y = sourcePoint.y + (targetPoint.y - sourcePoint.y) * i / steps
                    uiController.injectMotionEvent(
                        android.view.MotionEvent.obtain(
                            android.os.SystemClock.uptimeMillis(),
                            android.os.SystemClock.uptimeMillis(),
                            android.view.MotionEvent.ACTION_MOVE,
                            x.toFloat(),
                            y.toFloat(),
                            0,
                        ),
                    )
                }
            }
        }

    private fun atPosition(
        position: Int,
        itemMatcher: Matcher<android.view.View>,
    ): Matcher<android.view.View> =
        object : androidx.test.espresso.matcher.BoundedMatcher<android.view.View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                return viewHolder?.itemView?.let { itemMatcher.matches(it) } ?: false
            }
        }
}
