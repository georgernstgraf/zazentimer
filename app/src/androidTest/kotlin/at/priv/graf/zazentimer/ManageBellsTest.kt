package at.priv.graf.zazentimer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.BellRepository
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.SettingsPage
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class ManageBellsTest : AbstractZazenTest() {
    @Inject
    lateinit var bellRepository: BellRepository

    @After
    fun tearDown() {
        runBlocking {
            for (bell in bellRepository.getNonBuiltinBells()) {
                bellRepository.deleteBellById(bell.id)
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.filesDir
            .listFiles()
            ?.filter { it.name.startsWith("bell_") }
            ?.forEach { it.delete() }
    }

    @Test
    fun testNavigateToManageBells() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()
            .clickManageBells()
            .verifyScreenDisplayed()
    }

    @Test
    fun testEmptyState() {
        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()
            .clickManageBells()
            .verifyEmptyState()
    }

    @Test
    fun testDeleteCustomBell() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bellFileName = "bell_test_bell.mp3"
        val bellFile = File(context.filesDir, bellFileName)
        bellFile.writeText("dummy audio content")
        runBlocking {
            bellRepository.insertBell(
                BellEntity(
                    name = "test_bell",
                    uri = "file://${context.filesDir}/$bellFileName",
                    isBuiltin = false,
                ),
            )
        }

        MainPage()
            .verifyMainScreenIsDisplayed()
            .clickToolbarOverflowItem(R.string.menu_settings)
        SettingsPage()
            .clickManageBells()
            .verifyBellListed("test_bell")
            .clickDeleteForBell("test_bell")
            .confirmDelete()
            .verifyEmptyState()
    }
}
