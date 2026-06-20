package at.priv.graf.zazentimer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.BellRepository
import at.priv.graf.zazentimer.database.SectionRepository
import at.priv.graf.zazentimer.database.SessionRepository
import at.priv.graf.zazentimer.screens.MainPage
import at.priv.graf.zazentimer.screens.ManageBellsPage
import at.priv.graf.zazentimer.screens.SettingsPage
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sectionRepository: SectionRepository

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

    @Test
    fun importCustomBell_viaSafIntent_showsInList() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceFile = File(context.cacheDir, "import_test.mp3")
        context.resources.openRawResource(R.raw.bell1).use { input ->
            sourceFile.outputStream().use { output -> input.copyTo(output) }
        }

        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_CHOOSER))
                .respondWith(
                    Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                        Intent().setData(Uri.fromFile(sourceFile)),
                    ),
                )

            MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings)
            SettingsPage()
                .clickManageBells()
                .verifyScreenDisplayed()

            onView(withId(R.id.importButton)).perform(click())

            onIdle()

            ManageBellsPage().verifyBellListed("import_test.mp3")
        } finally {
            Intents.release()
            sourceFile.delete()
        }
    }

    @Test
    fun deleteCustomBellUsedBySection_showsAffectedDialogAndReassignsToDemo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bellFileName = "bell_section_test.mp3"
        val bellFile = File(context.filesDir, bellFileName)
        context.resources.openRawResource(R.raw.bell1).use { input ->
            bellFile.outputStream().use { output -> input.copyTo(output) }
        }

        val customBellName = "section_test_bell"
        val sessionName = "Test Session"
        val sectionName = "Test Section"

        var customBellId = -1
        var sessionId = -1
        var sectionId = -1
        var demoBellId = -1

        runBlocking {
            val customBell =
                BellEntity(
                    name = customBellName,
                    uri = "file://${bellFile.absolutePath}",
                    isBuiltin = false,
                )
            customBellId = bellRepository.insertBell(customBell).toInt()
            demoBellId = bellRepository.getDemoBellIdOrThrow()

            val session = Session(sessionName, "Test Description")
            sessionRepository.insertSession(session)
            sessionId = session.id

            val section = Section(sectionName, 120)
            section.bellId = customBellId
            sectionRepository.insertSection(session, section)
            sectionId = section.id
        }

        try {
            MainPage()
                .verifyMainScreenIsDisplayed()
                .clickToolbarOverflowItem(R.string.menu_settings)
            SettingsPage()
                .clickManageBells()
                .verifyBellListed(customBellName)
                .clickDeleteForBell(customBellName)

            val expectedMessage =
                context.getString(
                    R.string.confirm_delete_bell_affected,
                    customBellName,
                    context.getString(
                        R.string.affected_section_format,
                        sectionName,
                        sessionName,
                    ),
                )
            onView(withText(expectedMessage))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withText(R.string.action_delete))
                .inRoot(isDialog())
                .perform(click())

            onIdle()

            ManageBellsPage().verifyEmptyState()

            val section = runBlocking { sectionRepository.readSection(sectionId) }
            assertEquals(demoBellId, section?.bellId)
        } finally {
            runBlocking {
                if (sectionId != -1) {
                    sectionRepository.deleteSection(sectionId.toLong())
                }
                if (sessionId != -1) {
                    sessionRepository.deleteSession(sessionId)
                }
            }
        }
    }
}
