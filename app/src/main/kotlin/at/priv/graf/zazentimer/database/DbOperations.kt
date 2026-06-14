package at.priv.graf.zazentimer.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
import at.priv.graf.zazentimer.service.IdlingResourceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class DbOperations
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val context: Context = context.applicationContext
        private var appDb: AppDatabase? = null
        private var sessionDao: SessionDao? = null
        private var sectionDao: SectionDao? = null
        private var sessionBellVolumeDao: SessionBellVolumeDao? = null
        private var bellDao: BellDao? = null

        init {
            openDatabase()
        }

        private fun openDatabase() {
            appDb =
                Room
                    .databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        AppDatabase.DATABASE_NAME,
                    ).addMigrations(AppDatabase.MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(AppDatabase.ON_CREATE_CALLBACK)
                    .build()
            val db = appDb ?: return
            sessionDao = db.sessionDao()
            sectionDao = db.sectionDao()
            sessionBellVolumeDao = db.sessionBellVolumeDao()
            bellDao = db.bellDao()
        }

        private suspend fun <T> withIdling(block: suspend () -> T): T {
            IdlingResourceManager.increment()
            return try {
                block()
            } finally {
                IdlingResourceManager.decrement()
            }
        }

        fun close() {
            appDb?.let {
                it.close()
                appDb = null
                sessionDao = null
                sectionDao = null
                sessionBellVolumeDao = null
                bellDao = null
            }
        }

        fun reopen() {
            close()
            openDatabase()
        }

        fun isConnected(): Boolean = appDb?.isOpen == true

        fun getActualDatabaseVersion(): Int {
            val v = appDb?.openHelper?.readableDatabase?.version
            return v ?: AppDatabase.CURRENT_VERSION
        }

        suspend fun readSession(id: Int): Session? =
            withIdling {
                val dao = sessionDao ?: return@withIdling null
                val entity = dao.getSessionById(id)
                entity?.let {
                    val session = EntityMapper.toBo(it)
                    val bvDao = sessionBellVolumeDao
                    if (bvDao != null) {
                        session.bellVolumes = bvDao.getBellVolumesForSession(id).map { bv -> EntityMapper.toBo(bv) }
                    }
                    session
                }
            }

        suspend fun updateSession(session: Session) =
            withIdling {
                val entity = EntityMapper.toEntity(session)
                sessionDao?.update(entity)
            }

        suspend fun deleteSession(id: Int) =
            withIdling {
                sessionDao?.deleteById(id)
            }

        suspend fun duplicateSession(
            sourceId: Int,
            newName: String,
        ): Int =
            withIdling {
                val sDao = sessionDao ?: return@withIdling -1
                val secDao = sectionDao ?: return@withIdling -1
                val sourceEntity = sDao.getSessionById(sourceId) ?: return@withIdling -1
                val source = EntityMapper.toBo(sourceEntity)
                source.name = newName
                source.id = 0
                source.rank = (sDao.getMaxRank() ?: 0) + 1
                val sectionEntities = secDao.getSectionsForSession(sourceId)
                val newEntity = EntityMapper.toEntity(source)
                val newId = sDao.insert(newEntity)
                source.id = newId.toInt()
                for (se in sectionEntities) {
                    val section = EntityMapper.toBo(se)
                    section.id = 0
                    section.fkSession = source.id
                    if (section.rank == -1) {
                        val maxRank = secDao.getMaxRank(source.id)
                        section.rank = (maxRank ?: 0) + 1
                    }
                    section.bellId = resolveBellId(section.bellId)
                    val sectionEntity = EntityMapper.toEntity(section)
                    val sid = secDao.insert(sectionEntity)
                    section.id = sid.toInt()
                }
                val bvDao = sessionBellVolumeDao ?: return@withIdling source.id
                val bellVolumeEntities = bvDao.getBellVolumesForSession(sourceId)
                for (bv in bellVolumeEntities) {
                    val newBv =
                        SessionBellVolumeEntity(
                            fk_session = source.id,
                            bellId = resolveBellId(bv.bellId),
                            volume = bv.volume,
                        )
                    bvDao.insert(newBv)
                }
                source.id
            }

        suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume> =
            withIdling {
                val dao = sessionBellVolumeDao ?: return@withIdling emptyList()
                dao.getBellVolumesForSession(sessionId).map { EntityMapper.toBo(it) }
            }

        suspend fun saveBellVolumes(
            sessionId: Int,
            volumes: List<SessionBellVolume>,
        ) = withIdling {
            val dao = sessionBellVolumeDao ?: return@withIdling
            dao.deleteForSession(sessionId)
            val entities =
                volumes.map { bo ->
                    bo.fkSession = sessionId
                    bo.id = 0
                    EntityMapper.toEntity(bo)
                }
            dao.insertAll(entities)
        }

        suspend fun readSessionWithBellVolumes(id: Int): Session? =
            withIdling {
                val dao = sessionDao ?: return@withIdling null
                val entity = dao.getSessionById(id) ?: return@withIdling null
                val session = EntityMapper.toBo(entity)
                val bvDao = sessionBellVolumeDao ?: return@withIdling session
                session.bellVolumes = bvDao.getBellVolumesForSession(id).map { EntityMapper.toBo(it) }
                session
            }

        suspend fun deleteSection(id: Long) =
            withIdling {
                sectionDao?.deleteById(id)
            }

        suspend fun readSection(id: Int): Section? =
            withIdling {
                val dao = sectionDao ?: return@withIdling null
                val entity = dao.getSectionById(id)
                entity?.let { EntityMapper.toBo(it) }
            }

        suspend fun restoreSession(
            session: Session,
            sections: List<Section>,
            volumes: List<SessionBellVolume>,
        ) = withIdling {
            val sDao = sessionDao ?: return@withIdling
            val secDao = sectionDao ?: return@withIdling
            val bvDao = sessionBellVolumeDao ?: return@withIdling
            sDao.insert(EntityMapper.toEntity(session))
            for (section in sections) {
                secDao.insert(EntityMapper.toEntity(section))
            }
            for (volume in volumes) {
                bvDao.insert(EntityMapper.toEntity(volume))
            }
        }

        suspend fun updateSection(section: Section) =
            withIdling {
                val entity = EntityMapper.toEntity(section)
                sectionDao?.update(entity)
            }

        suspend fun insertSection(
            session: Session,
            section: Section,
        ) = withIdling {
            val dao = sectionDao ?: return@withIdling
            if (section.rank == -1) {
                val maxRank = dao.getMaxRank(session.id)
                section.rank = (maxRank ?: 0) + 1
            }
            section.bellId = resolveBellId(section.bellId)
            section.fkSession = session.id
            val entity = EntityMapper.toEntity(section)
            val newId = dao.insert(entity)
            section.id = newId.toInt()
        }

        private suspend fun resolveBellId(bellId: Int): Int {
            if (bellId > 0) {
                val bell = bellDao?.getById(bellId)
                if (bell != null && bell.id > 0) return bell.id
            }
            return fallbackBellId()
        }

        private suspend fun fallbackBellId(): Int {
            val demoBell =
                BellCollection.getDemoBell()?.uri?.toString()?.let { uri ->
                    bellDao?.getByUri(uri)
                }
            return demoBell?.id ?: 0
        }

        suspend fun insertSession(session: Session) =
            withIdling {
                val dao = sessionDao ?: return@withIdling
                if (session.rank <= 0) {
                    session.rank = (dao.getMaxRank() ?: 0) + 1
                }
                val entity = EntityMapper.toEntity(session)
                val newId = dao.insert(entity)
                session.id = newId.toInt()
            }

        suspend fun switchSessionPositions(
            id1: Long,
            id2: Long,
        ) = withIdling {
            val dao = sessionDao ?: return@withIdling
            val s1 = dao.getSessionById(id1.toInt())
            val s2 = dao.getSessionById(id2.toInt())
            if (s1 != null && s2 != null) {
                val rank1 = s1.rank
                val rank2 = s2.rank
                dao.updateRank(id1.toInt(), rank2)
                dao.updateRank(id2.toInt(), rank1)
            }
        }

        suspend fun readSections(sessionId: Int): Array<Section> =
            withIdling {
                val dao = sectionDao ?: return@withIdling emptyArray()
                val entities = dao.getSectionsForSession(sessionId)
                val result = ArrayList<Section>()
                for (entity in entities) {
                    result.add(EntityMapper.toBo(entity))
                }
                result.toTypedArray()
            }

        suspend fun readSessions(): Array<Session> =
            withIdling {
                val dao = sessionDao ?: return@withIdling emptyArray()
                val entities = dao.getAllSessions()
                val result = ArrayList<Session>()
                for (entity in entities) {
                    result.add(EntityMapper.toBo(entity))
                }
                result.toTypedArray()
            }

        suspend fun getBellById(id: Int): BellEntity? = withIdling { bellDao?.getById(id) }

        suspend fun getBellByUri(uri: String): BellEntity? = withIdling { bellDao?.getByUri(uri) }

        suspend fun getAllBells(): List<BellEntity> = withIdling { bellDao?.getAll() ?: emptyList() }

        suspend fun getBuiltinBells(): List<BellEntity> = withIdling { bellDao?.getBuiltinBells() ?: emptyList() }

        suspend fun getNonBuiltinBells(): List<BellEntity> = withIdling { bellDao?.getNonBuiltinBells() ?: emptyList() }

        suspend fun insertBell(bell: BellEntity): Long = withIdling { bellDao?.insert(bell) ?: -1 }

        suspend fun updateBell(bell: BellEntity) = withIdling { bellDao?.update(bell) }

        suspend fun deleteBellById(id: Int) = withIdling { bellDao?.deleteById(id) }

        suspend fun deleteCustomBell(bellId: Int) =
            withIdling {
                val demoBellUri = BellCollection.getDemoBell()?.uri?.toString()
                val demoTarget =
                    if (demoBellUri != null) {
                        bellDao?.getByUri(demoBellUri)
                    } else {
                        bellDao?.getBuiltinBells()?.firstOrNull()
                    } ?: return@withIdling
                val targetBellId = demoTarget.id

                val allSessionEntities = sessionDao?.getAllSessions() ?: emptyList()
                for (sessionEntity in allSessionEntities) {
                    val sections = sectionDao?.getSectionsForSession(sessionEntity.id) ?: emptyList()
                    for (section in sections) {
                        if (section.bellId == bellId) {
                            section.bellId = targetBellId
                            sectionDao?.update(section)
                        }
                    }
                    val volumes = sessionBellVolumeDao?.getBellVolumesForSession(sessionEntity.id) ?: emptyList()
                    for (volume in volumes) {
                        if (volume.bellId == bellId) {
                            sessionBellVolumeDao?.deleteById(volume.id.toLong())
                        }
                    }
                }

                bellDao?.deleteById(bellId)
            }

        @Suppress("CyclomaticComplexMethod", "LongMethod")
        suspend fun sanitizeBellUris() =
            withIdling {
                val bellDao = bellDao ?: return@withIdling
                val sectionDao = sectionDao ?: return@withIdling
                val bvDao = sessionBellVolumeDao ?: return@withIdling

                val allDbBells = bellDao.getAll()
                val currentBells = BellCollection.getBellList()

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

                val updatedBells = bellDao.getAll()
                val demoBellId =
                    BellCollection.getDemoBell()?.let { demo ->
                        updatedBells.find { it.isBuiltin && it.name == demo.getName() }?.id
                    } ?: return@withIdling

                val builtinNames =
                    currentBells
                        .filter { it.uri.scheme == "android.resource" }
                        .map { it.getName() }
                        .toSet()
                for (dbBell in updatedBells.filter { it.isBuiltin }) {
                    if (dbBell.name !in builtinNames) {
                        Log.w(TAG, "Builtin bell '${dbBell.name}' no longer exists, reassigning to demo")
                        val sections = sectionDao.getSectionsByBellId(dbBell.id)
                        for (s in sections) {
                            s.bellId = demoBellId
                            sectionDao.update(s)
                        }
                        bvDao.deleteByBellId(dbBell.id)
                        bellDao.deleteById(dbBell.id)
                    }
                }

                val customBellFiles =
                    context
                        .filesDir
                        .listFiles { _, name -> name.startsWith("bell_") }
                        ?.map { it.name }
                        ?.toSet() ?: emptySet()

                for (bell in updatedBells.filter { !it.isBuiltin }) {
                    val fileName = bell.uri.substringAfterLast("/")
                    if (fileName !in customBellFiles) {
                        Log.w(TAG, "Custom bell file missing ($fileName), removing from DB")
                        val sections = sectionDao.getSectionsByBellId(bell.id)
                        for (s in sections) {
                            s.bellId = demoBellId
                            sectionDao.update(s)
                        }
                        bvDao.deleteByBellId(bell.id)
                        bellDao.deleteById(bell.id)
                    } else {
                        val correctUri = "file://${context.filesDir}/$fileName"
                        if (bell.uri != correctUri) {
                            bell.uri = correctUri
                            bellDao.update(bell)
                        }
                    }
                }

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

        companion object {
            private const val TAG = "ZMT_DbOperations"
        }
    }
