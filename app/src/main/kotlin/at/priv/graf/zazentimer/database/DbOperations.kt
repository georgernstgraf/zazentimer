package at.priv.graf.zazentimer.database

import android.content.Context
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
        private val databaseOwner: DatabaseOwner,
        private val sessionRepo: SessionRepository,
        private val sectionRepo: SectionRepository,
        private val bellRepo: BellRepository,
        private val bellSanitizer: BellSanitizer,
        @ApplicationContext context: Context,
    ) {
        private val context: Context = context.applicationContext

        fun close() = databaseOwner.close()

        fun reopen() = databaseOwner.reopen()

        fun isConnected(): Boolean = databaseOwner.isConnected()

        fun applicationContext(): Context = context

        fun getActualDatabaseVersion(): Int = databaseOwner.getActualDatabaseVersion()

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

        suspend fun assignRanks(sessions: List<Session>) = sessionRepo.assignRanks(sessions)

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

        suspend fun getDemoBell(): BellEntity? = bellRepo.getDemoBell()

        suspend fun getDemoBellIdOrThrow(): Int = bellRepo.getDemoBellIdOrThrow()

        suspend fun insertBell(bell: BellEntity): Long = bellRepo.insertBell(bell)

        suspend fun updateBell(bell: BellEntity) = bellRepo.updateBell(bell)

        suspend fun deleteBellById(id: Int) = bellRepo.deleteBellById(id)

        suspend fun deleteCustomBell(bellId: Int) = bellRepo.deleteCustomBell(bellId)

        suspend fun sanitizeBellUris() = bellSanitizer.sanitizeBellUris()
    }
