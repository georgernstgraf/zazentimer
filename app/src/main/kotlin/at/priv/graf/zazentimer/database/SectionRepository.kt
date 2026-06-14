package at.priv.graf.zazentimer.database

import android.content.Context
import at.priv.graf.zazentimer.audio.BuiltinBells
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session

internal class SectionRepository(
    private val sectionDao: SectionDao,
    private val bellDao: BellDao,
    private val context: Context,
) {
    suspend fun readSection(id: Int): Section? =
        withIdling {
            val entity = sectionDao.getSectionById(id)
            entity?.let { EntityMapper.toBo(it) }
        }

    suspend fun readSections(sessionId: Int): Array<Section> =
        withIdling {
            val entities = sectionDao.getSectionsForSession(sessionId)
            val result = ArrayList<Section>()
            for (entity in entities) {
                result.add(EntityMapper.toBo(entity))
            }
            result.toTypedArray()
        }

    suspend fun updateSection(section: Section) =
        withIdling {
            sectionDao.update(EntityMapper.toEntity(section))
        }

    suspend fun insertSection(
        session: Session,
        section: Section,
    ) = withIdling {
        if (section.rank == -1) {
            val maxRank = sectionDao.getMaxRank(session.id)
            section.rank = (maxRank ?: 0) + 1
        }
        section.bellId = resolveBellId(section.bellId)
        section.fkSession = session.id
        val entity = EntityMapper.toEntity(section)
        val newId = sectionDao.insert(entity)
        section.id = newId.toInt()
    }

    suspend fun deleteSection(id: Long) =
        withIdling {
            sectionDao.deleteById(id)
        }

    suspend fun reassignBellReferences(
        oldBellId: Int,
        newBellId: Int,
    ) = withIdling {
        val sections = sectionDao.getSectionsByBellId(oldBellId)
        for (section in sections) {
            section.bell_id = newBellId
            sectionDao.update(section)
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
