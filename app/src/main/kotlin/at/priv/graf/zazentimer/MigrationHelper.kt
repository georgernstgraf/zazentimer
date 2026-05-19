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
        Log.d(TAG, "repairing bell IDs...")
        scope.launch { convertBellIds() }
        Log.d(TAG, "done repairing bell IDs")
    }

    private suspend fun convertBellIds() {
        for (session in dbOperations.readSessions()) {
            for (section in dbOperations.readSections(session.id)) {
                convertSectionBellId(section)
            }
        }
    }

    private suspend fun convertSectionBellId(section: Section) {
        if (section.bellId <= 0) {
            val demo = BellCollection.getDemoBell()
            if (demo != null) {
                val entity = dbOperations.getBellByUri(demo.uri.toString())
                section.bellId = entity?._id ?: 0
            }
            if (section.bellId > 0) {
                dbOperations.updateSection(section)
            }
        } else if (dbOperations.getBellById(section.bellId) == null) {
            val demo = BellCollection.getDemoBell()
            if (demo != null) {
                val entity = dbOperations.getBellByUri(demo.uri.toString())
                section.bellId = entity?._id ?: 0
            }
            if (section.bellId > 0) {
                dbOperations.updateSection(section)
            }
        }
    }

    companion object {
        private const val TAG = "ZMT_MigrationHelper"

        @Suppress("LongMethod", "CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
        suspend fun ensureBellsTableConsistent(
            context: Context,
            db: DbOperations,
        ) {
            // 1. Seed 8 built-in bells (idempotent: INSERT OR IGNORE)
            for (i in 0 until BellCollection.getBellList().size) {
                val bell = BellCollection.getBell(i) ?: continue
                db.insertBell(
                    BellEntity(
                        name = bell.getName(),
                        uri = bell.uri.toString(),
                        isBuiltin = true,
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
                val bell = BellCollection.getBellList().find { it.uri.toString() == entity.uri }
                if (bell != null && (entity.name != bell.getName())) {
                    entity.name = bell.getName()
                    db.updateBell(entity)
                }
            }

            // 3b. Mark bells as built-in if their URI matches a BellCollection built-in bell
            var anyBellsUpdated = false
            for (entity in db.getAllBells()) {
                if (entity.isBuiltin) continue
                val bell = BellCollection.getBellList().find { it.uri.toString() == entity.uri }
                if (bell != null) {
                    entity.isBuiltin = true
                    entity.name = bell.getName()
                    db.updateBell(entity)
                    anyBellsUpdated = true
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

            // 7. Repair stale section bellIds (e.g. pointing to non-existent bell)
            repairStaleSectionBellIds(db, finalBellEntities)
        }

        @Suppress("NestedBlockDepth", "LoopWithTooManyJumpStatements")
        private suspend fun repairStaleSectionBellIds(
            db: DbOperations,
            allBellEntities: List<BellEntity>,
        ) {
            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bellId <= 0) continue
                    val currentBell = allBellEntities.find { it._id == section.bellId }
                    if (currentBell == null) {
                        val demo =
                            allBellEntities.firstOrNull { it.isBuiltin && it.uri.endsWith("/raw/bell2") }
                                ?: allBellEntities.firstOrNull { it.isBuiltin }
                                ?: allBellEntities.firstOrNull()
                        if (demo != null) {
                            section.bellId = demo._id
                            db.updateSection(section)
                        }
                    }
                }
            }
        }

        @Suppress("NestedBlockDepth", "LoopWithTooManyJumpStatements")
        suspend fun repairBellUris(db: DbOperations) {
            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bellId <= 0) {
                        val demo = BellCollection.getDemoBell() ?: continue
                        val entity = db.getBellByUri(demo.uri.toString())
                        section.bellId = entity?._id ?: 0
                        if (section.bellId > 0) {
                            db.updateSection(section)
                        }
                    } else if (db.getBellById(section.bellId) == null) {
                        val demo = BellCollection.getDemoBell() ?: continue
                        val entity = db.getBellByUri(demo.uri.toString())
                        section.bellId = entity?._id ?: 0
                        if (section.bellId > 0) {
                            db.updateSection(section)
                        }
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
                allBellEntities.firstOrNull { it.isBuiltin && it.uri.endsWith("/raw/bell2") }
                    ?: allBellEntities.firstOrNull { it.isBuiltin }
                    ?: allBellEntities.firstOrNull()
            val demoId = demoEntity?._id ?: 0

            for (session in db.readSessions()) {
                for (section in db.readSections(session.id)) {
                    if (section.bellId <= 0) {
                        section.bellId = demoId
                        if (section.bellId > 0) {
                            db.updateSection(section)
                        }
                    }
                }

                val volumes = db.readBellVolumes(session.id)
                var changed = false
                for (bv in volumes) {
                    if (bv.bellId <= 0) {
                        bv.bellId = demoId
                        changed = true
                    } else if (bv.bellId > 0 && allBellEntities.none { it._id == bv.bellId }) {
                        bv.bellId = demoId
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
