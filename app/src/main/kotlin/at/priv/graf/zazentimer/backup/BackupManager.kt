package at.priv.graf.zazentimer.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
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
            val filesDir = filesDirProvider()
            val listFiles = filesDir.listFiles { f -> f.name != "InstantRun" }
            if (listFiles != null) {
                for (file in listFiles) {
                    val ze2 = ZipEntry(file.name)
                    zos.putNextEntry(ze2)
                    if (!sendFile(file, zos)) {
                        failed = true
                    }
                    zos.closeEntry()
                }
            }
            zos.close()
            onReopenDatabase()
        } catch (e: Exception) {
            failed = true
            try {
                onReopenDatabase()
            } catch (_: Exception) {
            }
        }
        return !failed
    }

    fun restore(zipFile: File): Int {
        var failed = false
        val databaseName = databaseFileProvider().name
        try {
            val zf = ZipFile(zipFile)
            val entries: Enumeration<out ZipEntry> = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name == databaseName) {
                    onCloseDatabase()
                    if (!receiveFile(zf.getInputStream(entry), databaseFileProvider())) {
                        failed = true
                    }
                    onReopenDatabase()
                } else if (!receiveFile(zf.getInputStream(entry), File(filesDirProvider(), entry.name))) {
                    failed = true
                }
            }
            zf.close()
        } catch (e: Exception) {
            failed = true
        }
        return if (failed) 2 else 0
    }

    companion object {
        internal fun sendFile(
            file: File,
            outputStream: OutputStream,
        ): Boolean {
            try {
                val fis = FileInputStream(file)
                val buf = ByteArray(32768)
                var read = fis.read(buf)
                while (read > 0) {
                    outputStream.write(buf, 0, read)
                    read = fis.read(buf)
                }
                fis.close()
                return true
            } catch (e: Exception) {
                return false
            }
        }

        internal fun receiveFile(
            inputStream: InputStream,
            file: File,
        ): Boolean {
            try {
                val fos = FileOutputStream(file)
                val buf = ByteArray(32768)
                var read = inputStream.read(buf)
                while (read > 0) {
                    fos.write(buf, 0, read)
                    read = inputStream.read(buf)
                }
                fos.close()
                return true
            } catch (e: Exception) {
                return false
            }
        }
    }
}
