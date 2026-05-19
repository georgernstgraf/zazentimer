package at.priv.graf.zazentimer

import android.content.Context
import android.util.Log
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.DbOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MigrationHelper(
    private val dbOperations: DbOperations,
    private val scope: CoroutineScope,
) {
    fun convertFromOldVersions() {
        // Bell URI repair runs EVERY startup to catch stale backup URIs
        Log.d(TAG, "repairing bell URIs...")
        scope.launch { convertBellIndices() }
        Log.d(TAG, "done repairing bell URIs")
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun convertBellIndices() {
        for (session in dbOperations.readSessions()) {
            for (section in dbOperations.readSections(session.id)) {
                convertSectionBellIndex(section)
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun convertSectionBellIndex(section: Section) {
        val bellUri = section.bellUri
        if (bellUri == null || bellUri.trim().isEmpty()) {
            val bell = BellCollection.getBell(section.bell)
            if (bell != null) {
                section.bellUri = bell.uri.toString()
            } else {
                section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: return
            }
            section.bell = BELL_INDEX_NONE
            dbOperations.updateSection(section)
        } else if (section.bell == BELL_INDEX_LEGACY_DEFAULT) {
            section.bell = BELL_INDEX_NONE
            section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: return
            dbOperations.updateSection(section)
        } else if (section.bell == BELL_INDEX_NONE && BellCollection.getBellForSection(section) == null) {
            section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: return
            dbOperations.updateSection(section)
        }
    }

    companion object {
        private const val TAG = "ZMT_MigrationHelper"
        private const val BELL_INDEX_NONE = -2
        private const val BELL_INDEX_LEGACY_DEFAULT = -1

        @Suppress("LongMethod", "CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
        suspend fun ensureBellsTableConsistent(
            context: Context,
            db: DbOperations,
        ) {
            // 1. Seed 8 built-in bells
            val builtinResNames =
                listOf(
                    "bell1",
                    "bell2",
                    "dharma107",
                    "dharmaschwarz88",
                    "shomyo90",
                    "tang164",
                    "tib230",
                    "zen97",
                )
            for (i in builtinResNames.indices) {
                val bell = BellCollection.getBell(i) ?: continue
                db.insertBell(
                    BellEntity(
                        name = bell.getName(),
                        uri = bell.uri.toString(),
                        isBuiltin = true,
                        resourceName = builtinResNames[i],
                    ),
                )
            }

            // 2. Sync custom bells from filesDir
            val filesDir = context.filesDir
            val customFiles = filesDir.listFiles { _, name -> name.startsWith("bell_") }
            if (customFiles != null) {
                for (file in customFiles) {
                    val uri = "file://${file.absolutePath}"
                    db.insertBell(
                        BellEntity(
                            name = file.name.removePrefix("bell_"),
                            uri = uri,
                        ),
                    )
                }
            }

            // 3. Resolve built-in bell URIs and names
            for (entity in db.getBuiltinBells()) {
                val resName = entity.resourceName ?: continue
                val idx = builtinResNames.indexOf(resName)
                if (idx >= 0) {
                    val bell = BellCollection.getBell(idx) ?: continue
                    if (entity.uri != bell.uri.toString() || entity.name != bell.getName()) {
                        entity.uri = bell.uri.toString()
                        entity.name = bell.getName()
                        db.updateBell(entity)
                    }
                }
            }

            // 3b. Mark bells as built-in if their URI matches a BellCollection built-in bell
            var anyBellsUpdated = false
            for (entity in db.getAllBells()) {
                if (entity.isBuiltin) continue
                val bell = BellCollection.getBellList().find { it.uri.toString() == entity.uri }
                if (bell != null) {
                    val idx = BellCollection.getBellList().indexOf(bell)
                    if (idx >= 0 && idx < builtinResNames.size) {
                        entity.isBuiltin = true
                        entity.resourceName = builtinResNames[idx]
                        entity.name = bell.getName()
                        db.updateBell(entity)
                        anyBellsUpdated = true
                    }
                }
            }

            // 4. Fix stale bell URIs (backup import case)
            val allBells = BellCollection.getBellList()
            for (entity in db.getNonBuiltinBells()) {
                val found = allBells.find { it.uri.toString() == entity.uri }
                if (found == null) {
                    val demo = BellCollection.getDemoBell() ?: allBells.firstOrNull() ?: continue
                    entity.uri = demo.uri.toString()
                    entity.name = demo.getName()
                    db.updateBell(entity)
                    anyBellsUpdated = true
                }
            }

            // 5. Deduplicate bells with same URI
            val allBellEntities = db.getAllBells()
            val byUri = allBellEntities.groupBy { it.uri }
            for ((_, group) in byUri) {
                if (group.size <= 1) continue
                val keep = group.first()
                for (remove in group.drop(1)) {
                    updateSectionBellId(db, remove._id, keep._id)
                    updateVolumeBellId(db, remove._id, keep._id)
                    db.deleteBellById(remove._id)
                    anyBellsUpdated = true
                }
            }

            // 5.5 If we updated any bells, we need to refresh our list before resolving IDs
            val finalBellEntities = if (anyBellsUpdated) db.getAllBells() else allBellEntities

            // 6. Resolve bellId for any entries still at 0
            resolveUnresolvedBellIds(db, finalBellEntities)

            // 7. Repair stale section URIs (e.g. from old backup with different package name)
            repairStaleSectionUris(db, finalBellEntities)
        }

        @Suppress("LoopWithTooManyJumpStatements")
        private suspend fun repairStaleSectionUris(
            db: DbOperations,
            allBellEntities: List<BellEntity>,
        ) {
            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bellId <= 0) continue
                    val currentBell = allBellEntities.find { it._id == section.bellId } ?: continue
                    if (section.bellUri == null || BellCollection.getBellForSection(section) == null) {
                        section.bellUri = currentBell.uri
                        section.bell = BELL_INDEX_NONE
                        db.updateSection(section)
                    }
                }
            }
        }

        suspend fun repairBellUris(db: DbOperations) {
            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bell == BELL_INDEX_NONE && BellCollection.getBellForSection(section) == null) {
                        section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: continue
                        db.updateSection(section)
                    }
                }
            }
        }

        private suspend fun updateSectionBellId(
            db: DbOperations,
            fromId: Int,
            toId: Int,
        ) {
            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bellId == fromId) {
                        section.bellId = toId
                        db.updateSection(section)
                    }
                }
            }
        }

        private suspend fun updateVolumeBellId(
            db: DbOperations,
            fromId: Int,
            toId: Int,
        ) {
            for (session in db.readSessions()) {
                val volumes = db.readBellVolumes(session.id)
                var changed = false
                for (bv in volumes) {
                    if (bv.bellId == fromId) {
                        bv.bellId = toId
                        changed = true
                    }
                }
                if (changed) {
                    // Deduplicate after remapping
                    val deduplicated =
                        volumes.groupBy { it.bellId }.map { (bellId, group) ->
                            if (group.size > 1) {
                                val avgVolume = group.map { it.volume }.average().toInt()
                                group.first().apply { volume = avgVolume }
                            } else {
                                group.first()
                            }
                        }
                    db.saveBellVolumes(session.id, deduplicated)
                }
            }
        }

        @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
        private suspend fun resolveUnresolvedBellIds(
            db: DbOperations,
            allBellEntities: List<BellEntity>,
        ) {
            val demoEntity =
                allBellEntities.firstOrNull { it.resourceName == "bell2" }
                    ?: allBellEntities.firstOrNull { it.isBuiltin }
                    ?: allBellEntities.firstOrNull()
            val demoId = demoEntity?._id ?: 0

            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bellId <= 0 && !section.bellUri.isNullOrEmpty()) {
                        val matched = allBellEntities.find { it.uri == section.bellUri }
                        section.bellId = matched?._id ?: demoId
                        if (section.bellId > 0) {
                            db.updateSection(section)
                        }
                    } else if (section.bellId <= 0) {
                        section.bellId = demoId
                        if (section.bellId > 0) {
                            db.updateSection(section)
                        }
                    }
                }

                val volumes = db.readBellVolumes(session.id)
                var changed = false
                for (bv in volumes) {
                    if (bv.bellId <= 0 && !bv.bellUri.isNullOrEmpty()) {
                        val matched = allBellEntities.find { it.uri == bv.bellUri }
                        // Special fix for backup imports: if URI is not found, force to demoId
                        bv.bellId = matched?._id ?: demoId
                        changed = true
                    } else if (bv.bellId <= 0) {
                        bv.bellId = demoId
                        changed = true
                    } else if (bv.bellId > 0 && allBellEntities.none { it._id == bv.bellId }) {
                        // The ID exists but points to a deleted/non-existent bell (due to deduplication)
                        val matched = allBellEntities.find { it.uri == bv.bellUri }
                        bv.bellId = matched?._id ?: demoId
                        changed = true
                    }
                }
                if (changed) {
                    db.saveBellVolumes(session.id, volumes)
                }
            }
        }
    }
}
