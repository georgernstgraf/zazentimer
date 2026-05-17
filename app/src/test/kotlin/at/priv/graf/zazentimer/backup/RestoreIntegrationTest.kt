
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreIntegrationTest {
    private lateinit var context: Context
    private lateinit var tempDir: File
    private lateinit var databaseFile: File
    private lateinit var filesDir: File
    private lateinit var backupManager: BackupManager
    private var db: AppDatabase? = null

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        at.priv.graf.zazentimer.audio.BellCollection.initialize(context)
        tempDir = File(context.cacheDir, "restore-test").apply { mkdirs() }
        databaseFile = File(tempDir, "zentimer")
        filesDir = File(tempDir, "files").apply { mkdirs() }
        
        backupManager = BackupManager(
            databaseFileProvider = { databaseFile },
            filesDirProvider = { filesDir },
            onCloseDatabase = { 
                db?.close() 
                db = null
            },
            onReopenDatabase = {
                // We'll reopen it manually in the test to control migrations
            }
        )
    }

    @After
    fun tearDown() {
        db?.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun restoreLenaBackup_migratesAndQueriesSuccessfully() = runBlocking {
        // 1. Copy backup from resources to temp file
        val userDir = System.getProperty("user.dir")
        val backupFileInProject = File(userDir, "src/test/resources/backups/zazentimer-lena-backup.zip")
        
        val backupResource = if (backupFileInProject.exists()) {
            backupFileInProject.inputStream()
        } else {
            // Fallback for different environments
            javaClass.getResourceAsStream("/backups/zazentimer-lena-backup.zip")
        }
        assertThat(backupResource).isNotNull()
        
        val zipFile = File(tempDir, "lena-backup.zip")
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
        assertThat(sessions.size).isEqualTo(5) // Lena's backup has 5 sessions

        for (session in sessions) {
            val sections = dbOps.readSections(session.id)
            assertThat(sections).isNotEmpty()
            
            for (section in sections) {
                // V7 specific check: bellId should be resolved
                assertThat(section.bellId).isGreaterThan(0)
                
                val bell = dbOps.getBellById(section.bellId)
                assertThat(bell).isNotNull()
            }
        }
        
        val bells = dbOps.getAllBells()
        assertThat(bells).hasSize(8) // 8 built-in bells
    }
}
