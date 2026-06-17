package at.priv.graf.zazentimer

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
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SessionEditPage
import at.priv.graf.zazentimer.screens.SettingsPage
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

        val originalFirstName = MainPage().getSessionNameAt(0)

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
            .verifyEditSessionScreen()
            .goBack()

        onIdle()

        MainPage()
            .verifyMainScreenIsDisplayed()

        onView(withId(R.id.recycler_sessions))
            .check(
                matches(
                    atPosition(
                        1,
                        hasDescendant(withText(originalFirstName)),
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

    @Test
    fun dragReorder_persistsAfterSettingsNavigation() {
        MainPage()
            .verifyMainScreenIsDisplayed()

        val originalFirstName = MainPage().getSessionNameAt(0)

        onView(withId(R.id.recycler_sessions))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    dragToPosition(1),
                ),
            )

        onIdle()

        MainPage()
            .clickToolbarOverflowItem(R.string.menu_settings)

        SettingsPage()
            .goBack()
            .verifyMainScreenIsDisplayed()

        onView(withId(R.id.recycler_sessions))
            .check(
                matches(
                    atPosition(
                        1,
                        hasDescendant(withText(originalFirstName)),
                    ),
                ),
            )
    }

    private fun dragToPosition(targetPosition: Int): ViewAction =
        object : ViewAction {
            override fun getConstraints(): Matcher<android.view.View> = isDisplayed()

            override fun getDescription(): String = "drag item to position $targetPosition"

            override fun perform(
                uiController: UiController,
                view: android.view.View,
            ) {
                val dragHandle = view.findViewById<android.view.View>(R.id.dragHandle)
                val recyclerView = view.parent as RecyclerView
                val targetHolder = recyclerView.findViewHolderForAdapterPosition(targetPosition)
                if (targetHolder == null) {
                    throw AssertionError("Target ViewHolder at position $targetPosition not found")
                }

                val handleLocation = IntArray(2)
                dragHandle.getLocationOnScreen(handleLocation)
                val sourceX = (handleLocation[0] + dragHandle.width / 2).toFloat()
                val sourceY = (handleLocation[1] + dragHandle.height / 2).toFloat()

                val targetLocation = IntArray(2)
                targetHolder.itemView.getLocationOnScreen(targetLocation)
                val targetX = (targetLocation[0] + targetHolder.itemView.width / 2).toFloat()
                val targetY = (targetLocation[1] + targetHolder.itemView.height / 2).toFloat()

                val downTime = android.os.SystemClock.uptimeMillis()
                val steps = 20

                val downEvent =
                    android.view.MotionEvent.obtain(
                        downTime,
                        downTime,
                        android.view.MotionEvent.ACTION_DOWN,
                        sourceX,
                        sourceY,
                        0,
                    )
                uiController.injectMotionEvent(downEvent)
                downEvent.recycle()

                for (i in 1..steps) {
                    val eventTime = downTime + i * 5L
                    val x = sourceX + (targetX - sourceX) * i / steps
                    val y = sourceY + (targetY - sourceY) * i / steps
                    val moveEvent =
                        android.view.MotionEvent.obtain(
                            downTime,
                            eventTime,
                            android.view.MotionEvent.ACTION_MOVE,
                            x,
                            y,
                            0,
                        )
                    uiController.injectMotionEvent(moveEvent)
                    moveEvent.recycle()
                }

                val upEvent =
                    android.view.MotionEvent.obtain(
                        downTime,
                        downTime + (steps + 1) * 5L,
                        android.view.MotionEvent.ACTION_UP,
                        targetX,
                        targetY,
                        0,
                    )
                uiController.injectMotionEvent(upEvent)
                upEvent.recycle()

                uiController.loopMainThreadUntilIdle()
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
