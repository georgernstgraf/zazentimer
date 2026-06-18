package at.priv.graf.zazentimer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.priv.graf.zazentimer.backup.BackupManager
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.BellRepository
import at.priv.graf.zazentimer.database.BellSanitizer
import at.priv.graf.zazentimer.database.DatabaseOwner
import at.priv.graf.zazentimer.database.SectionRepository
import at.priv.graf.zazentimer.database.SessionRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

@BackupTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class BackupRestoreInstrumentedTest : AbstractZazenTest() {
    @Inject
    lateinit var databaseOwner: DatabaseOwner

    @Inject
    lateinit var bellSanitizer: BellSanitizer

    @Inject
    lateinit var sessionRepo: SessionRepository

    @Inject
    lateinit var sectionRepo: SectionRepository

    @Inject
    lateinit var bellRepo: BellRepository

    private lateinit var backupManager: BackupManager

    private val zipFile by lazy {
        File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "zentimer_backup_room_v2.zip",
        )
    }

    @Before
    fun setupBackupRestore() {
        Assume.assumeTrue(zipFile.exists())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        backupManager =
            BackupManager(
                databaseFileProvider = { context.getDatabasePath(AppDatabase.DATABASE_NAME) },
                filesDirProvider = { context.filesDir },
                onCloseDatabase = { databaseOwner.close() },
                onReopenDatabase = { databaseOwner.reopen() },
            )
        var activityRef: ZazenTimerActivity? = null
        activityRule.scenario.onActivity { activityRef = it }
        activityRef?.resetDatabaseForTest()
    }

    @Test
    fun restore_realBackup_fullPipelineWorks() {
        val result = backupManager.restore(zipFile)
        assertEquals(0, result)
        runBlocking { bellSanitizer.sanitizeBellUris() }
        val sessions = runBlocking { sessionRepo.readSessions() }
        assertEquals(4, sessions.size)
        var totalSections = 0
        for (session in sessions) {
            val sections = runBlocking { sectionRepo.readSections(session.id) }
            totalSections += sections.size
        }
        assertEquals(17, totalSections)
    }

    @Test
    fun restore_realBackup_sanitizeBellUris_normalizesUris() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val result = backupManager.restore(zipFile)
        assertEquals(0, result)
        runBlocking { bellSanitizer.sanitizeBellUris() }
        val bells = runBlocking { bellRepo.getAllBells() }
        for (bell in bells) {
            if (bell.isBuiltin) {
                val expectedPrefix = "android.resource://${context.packageName}/"
                assertTrue(
                    "Builtin bell '${bell.name}' URI should start with $expectedPrefix but was ${bell.uri}",
                    bell.uri.startsWith(expectedPrefix),
                )
            } else {
                val expectedPrefix = "file://${context.filesDir.absolutePath}/"
                assertTrue(
                    "Custom bell '${bell.name}' URI should start with $expectedPrefix but was ${bell.uri}",
                    bell.uri.startsWith(expectedPrefix),
                )
            }
        }
    }

    @Test
    fun restore_realBackup_customBellFile_extracted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        backupManager.restore(zipFile)
        val customBell = File(context.filesDir, "bell_blah.aac")
        assertTrue(customBell.exists())
        assertTrue(customBell.canRead())
    }
}
