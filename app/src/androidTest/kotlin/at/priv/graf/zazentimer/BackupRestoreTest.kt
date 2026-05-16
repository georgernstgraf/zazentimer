package at.priv.graf.zazentimer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import at.priv.graf.zazentimer.screens.MainPage
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class BackupRestoreTest : AbstractZazenTest() {
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
