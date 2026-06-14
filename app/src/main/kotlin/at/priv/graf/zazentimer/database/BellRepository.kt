package at.priv.graf.zazentimer.database

import at.priv.graf.zazentimer.audio.BellCollection

internal class BellRepository(
    private val bellDao: BellDao,
    private val sectionDao: SectionDao,
    private val sessionBellVolumeDao: SessionBellVolumeDao,
) {
    suspend fun getBellById(id: Int): BellEntity? = withIdling { bellDao.getById(id) }

    suspend fun getBellByUri(uri: String): BellEntity? = withIdling { bellDao.getByUri(uri) }

    suspend fun getAllBells(): List<BellEntity> = withIdling { bellDao.getAll() }

    suspend fun getBuiltinBells(): List<BellEntity> = withIdling { bellDao.getBuiltinBells() }

    suspend fun getNonBuiltinBells(): List<BellEntity> = withIdling { bellDao.getNonBuiltinBells() }

    suspend fun insertBell(bell: BellEntity): Long = withIdling { bellDao.insert(bell) }

    suspend fun updateBell(bell: BellEntity) = withIdling { bellDao.update(bell) }

    suspend fun deleteBellById(id: Int) = withIdling { bellDao.deleteById(id) }

    suspend fun deleteCustomBell(bellId: Int) =
        withIdling {
            val demoBellUri = BellCollection.getDemoBell()?.uri?.toString()
            val demoTarget =
                if (demoBellUri != null) {
                    bellDao.getByUri(demoBellUri)
                } else {
                    bellDao.getBuiltinBells().firstOrNull()
                } ?: return@withIdling
            val targetBellId = demoTarget.id

            reassignBellReferences(bellId, targetBellId)
            sessionBellVolumeDao.deleteByBellId(bellId)
            bellDao.deleteById(bellId)
        }

    private suspend fun reassignBellReferences(
        oldBellId: Int,
        newBellId: Int,
    ) {
        val sections = sectionDao.getSectionsByBellId(oldBellId)
        for (section in sections) {
            section.bellId = newBellId
            sectionDao.update(section)
        }
    }
}
