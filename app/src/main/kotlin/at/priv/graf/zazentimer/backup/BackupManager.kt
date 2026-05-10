package at.priv.graf.zazentimer.backup

import android.util.Log
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
        var failed = false
        val databaseName = databaseFileProvider().name
        try {
            val zf = ZipFile(zipFile)
            if (!restoreEntries(zf, databaseName)) {
                failed = true
            }
            zf.close()
        } catch (e: IOException) {
            Log.e(TAG, "Restore failed", e)
            failed = true
        } catch (e: ZipException) {
            Log.e(TAG, "Restore failed", e)
            failed = true
        } catch (e: SecurityException) {
            Log.e(TAG, "Restore failed", e)
            failed = true
        }
        return if (failed) 2 else 0
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
    ): Boolean {
        var entryFailed = false
        val entries = zf.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name == databaseName) {
                onCloseDatabase()
                if (!receiveFile(zf.getInputStream(entry), databaseFileProvider())) {
                    entryFailed = true
                }
                onReopenDatabase()
            } else if (!receiveFile(zf.getInputStream(entry), File(filesDirProvider(), entry.name))) {
                entryFailed = true
            }
        }
        return !entryFailed
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
