package at.priv.graf.zazentimer.audio

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import at.priv.graf.zazentimer.backup.Streams
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.BellRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
            return try {
                val input = context.contentResolver.openInputStream(uri)
                if (input == null) {
                    Log.e(TAG, "Could not open input stream for URI")
                    return null
                }
                val output = context.openFileOutput(fileName, 0)
                input.use { inputStream ->
                    output.use { outputStream ->
                        Streams.copy(inputStream, outputStream)
                    }
                }
                val bellUri = "file://${context.filesDir}/$fileName"
                val entity =
                    BellEntity(
                        name = fileName.removePrefix("bell_"),
                        uri = bellUri,
                    )
                val id = bellRepository.insertBell(entity)
                entity.id = id.toInt()
                entity
            } catch (e: IOException) {
                Log.e(TAG, "IOException importing bell", e)
                null
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException importing bell", e)
                null
            }
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

        companion object {
            private const val TAG = "ZMT_BellImporter"
        }
    }
