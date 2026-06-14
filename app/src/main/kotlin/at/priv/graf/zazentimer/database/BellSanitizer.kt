package at.priv.graf.zazentimer.database

import android.content.Context
import android.util.Log
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Bell

internal class BellSanitizer(
    private val bellDao: BellDao,
    private val sectionDao: SectionDao,
    private val sessionBellVolumeDao: SessionBellVolumeDao,
    private val context: Context,
) {
    suspend fun sanitizeBellUris() =
        withIdling {
            val allDbBells = bellDao.getAll()
            val currentBells = BellCollection.getBellList()

            syncBuiltinBellUris(allDbBells, currentBells)

            val updatedBells = bellDao.getAll()
            val demoBellId =
                BellCollection.getDemoBell()?.let { demo ->
                    updatedBells.find { it.isBuiltin && it.name == demo.getName() }?.id
                } ?: return@withIdling

            removeOrphanedBuiltinBells(updatedBells, currentBells, demoBellId)
            removeOrphanedCustomBells(updatedBells, customBellFiles(), demoBellId)
            importOrphanedBellFiles(updatedBells, customBellFiles())
        }

    private fun customBellFiles(): Set<String> =
        context
            .filesDir
            .listFiles { _, name -> name.startsWith("bell_") }
            ?.map { it.name }
            ?.toSet() ?: emptySet()

    private suspend fun syncBuiltinBellUris(
        allDbBells: List<BellEntity>,
        currentBells: List<Bell>,
    ) {
        for (bell in currentBells) {
            if (bell.uri.scheme != "android.resource") continue
            val name = bell.getName()
            val existing = allDbBells.find { it.isBuiltin && it.name == name }
            if (existing != null) {
                if (existing.uri != bell.uri.toString()) {
                    existing.uri = bell.uri.toString()
                    bellDao.update(existing)
                }
            } else {
                bellDao.insert(
                    BellEntity(
                        name = name,
                        uri = bell.uri.toString(),
                        isBuiltin = true,
                    ),
                )
            }
        }
    }

    private suspend fun removeOrphanedBuiltinBells(
        updatedBells: List<BellEntity>,
        currentBells: List<Bell>,
        demoBellId: Int,
    ) {
        val builtinNames =
            currentBells
                .filter { it.uri.scheme == "android.resource" }
                .map { it.getName() }
                .toSet()
        for (dbBell in updatedBells.filter { it.isBuiltin }) {
            if (dbBell.name !in builtinNames) {
                Log.w(TAG, "Builtin bell '${dbBell.name}' no longer exists, reassigning to demo")
                reassignBellReferences(dbBell.id, demoBellId)
                sessionBellVolumeDao.deleteByBellId(dbBell.id)
                bellDao.deleteById(dbBell.id)
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
                reassignBellReferences(bell.id, demoBellId)
                sessionBellVolumeDao.deleteByBellId(bell.id)
                bellDao.deleteById(bell.id)
            } else {
                val correctUri = "file://${context.filesDir}/$fileName"
                if (bell.uri != correctUri) {
                    bell.uri = correctUri
                    bellDao.update(bell)
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
                bellDao.insert(
                    BellEntity(
                        name = fileName.removePrefix("bell_"),
                        uri = "file://${context.filesDir}/$fileName",
                        isBuiltin = false,
                    ),
                )
            }
        }
    }

    private suspend fun reassignBellReferences(
        oldBellId: Int,
        newBellId: Int,
    ) {
        val sections = sectionDao.getSectionsByBellId(oldBellId)
        for (section in sections) {
            section.bell_id = newBellId
            sectionDao.update(section)
        }
    }

    companion object {
        private const val TAG = "ZMT_DbOperations"
    }
}
