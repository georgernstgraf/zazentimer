
package at.priv.graf.zazentimer.backup

import android.content.Context
import at.priv.graf.zazentimer.MigrationHelper
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.DbOperations
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreIntegrationTest(
    private val backupFileName: String,
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Restore Backup: {0}")
        fun getBackupFiles(): Collection<Array<String>> {
            val userDir = System.getProperty("user.dir")
            val backupsDir = File(userDir, "src/test/resources/backups")

            // Fallback if running from a different working directory
            val files =
                if (backupsDir.exists() && backupsDir.isDirectory) {
                    backupsDir.listFiles { _, name -> name.endsWith(".zip") }?.map { it.name } ?: emptyList()
                } else {
                    // Hardcode known backups if dir scanning fails
                    listOf(
                        "zazentimer-georg-0516-backup.zip",
                        "zazentimer-georg-backup_01.zip",
                        "zazentimer-georg-backup_02.zip",
                        "zazentimer-georg-prod-backup.zip",
                        "zazentimer-lena-backup.zip",
                    )
                }

            return files.map { arrayOf(it) }
        }
    }

    private lateinit var context: Context
    private lateinit var tempDir: File
    private lateinit var databaseFile: File
    private lateinit var filesDir: File
    private lateinit var backupManager: BackupManager
    private var db: AppDatabase? = null

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        at.priv.graf.zazentimer.audio.BellCollection
            .initialize(context)
        tempDir = File(context.cacheDir, "restore-test-${System.currentTimeMillis()}").apply { mkdirs() }
        databaseFile = File(tempDir, "zentimer")
        filesDir = File(tempDir, "files").apply { mkdirs() }

        backupManager =
            BackupManager(
                databaseFileProvider = { databaseFile },
                filesDirProvider = { filesDir },
                onCloseDatabase = {
                    db?.close()
                    db = null
                },
                onReopenDatabase = {
                    // We'll reopen it manually in the test to control migrations
                },
            )
    }

    @After
    fun tearDown() {
        db?.close()
        tempDir.deleteRecursively()

        // Clean up the actual app database created by DbOperations to avoid cross-test contamination
        val internalDbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        internalDbFile.delete()
        File("${internalDbFile.absolutePath}-wal").delete()
        File("${internalDbFile.absolutePath}-shm").delete()
    }

    @Test
    fun restoreBackup_migratesAndQueriesSuccessfully() =
        runBlocking {
            println("Testing restore for backup: $backupFileName")

            // 1. Copy backup from resources to temp file
            val userDir = System.getProperty("user.dir")
            val backupFileInProject = File(userDir, "src/test/resources/backups/$backupFileName")

            val backupResource =
                if (backupFileInProject.exists()) {
                    backupFileInProject.inputStream()
                } else {
                    javaClass.getResourceAsStream("/backups/$backupFileName")
                }
            assertThat(backupResource).isNotNull()

            val zipFile = File(tempDir, backupFileName)
            backupResource!!.use { input ->
                FileOutputStream(zipFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 2. Perform restore
            val result = backupManager.restore(zipFile)
            assertThat(result).isEqualTo(0)
            assertThat(databaseFile.exists()).isTrue()

            // 3. Open database via DbOperations and trigger migrations
            val internalDbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            internalDbFile.parentFile?.mkdirs()
            databaseFile.copyTo(internalDbFile, overwrite = true)

            val dbOps = DbOperations(context)

            // 4. Run MigrationHelper repair (as happens at startup)
            MigrationHelper.ensureBellsTableConsistent(context, dbOps)

            // 5. Verify queries
            val sessions = dbOps.readSessions()
            assertThat(sessions).isNotEmpty()
            println("  Restored ${sessions.size} sessions")

            for (session in sessions) {
                val sections = dbOps.readSections(session.id)
                // It's possible some sessions have no sections, but we shouldn't crash
                println("  Session '${session.name}' has ${sections.size} sections")

                for (section in sections) {
                    // V7 specific check: bellId should be resolved
                    assertThat(section.bellId).isGreaterThan(0)

                    val bell = dbOps.getBellById(section.bellId)
                    assertThat(bell).isNotNull()
                }

                // Check volumes too
                val volumes = dbOps.readBellVolumes(session.id)
                for (volume in volumes) {
                    assertThat(volume.bellId).isGreaterThan(0)
                    val bell = dbOps.getBellById(volume.bellId)
                    if (bell == null) {
                        println("  ERROR: Null bell for Volume ID ${volume.bellId} in session '${session.name}'")
                        println("         Volume details: bell=${volume.bell}, uri=${volume.bellUri}")
                    }
                    assertThat(bell).isNotNull()
                }
            }

            val bells = dbOps.getAllBells()
            // We should have at least the 8 built-in bells
            assertThat(bells.size).isAtLeast(8)
            println("  Database has ${bells.size} bells total after repair")
        }
}
