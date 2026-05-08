package at.priv.graf.zazentimer

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import at.priv.graf.zazentimer.screens.MainPage

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class BackupRestoreTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(ZazenTimerActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun activityLaunchesSuccessfully() {
        val mainPage = MainPage()
        mainPage.verifyMainScreenIsDisplayed()
    }

    @Test
    fun testFreshAppLaunch() {
        val mainPage = MainPage()
        mainPage.verifyMainScreenIsDisplayed()
        mainPage.verifyDefaultSessionsAreVisible()
    }
}
