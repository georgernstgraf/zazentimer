package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.Room
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
                    ).addMigrations(
                        AppDatabase.MIGRATION_1_2,
                        AppDatabase.MIGRATION_2_3,
                        AppDatabase.MIGRATION_3_4,
                        AppDatabase.MIGRATION_4_5,
                        AppDatabase.MIGRATION_5_6,
                    ).addCallback(AppDatabase.ON_CREATE_CALLBACK)
                    .build()
            val db = appDb ?: return
            sessionDao = db.sessionDao()
            sectionDao = db.sectionDao()
            sessionBellVolumeDao = db.sessionBellVolumeDao()
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
            }
        }

        fun reopen() {
            close()
            openDatabase()
        }

        fun isConnected(): Boolean = appDb?.isOpen == true

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
                            bell = bv.bell,
                            belluri = bv.belluri,
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

        suspend fun updateSection(section: Section) =
            withIdling {
                val entity = EntityMapper.toEntity(section)
                sectionDao?.update(entity)
            }

        suspend fun switchPositions(
            id1: Long,
            id2: Long,
        ) = withIdling {
            val dao = sectionDao ?: return@withIdling
            val s1 = dao.getSectionById(id1.toInt())
            val s2 = dao.getSectionById(id2.toInt())
            if (s1 != null && s2 != null) {
                val rank1 = s1.rank ?: 0
                val rank2 = s2.rank ?: 0
                dao.updateRank(id1.toInt(), rank2)
                dao.updateRank(id2.toInt(), rank1)
            }
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
            section.fkSession = session.id
            val entity = EntityMapper.toEntity(section)
            val newId = dao.insert(entity)
            section.id = newId.toInt()
        }

        suspend fun insertSession(session: Session) =
            withIdling {
                val dao = sessionDao ?: return@withIdling
                val entity = EntityMapper.toEntity(session)
                val newId = dao.insert(entity)
                session.id = newId.toInt()
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
    }
