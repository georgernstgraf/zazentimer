package at.priv.graf.zazentimer.database

import at.priv.graf.zazentimer.Constants
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume

object EntityMapper {
    const val DEFAULT_VOLUME = Constants.DEFAULT_BELL_VOLUME

    fun toEntity(bo: Session): SessionEntity {
        val entity = SessionEntity()
        entity._id = bo.id
        entity.name = bo.name ?: ""
        entity.description = bo.description ?: ""
        entity.rank = bo.rank
        return entity
    }

    fun toBo(entity: SessionEntity): Session {
        val bo = Session()
        bo.id = entity._id
        bo.name = entity.name
        bo.description = entity.description
        bo.rank = entity.rank
        return bo
    }

    fun toEntity(bo: Section): SectionEntity {
        val entity = SectionEntity()
        entity._id = bo.id
        entity.name = bo.name ?: ""
        entity.duration = bo.duration
        entity.rank = bo.rank
        entity.bellcount = bo.bellcount
        entity.bellpause = bo.bellpause
        entity.bellId = bo.bellId
        entity.fk_session = bo.fkSession
        return entity
    }

    fun toBo(entity: SectionEntity): Section {
        val bo = Section()
        bo.id = entity._id
        bo.fkSession = entity.fk_session
        bo.name = entity.name
        bo.duration = entity.duration
        bo.rank = entity.rank
        bo.bellcount = entity.bellcount
        bo.bellpause = entity.bellpause
        bo.bellId = entity.bellId
        return bo
    }

    fun toEntity(bo: SessionBellVolume): SessionBellVolumeEntity {
        val entity = SessionBellVolumeEntity()
        entity._id = bo.id
        entity.fk_session = bo.fkSession
        entity.bellId = bo.bellId
        entity.volume = bo.volume
        return entity
    }

    fun toBo(entity: SessionBellVolumeEntity): SessionBellVolume {
        val bo = SessionBellVolume()
        bo.id = entity._id
        bo.fkSession = entity.fk_session
        bo.bellId = entity.bellId
        bo.volume = entity.volume
        return bo
    }
}
