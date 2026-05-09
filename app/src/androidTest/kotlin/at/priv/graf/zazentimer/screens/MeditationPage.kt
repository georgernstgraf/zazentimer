package at.priv.graf.zazentimer.screens

import at.priv.graf.zazentimer.R

class MeditationPage {
    private val robot = ScreenRobot()

    fun verifyMeditationScreenIsDisplayed(): MeditationPage {
        robot.checkElementIsDisplayed(R.id.but_pause)
        robot.checkElementIsDisplayed(R.id.but_stop)
        robot.checkElementIsDisplayed(R.id.timerView)
        return this
    }

    fun verifyTimerIsRunning(): MeditationPage {
        robot.checkElementIsDisplayed(R.id.but_pause)
        robot.checkElementIsDisplayed(R.id.but_stop)
        robot.checkElementIsDisplayed(R.id.timerView)
        return this
    }

    fun clickPause(): MeditationPage {
        robot.clickOnView(R.id.but_pause)
        return this
    }

    fun clickStop(): MainPage {
        robot.clickOnView(R.id.but_stop)
        return MainPage()
    }

    fun verifyPauseButtonDisplayed(): MeditationPage {
        robot.checkElementIsDisplayed(R.id.but_pause)
        return this
    }

    fun verifyStopButtonDisplayed(): MeditationPage {
        robot.checkElementIsDisplayed(R.id.but_stop)
        return this
    }

    fun goBack(): MainPage {
        robot.pressBack()
        return MainPage()
    }
}
