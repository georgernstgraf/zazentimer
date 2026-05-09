package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.Room
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbOperations
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val context: Context = context.applicationContext
        private var appDb: AppDatabase? = null
        private var sessionDao: SessionDao? = null
        private var sectionDao: SectionDao? = null

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
                    ).addCallback(AppDatabase.ON_CREATE_CALLBACK)
                    .build()
            val db = appDb ?: return
            sessionDao = db.sessionDao()
            sectionDao = db.sectionDao()
        }

        fun close() {
            appDb?.let {
                it.close()
                appDb = null
                sessionDao = null
                sectionDao = null
            }
        }

        fun reopen() {
            close()
            openDatabase()
        }

        fun isConnected(): Boolean = appDb?.isOpen == true

        suspend fun readSession(id: Int): Session? {
            val dao = sessionDao ?: return null
            val entity = dao.getSessionById(id)
            return entity?.let { toBo(it) }
        }

        suspend fun updateSession(session: Session) {
            val entity = toEntity(session)
            sessionDao?.update(entity)
        }

        suspend fun deleteSession(id: Int) {
            sessionDao?.deleteById(id)
        }

        suspend fun duplicateSession(
            sourceId: Int,
            newName: String,
        ): Int {
            val sDao = sessionDao ?: return -1
            val secDao = sectionDao ?: return -1
            val sourceEntity = sDao.getSessionById(sourceId) ?: return -1
            val source = toBo(sourceEntity)
            source.name = newName
            source.id = 0
            val sectionEntities = secDao.getSectionsForSession(sourceId)
            val newEntity = toEntity(source)
            val newId = sDao.insert(newEntity)
            source.id = newId.toInt()
            for (se in sectionEntities) {
                val section = toBo(se)
                section.id = 0
                section.fkSession = source.id
                if (section.rank == -1) {
                    val maxRank = secDao.getMaxRank(source.id)
                    section.rank = (maxRank ?: 0) + 1
                }
                val sectionEntity = toEntity(section)
                val sid = secDao.insert(sectionEntity)
                section.id = sid.toInt()
            }
            return source.id
        }

        suspend fun deleteSection(id: Long) {
            sectionDao?.deleteById(id)
        }

        suspend fun readSection(id: Int): Section? {
            val dao = sectionDao ?: return null
            val entity = dao.getSectionById(id)
            return entity?.let { toBo(it) }
        }

        suspend fun updateSection(section: Section) {
            val entity = toEntity(section)
            sectionDao?.update(entity)
        }

        suspend fun switchPositions(
            id1: Long,
            id2: Long,
        ) {
            val dao = sectionDao ?: return
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
        ) {
            val dao = sectionDao ?: return
            if (section.rank == -1) {
                val maxRank = dao.getMaxRank(session.id)
                section.rank = (maxRank ?: 0) + 1
            }
            section.fkSession = session.id
            val entity = toEntity(section)
            val newId = dao.insert(entity)
            section.id = newId.toInt()
        }

        suspend fun insertSession(session: Session) {
            val dao = sessionDao ?: return
            val entity = toEntity(session)
            val newId = dao.insert(entity)
            session.id = newId.toInt()
        }

        suspend fun readSections(sessionId: Int): Array<Section> {
            val dao = sectionDao ?: return emptyArray()
            val entities = dao.getSectionsForSession(sessionId)
            val result = ArrayList<Section>()
            for (entity in entities) {
                result.add(toBo(entity))
            }
            return result.toTypedArray()
        }

        suspend fun readSessions(): Array<Session> {
            val dao = sessionDao ?: return emptyArray()
            val entities = dao.getAllSessions()
            val result = ArrayList<Session>()
            for (entity in entities) {
                result.add(toBo(entity))
            }
            return result.toTypedArray()
        }

        companion object {
            private fun toEntity(bo: Session): SessionEntity {
                val entity = SessionEntity()
                entity._id = bo.id
                entity.name = bo.name ?: ""
                entity.description = bo.description ?: ""
                return entity
            }

            private fun toBo(entity: SessionEntity): Session {
                val bo = Session()
                bo.id = entity._id
                bo.name = entity.name
                bo.description = entity.description
                return bo
            }

            private fun toEntity(bo: Section): SectionEntity {
                val entity = SectionEntity()
                entity._id = bo.id
                entity.fk_session = bo.fkSession
                entity.name = bo.name ?: ""
                entity.duration = bo.duration
                entity.bell = bo.bell
                entity.rank = bo.rank
                entity.bellcount = bo.bellcount
                entity.bellpause = bo.bellpause
                entity.belluri = bo.bellUri
                entity.volume = bo.volume
                return entity
            }

            private fun toBo(entity: SectionEntity): Section {
                val bo = Section()
                bo.id = entity._id
                bo.fkSession = entity.fk_session
                bo.name = entity.name
                bo.duration = entity.duration
                bo.bell = entity.bell
                bo.rank = entity.rank ?: -1
                bo.bellcount = entity.bellcount ?: 1
                bo.bellpause = entity.bellpause ?: 1
                bo.bellUri = entity.belluri
                bo.volume = entity.volume ?: 100
                return bo
            }
        }
    }
