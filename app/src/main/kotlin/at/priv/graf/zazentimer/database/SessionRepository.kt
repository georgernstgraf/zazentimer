package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import at.priv.graf.zazentimer.audio.BuiltinBells
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume

@Suppress("TooManyFunctions")
internal class SessionRepository(
    private val appDb: RoomDatabase,
    private val sessionDao: SessionDao,
    private val sectionDao: SectionDao,
    private val sessionBellVolumeDao: SessionBellVolumeDao,
    private val bellDao: BellDao,
    private val context: Context,
) {
    suspend fun readSession(id: Int): Session? =
        withIdling {
            val entity = sessionDao.getSessionById(id)
            entity?.let {
                val session = EntityMapper.toBo(it)
                session.bellVolumes =
                    sessionBellVolumeDao.getBellVolumesForSession(id).map { bv -> EntityMapper.toBo(bv) }
                session
            }
        }

    suspend fun updateSession(session: Session) =
        withIdling {
            sessionDao.update(EntityMapper.toEntity(session))
        }

    suspend fun deleteSession(id: Int) =
        withIdling {
            sessionDao.deleteById(id)
        }

    suspend fun duplicateSession(
        sourceId: Int,
        newName: String,
    ): Int =
        withIdling {
            appDb.withTransaction {
                val sourceEntity = sessionDao.getSessionById(sourceId) ?: return@withTransaction -1
                val source = EntityMapper.toBo(sourceEntity)
                source.name = newName
                source.id = 0
                source.rank = (sessionDao.getMaxRank() ?: 0) + 1
                val sectionEntities = sectionDao.getSectionsForSession(sourceId)
                val newEntity = EntityMapper.toEntity(source)
                val newId = sessionDao.insert(newEntity)
                source.id = newId.toInt()
                for (se in sectionEntities) {
                    val section = EntityMapper.toBo(se)
                    section.id = 0
                    section.fkSession = source.id
                    if (section.rank == -1) {
                        val maxRank = sectionDao.getMaxRank(source.id)
                        section.rank = (maxRank ?: 0) + 1
                    }
                    section.bellId = resolveBellId(section.bellId)
                    val sectionEntity = EntityMapper.toEntity(section)
                    val sid = sectionDao.insert(sectionEntity)
                    section.id = sid.toInt()
                }
                val bellVolumeEntities = sessionBellVolumeDao.getBellVolumesForSession(sourceId)
                for (bv in bellVolumeEntities) {
                    val newBv =
                        SessionBellVolumeEntity(
                            fk_session = source.id,
                            bell_id = resolveBellId(bv.bell_id),
                            volume = bv.volume,
                        )
                    sessionBellVolumeDao.insert(newBv)
                }
                source.id
            }
        }

    suspend fun readBellVolumes(sessionId: Int): List<SessionBellVolume> =
        withIdling {
            sessionBellVolumeDao.getBellVolumesForSession(sessionId).map { EntityMapper.toBo(it) }
        }

    suspend fun saveBellVolumes(
        sessionId: Int,
        volumes: List<SessionBellVolume>,
    ) = withIdling {
        appDb.withTransaction {
            sessionBellVolumeDao.deleteForSession(sessionId)
            val entities =
                volumes.map { bo ->
                    bo.fkSession = sessionId
                    bo.id = 0
                    EntityMapper.toEntity(bo)
                }
            sessionBellVolumeDao.insertAll(entities)
        }
    }

    suspend fun readSessionWithBellVolumes(id: Int): Session? =
        withIdling {
            val entity = sessionDao.getSessionById(id) ?: return@withIdling null
            val session = EntityMapper.toBo(entity)
            session.bellVolumes =
                sessionBellVolumeDao.getBellVolumesForSession(id).map { EntityMapper.toBo(it) }
            session
        }

    suspend fun restoreSession(
        session: Session,
        sections: List<Section>,
        volumes: List<SessionBellVolume>,
    ) = withIdling {
        appDb.withTransaction {
            sessionDao.insert(EntityMapper.toEntity(session))
            for (section in sections) {
                sectionDao.insert(EntityMapper.toEntity(section))
            }
            for (volume in volumes) {
                sessionBellVolumeDao.insert(EntityMapper.toEntity(volume))
            }
        }
    }

    suspend fun insertSession(session: Session) =
        withIdling {
            if (session.rank <= 0) {
                session.rank = (sessionDao.getMaxRank() ?: 0) + 1
            }
            val entity = EntityMapper.toEntity(session)
            val newId = sessionDao.insert(entity)
            session.id = newId.toInt()
        }

    suspend fun switchSessionPositions(
        id1: Long,
        id2: Long,
    ) = withIdling {
        appDb.withTransaction {
            val s1 = sessionDao.getSessionById(id1.toInt())
            val s2 = sessionDao.getSessionById(id2.toInt())
            if (s1 != null && s2 != null) {
                val rank1 = s1.rank
                val rank2 = s2.rank
                sessionDao.updateRank(id1.toInt(), rank2)
                sessionDao.updateRank(id2.toInt(), rank1)
            }
        }
    }

    suspend fun readSessions(): Array<Session> =
        withIdling {
            val entities = sessionDao.getAllSessions()
            val result = ArrayList<Session>()
            for (entity in entities) {
                result.add(EntityMapper.toBo(entity))
            }
            result.toTypedArray()
        }

    suspend fun assignRanks(sessions: List<Session>) =
        withIdling {
            appDb.withTransaction {
                for (i in sessions.indices) {
                    sessions[i].rank = i
                }
                for (session in sessions) {
                    sessionDao.update(EntityMapper.toEntity(session))
                }
            }
        }

    private suspend fun resolveBellId(bellId: Int): Int {
        if (bellId > 0) {
            val bell = bellDao.getById(bellId)
            if (bell != null && bell.id > 0) return bell.id
        }
        return fallbackBellId()
    }

    private suspend fun fallbackBellId(): Int {
        val demoBellName = context.getString(BuiltinBells.DEMO_BELL_NAME_RES)
        val demoBell = bellDao.getBuiltinByName(demoBellName)
        if (demoBell != null) return demoBell.id
        val firstBuiltin = bellDao.getBuiltinBells().firstOrNull()
        return firstBuiltin?.id ?: error("No builtin bells in database")
    }
}
