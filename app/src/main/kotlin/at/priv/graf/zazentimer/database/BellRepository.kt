package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import at.priv.graf.zazentimer.audio.BuiltinBells

@Suppress("TooManyFunctions")
internal class BellRepository(
    private val appDb: RoomDatabase,
    private val bellDao: BellDao,
    private val sectionDao: SectionDao,
    private val sessionBellVolumeDao: SessionBellVolumeDao,
    private val context: Context,
) {
    suspend fun getBellById(id: Int): BellEntity? = withIdling { bellDao.getById(id) }

    suspend fun getBellByUri(uri: String): BellEntity? = withIdling { bellDao.getByUri(uri) }

    suspend fun getAllBells(): List<BellEntity> = withIdling { bellDao.getAll() }

    suspend fun getBuiltinBells(): List<BellEntity> = withIdling { bellDao.getBuiltinBells() }

    suspend fun getNonBuiltinBells(): List<BellEntity> = withIdling { bellDao.getNonBuiltinBells() }

    suspend fun getDemoBell(): BellEntity? =
        withIdling {
            bellDao.getBuiltinByName(context.getString(BuiltinBells.DEMO_BELL_NAME_RES))
        }

    suspend fun getDemoBellIdOrThrow(): Int =
        getDemoBell()?.id ?: error("No builtin bells in database")

    suspend fun insertBell(bell: BellEntity): Long = withIdling { bellDao.insert(bell) }

    suspend fun updateBell(bell: BellEntity) = withIdling { bellDao.update(bell) }

    suspend fun deleteBellById(id: Int) = withIdling { bellDao.deleteById(id) }

    suspend fun deleteCustomBell(bellId: Int) =
        withIdling {
            appDb.withTransaction {
                val demoBellName = context.getString(BuiltinBells.DEMO_BELL_NAME_RES)
                val demoTarget =
                    bellDao.getBuiltinByName(demoBellName)
                        ?: bellDao.getBuiltinBells().firstOrNull()
                        ?: error("No builtin bells in database")
                val targetBellId = demoTarget.id

                reassignBellReferences(bellId, targetBellId)
                sessionBellVolumeDao.deleteByBellId(bellId)
                bellDao.deleteById(bellId)
            }
        }

    private suspend fun reassignBellReferences(
        oldBellId: Int,
        newBellId: Int,
    ) {
        val sections = sectionDao.getSectionsByBellId(oldBellId)
        for (section in sections) {
            section.bell_id = newBellId
            sectionDao.update(section)
        }
    }
}
