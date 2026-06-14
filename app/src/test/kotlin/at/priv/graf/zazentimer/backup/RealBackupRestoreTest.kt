package at.priv.graf.zazentimer.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import at.priv.graf.zazentimer.database.AppDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealBackupRestoreTest {
    private lateinit var filesDir: File
    private lateinit var databaseDir: File
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        filesDir = context.filesDir.apply { mkdirs() }
        databaseDir = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile!!.apply { mkdirs() }
        backupManager =
            BackupManager(
                databaseFileProvider = { File(databaseDir, AppDatabase.DATABASE_NAME) },
                filesDirProvider = { filesDir },
                onCloseDatabase = { },
                onReopenDatabase = { },
            )
    }

    @Test
    fun restore_realV2Backup_returnsSuccess() {
        val result = backupManager.restore(copyResourceZip())

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun restore_realV2Backup_databaseHasCorrectRowCounts() {
        backupManager.restore(copyResourceZip())

        val dbFile = File(databaseDir, AppDatabase.DATABASE_NAME)
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertThat(count(db, "SELECT COUNT(*) FROM sessions")).isEqualTo(4)
            assertThat(count(db, "SELECT COUNT(*) FROM sections")).isEqualTo(17)
            assertThat(count(db, "SELECT COUNT(*) FROM bells")).isEqualTo(9)
            assertThat(count(db, "SELECT COUNT(*) FROM session_bell_volumes")).isEqualTo(6)
        }
    }

    @Test
    fun restore_realV2Backup_bellFileExtracted() {
        backupManager.restore(copyResourceZip())

        assertThat(File(filesDir, "bell_blah.aac").exists()).isTrue()
    }

    @Test
    fun restore_realV2Backup_fkIntegrityIntact() {
        backupManager.restore(copyResourceZip())

        val dbFile = File(databaseDir, AppDatabase.DATABASE_NAME)
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val orphanedSections =
                count(
                    db,
                    "SELECT COUNT(*) FROM sections s LEFT JOIN sessions " +
                        "ON s.fk_session = sessions.id WHERE sessions.id IS NULL",
                )
            assertThat(orphanedSections).isEqualTo(0)
            val orphanedVolumes =
                count(
                    db,
                    "SELECT COUNT(*) FROM session_bell_volumes v LEFT JOIN bells " +
                        "ON v.bellId = bells.id WHERE bells.id IS NULL",
                )
            assertThat(orphanedVolumes).isEqualTo(0)
        }
    }

    @Test
    fun restore_realV2Backup_versionMatchesBackup() {
        backupManager.restore(copyResourceZip())

        val dbFile = File(databaseDir, AppDatabase.DATABASE_NAME)
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertThat(db.version).isEqualTo(2)
        }
    }

    private fun copyResourceZip(): File {
        val zipFile = File.createTempFile("zentimer_backup_room_v2", ".zip")
        javaClass.getResourceAsStream("/backups/zentimer_backup_room_v2.zip")!!.use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }
        return zipFile
    }

    private fun count(
        db: SQLiteDatabase,
        sql: String,
    ): Int {
        db.rawQuery(sql, null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }
}
