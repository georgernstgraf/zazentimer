package at.priv.graf.zazentimer.backup

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    private lateinit var tempDir: File
    private lateinit var databaseDir: File
    private lateinit var filesDir: File
    private lateinit var databaseFile: File
    private var closeCalled = false
    private var reopenCalled = false
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        tempDir = createTempDir()
        databaseDir = File(tempDir, "databases").apply { mkdirs() }
        filesDir = File(tempDir, "files").apply { mkdirs() }
        databaseFile = File(databaseDir, "zentimer")
        closeCalled = false
        reopenCalled = false
        backupManager = BackupManager(
            databaseFileProvider = { databaseFile },
            filesDirProvider = { filesDir },
            onCloseDatabase = { closeCalled = true },
            onReopenDatabase = { reopenCalled = true }
        )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun backup_emptyFilesDir_producesZipWithOnlyDatabase() {
        databaseFile.writeText("db-content")
        val backupFile = File(tempDir, "backup.zip")

        val result = backupManager.backup(FileOutputStream(backupFile))

        assertThat(result).isTrue()
        assertThat(closeCalled).isTrue()
        assertThat(reopenCalled).isTrue()
        assertThat(backupFile.exists()).isTrue()
        assertThat(backupFile.length()).isGreaterThan(0L)
    }

    @Test
    fun backup_withFiles_producesZipWithAllFiles() {
        databaseFile.writeText("db-content")
        File(filesDir, "prefs1").writeText("pref-content-1")
        File(filesDir, "prefs2").writeText("pref-content-2")
        val backupFile = File(tempDir, "backup.zip")

        val result = backupManager.backup(FileOutputStream(backupFile))

        assertThat(result).isTrue()
        val entries = java.util.zip.ZipFile(backupFile).use { zf ->
            zf.entries().toList().map { it.name }
        }
        assertThat(entries).containsExactly("zentimer", "prefs1", "prefs2")
    }

    @Test
    fun backup_skipsInstantRunDirectory() {
        databaseFile.writeText("db-content")
        val instantRun = File(filesDir, "InstantRun")
        instantRun.mkdirs()
        File(filesDir, "goodfile").writeText("data")
        val backupFile = File(tempDir, "backup.zip")

        backupManager.backup(FileOutputStream(backupFile))

        val entries = java.util.zip.ZipFile(backupFile).use { zf ->
            zf.entries().toList().map { it.name }
        }
        assertThat(entries).containsExactly("zentimer", "goodfile")
    }

    @Test
    fun backup_callsCloseBeforeAndReopenAfter() {
        databaseFile.writeText("db")
        val backupFile = File(tempDir, "backup.zip")

        backupManager.backup(FileOutputStream(backupFile))

        assertThat(closeCalled).isTrue()
        assertThat(reopenCalled).isTrue()
    }

    @Test
    fun backup_missingDatabaseFile_stillReturnsFalse() {
        val backupFile = File(tempDir, "backup.zip")

        val result = backupManager.backup(FileOutputStream(backupFile))

        assertThat(result).isFalse()
        assertThat(reopenCalled).isTrue()
    }

    @Test
    fun restore_roundTrip_preservesData() {
        databaseFile.writeText("original-db-content")
        File(filesDir, "config").writeText("config-data")
        val backupFile = File(tempDir, "backup.zip")

        backupManager.backup(FileOutputStream(backupFile))

        databaseFile.writeText("overwritten")
        File(filesDir, "config").writeText("overwritten")

        val result = backupManager.restore(backupFile)

        assertThat(result).isEqualTo(0)
        assertThat(databaseFile.readText()).isEqualTo("original-db-content")
        assertThat(File(filesDir, "config").readText()).isEqualTo("config-data")
    }

    @Test
    fun restore_noFilesInBackupZip_restoresDatabaseOnly() {
        databaseFile.writeText("db-data")
        val backupFile = File(tempDir, "backup.zip")
        backupManager.backup(FileOutputStream(backupFile))

        databaseFile.writeText("corrupted")

        backupManager.restore(backupFile)

        assertThat(databaseFile.readText()).isEqualTo("db-data")
    }

    @Test
    fun restore_callsCloseAndReopenForDatabaseEntry() {
        databaseFile.writeText("db")
        val backupFile = File(tempDir, "backup.zip")
        backupManager.backup(FileOutputStream(backupFile))

        closeCalled = false
        reopenCalled = false

        backupManager.restore(backupFile)

        assertThat(closeCalled).isTrue()
        assertThat(reopenCalled).isTrue()
    }

    @Test
    fun restore_corruptedZipFile_returnsError() {
        val corruptedFile = File(tempDir, "corrupted.zip")
        corruptedFile.writeText("this is not a valid zip file at all")

        val result = backupManager.restore(corruptedFile)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun restore_nonExistentFile_returnsError() {
        val missingFile = File(tempDir, "nonexistent.zip")

        val result = backupManager.restore(missingFile)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun restore_emptyZip_returnsSuccess() {
        val emptyZip = File(tempDir, "empty.zip")
        ZipOutputStream(FileOutputStream(emptyZip)).use { it.close() }

        val result = backupManager.restore(emptyZip)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun restore_zipWithMissingEntries_stillSucceeds() {
        val zipFile = File(tempDir, "partial.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry = ZipEntry("otherfile")
            zos.putNextEntry(entry)
            zos.write("other-data".toByteArray())
            zos.closeEntry()
        }

        val result = backupManager.restore(zipFile)

        assertThat(result).isEqualTo(0)
        assertThat(File(filesDir, "otherfile").readText()).isEqualTo("other-data")
    }

    @Test
    fun restore_binaryData_preservedExactly() {
        val binaryData = ByteArray(1024) { it.toByte() }
        databaseFile.writeBytes(binaryData)
        val backupFile = File(tempDir, "backup.zip")

        backupManager.backup(FileOutputStream(backupFile))

        databaseFile.writeBytes(ByteArray(1024) { 0 })

        backupManager.restore(backupFile)

        assertThat(databaseFile.readBytes()).isEqualTo(binaryData)
    }

    @Test
    fun sendFile_readsEntireFile() {
        val file = File(tempDir, "testfile")
        file.writeText("hello world")
        val output = java.io.ByteArrayOutputStream()

        val result = BackupManager.sendFile(file, output)

        assertThat(result).isTrue()
        assertThat(output.toByteArray()).isEqualTo("hello world".toByteArray())
    }

    @Test
    fun sendFile_nonExistentFile_returnsFalse() {
        val missing = File(tempDir, "missing")
        val output = java.io.ByteArrayOutputStream()

        val result = BackupManager.sendFile(missing, output)

        assertThat(result).isFalse()
    }

    @Test
    fun receiveFile_writesEntireStream() {
        val target = File(tempDir, "output")
        val input = "stream data".byteInputStream()

        val result = BackupManager.receiveFile(input, target)

        assertThat(result).isTrue()
        assertThat(target.readText()).isEqualTo("stream data")
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.nanoTime()}")
        dir.mkdirs()
        dir.deleteOnExit()
        return dir
    }
}
