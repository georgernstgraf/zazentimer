package at.priv.graf.zazentimer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.backup.BackupManager
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.DatabaseOwner
import at.priv.graf.zazentimer.database.SectionRepository
import at.priv.graf.zazentimer.database.SessionRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
@org.junit.Ignore("Blocked by #290: ActivityScenario.launch hangs after resetDatabaseForTest.")
class ZazenTimerBackupTest : AbstractZazenTest() {
    @Inject
    lateinit var databaseOwner: DatabaseOwner

    @Inject
    lateinit var sessionRepo: SessionRepository

    @Inject
    lateinit var sectionRepo: SectionRepository

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, ZazenTimerActivity.BACKUP_ZIP_NAME).delete()
        activityRule.scenario.onActivity { it.resetDatabaseForTest() }
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
