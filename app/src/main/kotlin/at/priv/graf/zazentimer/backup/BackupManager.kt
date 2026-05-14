package at.priv.graf.zazentimer.backup

import android.util.Log
import at.priv.graf.zazentimer.database.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.SecurityException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BackupManager(
    private val databaseFileProvider: () -> File,
    private val filesDirProvider: () -> File,
    private val onCloseDatabase: () -> Unit,
    private val onReopenDatabase: () -> Unit,
) {
    fun backup(outputStream: OutputStream): Boolean {
        var failed = false
        try {
            onCloseDatabase()
            val zos = ZipOutputStream(outputStream)
            val databaseFile = databaseFileProvider()
            val ze = ZipEntry(databaseFile.name)
            zos.putNextEntry(ze)
            if (!sendFile(databaseFile, zos)) {
                failed = true
            }
            zos.closeEntry()
            if (!addFilesDirToZip(zos, filesDirProvider())) {
                failed = true
            }
            zos.close()
            safeReopenDatabase()
        } catch (e: IOException) {
            Log.e(TAG, "Backup failed", e)
            failed = true
            safeReopenDatabase()
        } catch (e: ZipException) {
            Log.e(TAG, "Backup failed", e)
            failed = true
            safeReopenDatabase()
        } catch (e: SecurityException) {
            Log.e(TAG, "Backup failed", e)
            failed = true
            safeReopenDatabase()
        }
        return !failed
    }

    fun restore(zipFile: File): Int {
        var result = 0
        val databaseName = databaseFileProvider().name
        try {
            val zf = ZipFile(zipFile)
            result = restoreEntries(zf, databaseName)
            zf.close()
        } catch (e: IOException) {
            Log.e(TAG, "Restore failed", e)
            result = 2
        } catch (e: ZipException) {
            Log.e(TAG, "Restore failed", e)
            result = 2
        } catch (e: SecurityException) {
            Log.e(TAG, "Restore failed", e)
            result = 2
        }
        return result
    }

    @Suppress("TooGenericExceptionCaught")
    private fun safeReopenDatabase() {
        try {
            onReopenDatabase()
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to reopen database", e)
        }
    }

    private fun restoreEntries(
        zf: ZipFile,
        databaseName: String,
    ): Int {
        var result = 0
        val entries = zf.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name == databaseName) {
                val entryBytes = zf.getInputStream(entry).use { it.readBytes() }
                val dbVersion = readDatabaseVersion(entryBytes)
                if (dbVersion > AppDatabase.VERSION_6) {
                    return ERROR_VERSION_TOO_HIGH
                }
                onCloseDatabase()
                if (!receiveBytes(entryBytes, databaseFileProvider())) {
                    result = 2
                }
                onReopenDatabase()
            } else if (!receiveFile(zf.getInputStream(entry), File(filesDirProvider(), entry.name))) {
                result = 2
            }
        }
        return result
    }

    @Suppress("MagicNumber")
    private fun readDatabaseVersion(bytes: ByteArray): Int {
        val magic = "SQLite format 3\u0000".toByteArray()
        val isValidSqlite =
            bytes.size >= 68 &&
                magic.indices.none { bytes[it] != magic[it] }
        if (!isValidSqlite) return 0
        val off = 60
        return ((bytes[off].toInt() and 0xFF) shl 24) or
            ((bytes[off + 1].toInt() and 0xFF) shl 16) or
            ((bytes[off + 2].toInt() and 0xFF) shl 8) or
            (bytes[off + 3].toInt() and 0xFF)
    }

    private fun receiveBytes(
        bytes: ByteArray,
        file: File,
    ): Boolean {
        var success = true
        try {
            val fos = FileOutputStream(file)
            fos.write(bytes)
            fos.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to receive bytes", e)
            success = false
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to receive bytes", e)
            success = false
        }
        return success
    }

    private fun addFilesDirToZip(
        zos: ZipOutputStream,
        filesDir: File,
    ): Boolean {
        val listFiles = filesDir.listFiles { f -> f.name != "InstantRun" } ?: return true
        var fileFailed = false
        for (file in listFiles) {
            val ze = ZipEntry(file.name)
            zos.putNextEntry(ze)
            if (!sendFile(file, zos)) {
                fileFailed = true
            }
            zos.closeEntry()
        }
        return !fileFailed
    }

    companion object {
        internal const val BUFFER_SIZE = 32768

        internal const val ERROR_VERSION_TOO_HIGH = 3

        private const val TAG = "ZMT_BackupManager"

        internal fun sendFile(
            file: File,
            outputStream: OutputStream,
        ): Boolean {
            var success = true
            try {
                val fis = FileInputStream(file)
                val buf = ByteArray(BUFFER_SIZE)
                var read = fis.read(buf)
                while (read > 0) {
                    outputStream.write(buf, 0, read)
                    read = fis.read(buf)
                }
                fis.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send file", e)
                success = false
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to send file", e)
                success = false
            }
            return success
        }

        internal fun receiveFile(
            inputStream: InputStream,
            file: File,
        ): Boolean {
            var success = true
            try {
                val fos = FileOutputStream(file)
                val buf = ByteArray(BUFFER_SIZE)
                var read = inputStream.read(buf)
                while (read > 0) {
                    fos.write(buf, 0, read)
                    read = inputStream.read(buf)
                }
                fos.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to receive file", e)
                success = false
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to receive file", e)
                success = false
            }
            return success
        }
    }
}
