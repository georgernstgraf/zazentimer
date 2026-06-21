package at.priv.graf.zazentimer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.backup.BackupManager
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.BellSanitizer
import at.priv.graf.zazentimer.database.DatabaseOwner
import at.priv.graf.zazentimer.database.DemoSessionCreator
import at.priv.graf.zazentimer.database.SectionRepository
import at.priv.graf.zazentimer.database.SessionRepository
import at.priv.graf.zazentimer.service.IdlingResourceManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.io.path.Path

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class ZazenTimerBackupTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val globalTimeout: Timeout = Timeout(5, TimeUnit.MINUTES)

    @Inject
    lateinit var databaseOwner: DatabaseOwner

    @Inject
    lateinit var sessionRepo: SessionRepository

    @Inject
    lateinit var sectionRepo: SectionRepository

    @Inject
    lateinit var bellSanitizer: BellSanitizer

    @Inject
    lateinit var demoSessionCreator: DemoSessionCreator

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        IdlingRegistry.getInstance().register(IdlingResourceManager.countingIdlingResource)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, ZazenTimerActivity.BACKUP_ZIP_NAME).delete()
        runBlocking(Dispatchers.IO) {
            val sessions = sessionRepo.readSessions()
            for (session in sessions) {
                for (section in sectionRepo.readSections(session.id)) {
                    sectionRepo.deleteSection(section.id.toLong())
                }
                sessionRepo.deleteSession(session.id)
            }
            bellSanitizer.sanitizeBellUris()
            demoSessionCreator.createDemoSessions()
        }
        val noBackupDir = Path(context.noBackupFilesDir.absolutePath).toFile()
        File(noBackupDir, "demo_sessions_created").createNewFile()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(IdlingResourceManager.countingIdlingResource)
    }

    @Test
    fun createBackupAndFinish_producesZipAndRestoresData() {
        val expectedSessions = runBlocking { sessionRepo.readSessions() }
        val expectedSectionCount =
            expectedSessions.sumOf { session ->
                runBlocking { sectionRepo.readSections(session.id).size }
            }
        val expectedSessionNames = expectedSessions.map { it.name }.toSet()

        val intent =
            Intent(context, ZazenTimerActivity::class.java).apply {
                putExtra(ZazenTimerActivity.INTENT_EXTRA_CREATE_BACKUP, "true")
            }

        ActivityScenario.launch<ZazenTimerActivity>(intent).use { scenario ->
            val deadline = System.currentTimeMillis() + BACKUP_TIMEOUT_MS
            while (scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
                Thread.sleep(POLL_INTERVAL_MS)
            }
            assertEquals(
                "Activity should finish after creating the backup",
                Lifecycle.State.DESTROYED,
                scenario.state,
            )
        }

        val zipFile = File(context.cacheDir, ZazenTimerActivity.BACKUP_ZIP_NAME)
        assertTrue("Backup ZIP should exist", zipFile.exists())
        assertTrue("Backup ZIP should contain data", zipFile.length() > 0)

        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().toList()
            val databaseEntry = entries.find { it.name == AppDatabase.DATABASE_NAME }
            assertNotNull("Backup ZIP should contain the database file", databaseEntry)

            val unexpectedEntries =
                entries.filter {
                    it.name != AppDatabase.DATABASE_NAME && !it.name.startsWith("bell_")
                }
            assertTrue(
                "Backup ZIP should only contain the database and bell_* files, but found: " +
                    unexpectedEntries.joinToString { it.name },
                unexpectedEntries.isEmpty(),
            )
        }

        val backupManager =
            BackupManager(
                databaseFileProvider = { context.getDatabasePath(AppDatabase.DATABASE_NAME) },
                filesDirProvider = { context.filesDir },
                onCloseDatabase = { databaseOwner.close() },
                onReopenDatabase = { databaseOwner.reopen() },
            )
        val restoreResult = backupManager.restore(zipFile)
        assertEquals("Restore should succeed", 0, restoreResult)

        val restoredSessions = runBlocking { sessionRepo.readSessions() }
        assertEquals(
            "Session count should match after restore",
            expectedSessions.size,
            restoredSessions.size,
        )

        val restoredSectionCount =
            restoredSessions.sumOf { session ->
                runBlocking { sectionRepo.readSections(session.id).size }
            }
        assertEquals(
            "Section count should match after restore",
            expectedSectionCount,
            restoredSectionCount,
        )

        val restoredSessionNames = restoredSessions.map { it.name }.toSet()
        assertEquals(
            "Restored session names should match",
            expectedSessionNames,
            restoredSessionNames,
        )
    }

    companion object {
        private const val BACKUP_TIMEOUT_MS = 30_000L
        private const val POLL_INTERVAL_MS = 200L
    }
}
