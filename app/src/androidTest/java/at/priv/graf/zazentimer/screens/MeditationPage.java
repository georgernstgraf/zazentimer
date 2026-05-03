package at.priv.graf.zazentimer.screens;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import at.priv.graf.zazentimer.R;

/**
 * Page object for the meditation screen of the application.
 */
public class MeditationPage extends BasePage {
    
    /**
     * Verifies that the meditation screen is displayed correctly.
     * @return This MeditationPage instance for method chaining
     */
    public MeditationPage verifyMeditationScreenIsDisplayed() {
        // Check that key meditation elements are displayed
        checkElementIsDisplayed(R.id.but_pause);
        checkElementIsDisplayed(R.id.but_stop);
        checkElementIsDisplayed(R.id.timerView);
        return this;
    }
    
    /**
     * Verifies that the meditation timer is running.
     * @return This MeditationPage instance for method chaining
     */
    public MeditationPage verifyTimerIsRunning() {
        // Check that the pause button is displayed which indicates the meditation has started
        checkElementIsDisplayed(R.id.but_pause);
        // Check that the stop button is displayed
        checkElementIsDisplayed(R.id.but_stop);
        // Check that the timer view is displayed
        checkElementIsDisplayed(R.id.timerView);
        return this;
    }
    
    /**
     * Clicks the pause button.
     * @return This MeditationPage instance for method chaining
     */
    public MeditationPage clickPause() {
        clickOnView(R.id.but_pause);
        return this;
    }
    
    /**
     * Clicks the stop button.
     * @return The MainPage instance after stopping meditation
     */
    public MainPage clickStop() {
        clickOnView(R.id.but_stop);
        return new MainPage();
    }

    /**
     * Verifies the pause button is displayed.
     * @return This MeditationPage instance for method chaining
     */
    public MeditationPage verifyPauseButtonDisplayed() {
        checkElementIsDisplayed(R.id.but_pause);
        return this;
    }

    /**
     * Verifies the stop button is displayed.
     * @return This MeditationPage instance for method chaining
     */
    public MeditationPage verifyStopButtonDisplayed() {
        checkElementIsDisplayed(R.id.but_stop);
        return this;
    }

    /**
     * Presses back and returns to the main screen.
     * @return The MainPage instance
     */
    public MainPage goBack() {
        pressBack();
        return new MainPage();
    }
}