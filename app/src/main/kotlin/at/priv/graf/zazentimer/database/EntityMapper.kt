package at.priv.graf.zazentimer.database

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session

object EntityMapper {
    const val DEFAULT_VOLUME = 100

    fun toEntity(bo: Session): SessionEntity {
        val entity = SessionEntity()
        entity._id = bo.id
        entity.name = bo.name ?: ""
        entity.description = bo.description ?: ""
        return entity
    }

    fun toBo(entity: SessionEntity): Session {
        val bo = Session()
        bo.id = entity._id
        bo.name = entity.name
        bo.description = entity.description
        return bo
    }

    fun toEntity(bo: Section): SectionEntity {
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

    fun toBo(entity: SectionEntity): Section {
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
        bo.volume = entity.volume ?: DEFAULT_VOLUME
        return bo
    }
}
