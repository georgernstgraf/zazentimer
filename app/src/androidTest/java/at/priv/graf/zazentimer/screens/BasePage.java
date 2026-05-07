package at.priv.graf.zazentimer.screens;

import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * Base page class that contains common functionality for all page objects.
 */
public abstract class BasePage {

    /**
     * Checks if an element is displayed on the screen.
     * @param viewId The ID of the view to check
     */
    public void checkElementIsDisplayed(int viewId) {
        onViewWithId(viewId).check(matches(isDisplayed()));
    }

    /**
     * Clicks on a view with the specified ID.
     * @param viewId The ID of the view to click
     */
    public void clickOnView(int viewId) {
        onViewWithId(viewId).perform(click());
    }

    /**
     * Gets a ViewInteraction for a view with the specified ID.
     * @param viewId The ID of the view
     * @return ViewInteraction for the view
     */
    protected ViewInteraction onViewWithId(int viewId) {
        return Espresso.onView(ViewMatchers.withId(viewId));
    }

    /**
     * Opens the toolbar overflow menu and clicks an item by its text.
     * @param textResId The resource ID of the menu item text (e.g. R.string.menu_settings)
     */
    public BasePage clickToolbarOverflowItem(int textResId) {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(textResId)).perform(click());
        return this;
    }

    /**
     * Clicks a dialog button by its text resource ID.
     * @param textId The resource ID of the button text
     */
    public BasePage clickDialogButton(int textId) {
        onView(withText(textId)).perform(click());
        return this;
    }

    /**
     * Asserts that a RecyclerView has the expected number of child items.
     * @param recyclerViewId The resource ID of the RecyclerView
     * @param expectedCount The expected item count
     */
    public BasePage assertRecyclerViewItemCount(int recyclerViewId, int expectedCount) {
        onView(withId(recyclerViewId)).check(matches(new RecyclerViewItemCountMatcher(expectedCount)));
        return this;
    }

    /**
     * Scrolls a RecyclerView to the given item position.
     * @param recyclerViewId The resource ID of the RecyclerView
     * @param position The position to scroll to
     */
    public BasePage scrollToRecyclerViewPosition(int recyclerViewId, int position) {
        onView(withId(recyclerViewId))
                .perform(RecyclerViewActions.scrollToPosition(position));
        return this;
    }

    /**
     * Presses the back button.
     */
    public void pressBack() {
        Espresso.pressBack();
    }

    /**
     * Returns a ViewAction that clicks a child view with the given ID
     * within a RecyclerView item. Use with RecyclerViewActions.actionOnItemAtPosition.
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "Click child view with id " + id;
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(id);
                if (child != null) {
                    child.performClick();
                }
            }
        };
    }

    private static class RecyclerViewItemCountMatcher extends org.hamcrest.TypeSafeMatcher<View> {
        private final int expectedCount;

        RecyclerViewItemCountMatcher(int expectedCount) {
            this.expectedCount = expectedCount;
        }

        @Override
        public void describeTo(org.hamcrest.Description description) {
            description.appendText("RecyclerView with item count: " + expectedCount);
        }

        @Override
        protected boolean matchesSafely(View item) {
            if (item instanceof androidx.recyclerview.widget.RecyclerView) {
                androidx.recyclerview.widget.RecyclerView recyclerView =
                        (androidx.recyclerview.widget.RecyclerView) item;
                return recyclerView.getAdapter() != null
                        && recyclerView.getAdapter().getItemCount() == expectedCount;
            }
            return false;
        }
    }
}