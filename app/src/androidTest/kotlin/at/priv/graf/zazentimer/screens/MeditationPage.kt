package at.priv.graf.zazentimer.screens

import at.priv.graf.zazentimer.R

class MeditationPage : BasePage() {
    fun verifyMeditationScreenIsDisplayed(): MeditationPage {
        checkElementIsDisplayed(R.id.but_pause)
        checkElementIsDisplayed(R.id.but_stop)
        checkElementIsDisplayed(R.id.timerView)
        return this
    }

    fun verifyTimerIsRunning(): MeditationPage {
        checkElementIsDisplayed(R.id.but_pause)
        checkElementIsDisplayed(R.id.but_stop)
        checkElementIsDisplayed(R.id.timerView)
        return this
    }

    fun clickPause(): MeditationPage {
        clickOnView(R.id.but_pause)
        return this
    }

    fun clickStop(): MainPage {
        clickOnView(R.id.but_stop)
        return MainPage()
    }

    fun verifyPauseButtonDisplayed(): MeditationPage {
        checkElementIsDisplayed(R.id.but_pause)
        return this
    }

    fun verifyStopButtonDisplayed(): MeditationPage {
        checkElementIsDisplayed(R.id.but_stop)
        return this
    }

    fun goBack(): MainPage {
        pressBack()
        return MainPage()
    }
}
