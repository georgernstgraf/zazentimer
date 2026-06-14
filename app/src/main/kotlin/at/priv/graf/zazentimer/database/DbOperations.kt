package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.Room
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
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

        private var _sessionDao: SessionDao? = null
        private val sessionDao: SessionDao get() = _sessionDao ?: error("Database is closed")

        private var _sectionDao: SectionDao? = null
        private val sectionDao: SectionDao get() = _sectionDao ?: error("Database is closed")

        private var _sessionBellVolumeDao: SessionBellVolumeDao? = null
        private val sessionBellVolumeDao: SessionBellVolumeDao
            get() = _sessionBellVolumeDao ?: error("Database is closed")

        private var _bellDao: BellDao? = null
        private val bellDao: BellDao get() = _bellDao ?: error("Database is closed")

        private lateinit var sessionRepo: SessionRepository
        private lateinit var sectionRepo: SectionRepository
        private lateinit var bellRepo: BellRepository
        private lateinit var bellSanitizer: BellSanitizer

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
            _sessionDao = db.sessionDao()
            _sectionDao = db.sectionDao()
            _sessionBellVolumeDao = db.sessionBellVolumeDao()
            _bellDao = db.bellDao()
            sessionRepo = SessionRepository(sessionDao, sectionDao, sessionBellVolumeDao, bellDao)
            sectionRepo = SectionRepository(sectionDao, bellDao)
            bellRepo = BellRepository(bellDao, sectionDao, sessionBellVolumeDao)
            bellSanitizer = BellSanitizer(bellDao, sectionDao, sessionBellVolumeDao, context)
        }

        fun close() {
            appDb?.let {
                it.close()
                appDb = null
                _sessionDao = null
                _sectionDao = null
                _sessionBellVolumeDao = null
                _bellDao = null
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

        suspend fun readSession(id: Int): Session? = sessionRepo.readSession(id)

        suspend fun updateSession(session: Session) = sessionRepo.updateSession(session)

        suspend fun deleteSession(id: Int) = sessionRepo.deleteSession(id)

        suspend fun duplicateSession(
            sourceId: Int,
            newName: String,
        ): Int = sessionRepo.duplicateSession(sourceId, newName)

        suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume> = sessionRepo.readBellVolumes(sessionId)

        suspend fun saveBellVolumes(
            sessionId: Int,
            volumes: List<SessionBellVolume>,
        ) = sessionRepo.saveBellVolumes(sessionId, volumes)

        suspend fun readSessionWithBellVolumes(id: Int): Session? = sessionRepo.readSessionWithBellVolumes(id)

        suspend fun restoreSession(
            session: Session,
            sections: List<Section>,
            volumes: List<SessionBellVolume>,
        ) = sessionRepo.restoreSession(session, sections, volumes)

        suspend fun insertSession(session: Session) = sessionRepo.insertSession(session)

        suspend fun switchSessionPositions(
            id1: Long,
            id2: Long,
        ) = sessionRepo.switchSessionPositions(id1, id2)

        suspend fun readSessions(): Array<Session> = sessionRepo.readSessions()

        suspend fun readSection(id: Int): Section? = sectionRepo.readSection(id)

        suspend fun readSections(sessionId: Int): Array<Section> = sectionRepo.readSections(sessionId)

        suspend fun updateSection(section: Section) = sectionRepo.updateSection(section)

        suspend fun insertSection(
            session: Session,
            section: Section,
        ) = sectionRepo.insertSection(session, section)

        suspend fun deleteSection(id: Long) = sectionRepo.deleteSection(id)

        suspend fun reassignBellReferences(
            oldBellId: Int,
            newBellId: Int,
        ) = sectionRepo.reassignBellReferences(oldBellId, newBellId)

        suspend fun getBellById(id: Int): BellEntity? = bellRepo.getBellById(id)

        suspend fun getBellByUri(uri: String): BellEntity? = bellRepo.getBellByUri(uri)

        suspend fun getAllBells(): List<BellEntity> = bellRepo.getAllBells()

        suspend fun getBuiltinBells(): List<BellEntity> = bellRepo.getBuiltinBells()

        suspend fun getNonBuiltinBells(): List<BellEntity> = bellRepo.getNonBuiltinBells()

        suspend fun insertBell(bell: BellEntity): Long = bellRepo.insertBell(bell)

        suspend fun updateBell(bell: BellEntity) = bellRepo.updateBell(bell)

        suspend fun deleteBellById(id: Int) = bellRepo.deleteBellById(id)

        suspend fun deleteCustomBell(bellId: Int) = bellRepo.deleteCustomBell(bellId)

        suspend fun sanitizeBellUris() = bellSanitizer.sanitizeBellUris()
    }
