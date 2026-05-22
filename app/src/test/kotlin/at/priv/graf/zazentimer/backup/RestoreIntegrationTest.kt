package at.priv.graf.zazentimer.backup

import android.content.Context
import at.priv.graf.zazentimer.database.AppDatabase
import com.google.common.truth.Truth.assertThat
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

    private val oldBackupFiles =
        listOf(
            "zazentimer-georg-0516-backup_old.zip",
            "zazentimer-georg-backup_01_old.zip",
            "zazentimer-georg-backup_02_old.zip",
            "zazentimer-georg-prod-backup_old.zip",
            "zazentimer-lena-backup_old.zip",
        )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        tempDir = File(context.cacheDir, "restore-test-${System.currentTimeMillis()}").apply { mkdirs() }
        databaseFile = File(tempDir, AppDatabase.DATABASE_NAME)
        filesDir = File(tempDir, "files").apply { mkdirs() }

        backupManager =
            BackupManager(
                databaseFileProvider = { databaseFile },
                filesDirProvider = { filesDir },
                onCloseDatabase = { },
                onReopenDatabase = { },
            )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun oldFormatBackup_doesNotRestoreDatabase() {
        for (fileName in oldBackupFiles) {
            val zipFile = resolveBackup(fileName) ?: continue
            val result = backupManager.restore(zipFile)
            assertThat(result).isEqualTo(1)
            assertThat(databaseFile.exists()).isFalse()
            zipFile.delete()
        }
    }

    private fun resolveBackup(fileName: String): File? {
        val userDir = System.getProperty("user.dir")
        val backupFileInProject = File(userDir, "src/test/resources/backups/$fileName")
        if (backupFileInProject.exists()) {
            return backupFileInProject
        }
        val resourceStream = javaClass.getResourceAsStream("/backups/$fileName")
        return if (resourceStream != null) {
            val zipFile = File(tempDir, fileName)
            resourceStream.use { input -> FileOutputStream(zipFile).use { output -> input.copyTo(output) } }
            zipFile
        } else {
            null
        }
    }
}
