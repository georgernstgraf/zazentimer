package at.priv.graf.zazentimer.database

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import at.priv.graf.zazentimer.audio.BellImportException
import at.priv.graf.zazentimer.audio.BellValidator
import at.priv.graf.zazentimer.audio.BuiltinBells
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class BellSanitizer
    @Inject
    constructor(
        private val databaseOwner: DatabaseOwner,
        @ApplicationContext private val context: Context,
    ) {
        private fun bellDao(): BellDao = databaseOwner.bellDao()

        private fun sectionDao(): SectionDao = databaseOwner.sectionDao()

        private fun sessionBellVolumeDao(): SessionBellVolumeDao = databaseOwner.sessionBellVolumeDao()

        private fun appDb(): AppDatabase = databaseOwner.appDatabase()

        private data class BuiltinDefinition(
            val name: String,
            val uri: String,
        )

        private fun builtinDefinitions(): List<BuiltinDefinition> =
            BuiltinBells.definitions().map { seed ->
                BuiltinDefinition(
                    name = context.getString(seed.nameResId),
                    uri = BuiltinBells.resourceUri(context, seed.rawResId),
                )
            }

        suspend fun sanitizeBellUris() =
            withIdling {
                appDb().withTransaction {
                    val allDbBells = bellDao().getAll()
                    val definitions = builtinDefinitions()

                    syncBuiltinBellUris(allDbBells, definitions)

                    val updatedBells = bellDao().getAll()
                    val demoBellName = context.getString(BuiltinBells.DEMO_BELL_NAME_RES)
                    val demoBellId =
                        updatedBells.find { it.isBuiltin && it.name == demoBellName }?.id
                            ?: return@withTransaction

                    removeOrphanedBuiltinBells(updatedBells, definitions, demoBellId)
                    removeOrphanedCustomBells(updatedBells, customBellFiles(), demoBellId)
                    importOrphanedBellFiles(updatedBells, customBellFiles())
                }
            }

        private fun customBellFiles(): Set<String> =
            context
                .filesDir
                .listFiles { _, name -> name.startsWith("bell_") }
                ?.map { it.name }
                ?.toSet() ?: emptySet()

        private suspend fun syncBuiltinBellUris(
            allDbBells: List<BellEntity>,
            definitions: List<BuiltinDefinition>,
        ) {
            for (definition in definitions) {
                val existing = allDbBells.find { it.isBuiltin && it.name == definition.name }
                if (existing != null) {
                    if (existing.uri != definition.uri) {
                        existing.uri = definition.uri
                        bellDao().update(existing)
                    }
                } else {
                    bellDao().insert(
                        BellEntity(
                            name = definition.name,
                            uri = definition.uri,
                            isBuiltin = true,
                        ),
                    )
                }
            }
        }

        private suspend fun removeOrphanedBuiltinBells(
            updatedBells: List<BellEntity>,
            definitions: List<BuiltinDefinition>,
            demoBellId: Int,
        ) {
            val builtinNames = definitions.map { it.name }.toSet()
            for (dbBell in updatedBells.filter { it.isBuiltin }) {
                if (dbBell.name !in builtinNames) {
                    Log.w(TAG, "Builtin bell '${dbBell.name}' no longer exists, reassigning to demo")
                    appDb().withTransaction {
                        reassignBellReferences(dbBell.id, demoBellId)
                        sessionBellVolumeDao().deleteByBellId(dbBell.id)
                        bellDao().deleteById(dbBell.id)
                    }
                }
            }
        }

        private suspend fun removeOrphanedCustomBells(
            updatedBells: List<BellEntity>,
            customBellFiles: Set<String>,
            demoBellId: Int,
        ) {
            for (bell in updatedBells.filter { !it.isBuiltin }) {
                val fileName = bell.uri.substringAfterLast("/")
                if (fileName !in customBellFiles) {
                    Log.w(TAG, "Custom bell file missing ($fileName), removing from DB")
                    appDb().withTransaction {
                        reassignBellReferences(bell.id, demoBellId)
                        sessionBellVolumeDao().deleteByBellId(bell.id)
                        bellDao().deleteById(bell.id)
                    }
                } else {
                    val correctUri = "file://${context.filesDir}/$fileName"
                    if (bell.uri != correctUri) {
                        bell.uri = correctUri
                        bellDao().update(bell)
                    }
                }
            }
        }

        private suspend fun importOrphanedBellFiles(
            updatedBells: List<BellEntity>,
            customBellFiles: Set<String>,
        ) {
            val dbCustomFilenames =
                updatedBells
                    .filter { !it.isBuiltin }
                    .map { it.uri.substringAfterLast("/") }
                    .toSet()
            for (fileName in customBellFiles) {
                if (fileName !in dbCustomFilenames) {
                    Log.i(TAG, "Found orphaned custom bell file ($fileName), adding to DB")
                    val bellUri = "file://${context.filesDir}/$fileName"
                    try {
                        BellValidator.validate(context, Uri.parse(bellUri))
                        bellDao().insert(
                            BellEntity(
                                name = fileName.removePrefix("bell_"),
                                uri = bellUri,
                                isBuiltin = false,
                            ),
                        )
                    } catch (e: BellImportException) {
                        Log.w(TAG, "Orphaned bell file $fileName is invalid audio, deleting", e)
                        File(context.filesDir, fileName).delete()
                    }
                }
            }
        }

        private suspend fun reassignBellReferences(
            oldBellId: Int,
            newBellId: Int,
        ) {
            val sections = sectionDao().getSectionsByBellId(oldBellId)
            for (section in sections) {
                section.bell_id = newBellId
                sectionDao().update(section)
            }
        }

        companion object {
            private const val TAG = "ZMT_BellSanitizer"
        }
    }
