package at.priv.graf.zazentimer.audio

import android.content.Context
import android.database.Cursor
import android.net.Uri
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.BellRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BellImporter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val bellRepository: BellRepository,
    ) {
        suspend fun import(uri: Uri): BellEntity? {
            val fileName = resolveBellFileName(uri)
            var success = false
            return try {
                doImport(uri, fileName).also { success = true }
            } catch (e: IOException) {
                throw BellImportException(e.message ?: "IOException importing bell", e)
            } catch (e: SecurityException) {
                throw BellImportException(e.message ?: "SecurityException importing bell", e)
            } finally {
                if (!success) {
                    File(context.filesDir, fileName).delete()
                }
            }
        }

        private suspend fun doImport(
            uri: Uri,
            fileName: String,
        ): BellEntity {
            val input =
                context.contentResolver.openInputStream(uri)
                    ?: throw BellImportException("Could not open input stream for URI")
            input.use { inputStream ->
                val output = context.openFileOutput(fileName, 0)
                output.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val bellUri = "file://${context.filesDir}/$fileName"
            BellValidator.validate(context, Uri.parse(bellUri))
            val entity =
                BellEntity(
                    name = fileName.removePrefix("bell_"),
                    uri = bellUri,
                )
            val id = bellRepository.insertBell(entity)
            entity.id = id.toInt()
            return entity
        }

        private fun resolveBellFileName(uri: Uri): String {
            var str = "bell_unnamed"
            if (uri.scheme == "content") {
                val query: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                if (query != null && query.count != 0) {
                    val columnIndex = query.getColumnIndex("_display_name")
                    if (columnIndex >= 0) {
                        query.moveToFirst()
                        val colVal = query.getString(columnIndex) ?: ""
                        str = "bell_$colVal"
                    } else {
                        val segment = uri.lastPathSegment ?: ""
                        str = "bell_$segment"
                    }
                }
                query?.close()
            } else {
                uri.lastPathSegment?.let { str = "bell_$it" }
            }
            return str
        }
    }
