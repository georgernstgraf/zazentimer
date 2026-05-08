package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.Room
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbOperations @Inject constructor(
    @ApplicationContext context: Context
) {
    private val context: Context = context.applicationContext
    private var appDb: AppDatabase? = null
    private var sessionDao: SessionDao? = null
    private var sectionDao: SectionDao? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        openDatabase()
    }

    private fun openDatabase() {
        appDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .addCallback(AppDatabase.ON_CREATE_CALLBACK)
            .build()
        sessionDao = appDb!!.sessionDao()
        sectionDao = appDb!!.sectionDao()
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

    fun isConnected(): Boolean = appDb != null && appDb!!.isOpen

    fun readSession(id: Int): Session? = executeSync {
        val entity = sessionDao!!.getSessionById(id)
        entity?.let { toBo(it) }
    }

    fun updateSession(session: Session) {
        val entity = toEntity(session)
        executor.execute { sessionDao!!.update(entity) }
    }

    fun deleteSession(id: Int) {
        executor.execute { sessionDao!!.deleteById(id) }
    }

    fun duplicateSession(sourceId: Int, newName: String): Int = executeSync {
        val sourceEntity = sessionDao!!.getSessionById(sourceId)
        val source = toBo(sourceEntity!!)
        source.name = newName
        source.id = 0
        val sectionEntities = sectionDao!!.getSectionsForSession(sourceId)
        val newEntity = toEntity(source)
        val newId = sessionDao!!.insert(newEntity)
        source.id = newId.toInt()
        for (se in sectionEntities) {
            val section = toBo(se)
            section.id = 0
            section.fkSession = source.id
            if (section.rank == -1) {
                val maxRank = sectionDao!!.getMaxRank(source.id)
                section.rank = (maxRank ?: 0) + 1
            }
            val sectionEntity = toEntity(section)
            val sid = sectionDao!!.insert(sectionEntity)
            section.id = sid.toInt()
        }
        source.id
    }

    fun deleteSection(id: Long) {
        executor.execute { sectionDao!!.deleteById(id) }
    }

    fun readSection(id: Int): Section? = executeSync {
        val entity = sectionDao!!.getSectionById(id)
        entity?.let { toBo(it) }
    }

    fun updateSection(section: Section) {
        val entity = toEntity(section)
        executor.execute { sectionDao!!.update(entity) }
    }

    fun switchPositions(id1: Long, id2: Long) {
        executor.execute {
            val s1 = sectionDao!!.getSectionById(id1.toInt())
            val s2 = sectionDao!!.getSectionById(id2.toInt())
            if (s1 != null && s2 != null) {
                val rank1 = s1.rank ?: 0
                val rank2 = s2.rank ?: 0
                sectionDao!!.updateRank(id1.toInt(), rank2)
                sectionDao!!.updateRank(id2.toInt(), rank1)
            }
        }
    }

    fun insertSection(session: Session, section: Section) {
        executeSync {
            if (section.rank == -1) {
                val maxRank = sectionDao!!.getMaxRank(session.id)
                section.rank = (maxRank ?: 0) + 1
            }
            section.fkSession = session.id
            val entity = toEntity(section)
            val newId = sectionDao!!.insert(entity)
            section.id = newId.toInt()
            null
        }
    }

    fun insertSession(session: Session) {
        executeSync {
            val entity = toEntity(session)
            val newId = sessionDao!!.insert(entity)
            session.id = newId.toInt()
            null
        }
    }

    fun readSections(sessionId: Int): Array<Section> = executeSync {
        val entities = sectionDao!!.getSectionsForSession(sessionId)
        val result = ArrayList<Section>()
        for (entity in entities) {
            result.add(toBo(entity))
        }
        result.toTypedArray()
    }

    fun readSessions(): Array<Session> = executeSync {
        val entities = sessionDao!!.getAllSessions()
        val result = ArrayList<Session>()
        for (entity in entities) {
            result.add(toBo(entity))
        }
        result.toTypedArray()
    }

    private fun <T> executeSync(callable: Callable<T>): T {
        try {
            return executor.submit(callable).get()
        } catch (e: ExecutionException) {
            val cause = e.cause
            if (cause is RuntimeException) throw cause
            throw RuntimeException(cause)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException(e)
        }
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
