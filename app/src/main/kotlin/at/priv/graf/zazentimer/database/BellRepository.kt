package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.withTransaction
import at.priv.graf.zazentimer.audio.BuiltinBells
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class BellRepository
    @Inject
    constructor(
        private val databaseOwner: DatabaseOwner,
        @ApplicationContext private val context: Context,
    ) {
        private fun bellDao(): BellDao = databaseOwner.bellDao()

        private fun sectionDao(): SectionDao = databaseOwner.sectionDao()

        private fun sessionBellVolumeDao(): SessionBellVolumeDao = databaseOwner.sessionBellVolumeDao()

        private fun appDb(): AppDatabase = databaseOwner.appDatabase()

        suspend fun getBellById(id: Int): BellEntity? = withIdling { bellDao().getById(id) }

        suspend fun getBellByUri(uri: String): BellEntity? = withIdling { bellDao().getByUri(uri) }

        suspend fun getAllBells(): List<BellEntity> = withIdling { bellDao().getAll() }

        suspend fun getBuiltinBells(): List<BellEntity> = withIdling { bellDao().getBuiltinBells() }

        suspend fun getNonBuiltinBells(): List<BellEntity> = withIdling { bellDao().getNonBuiltinBells() }

        suspend fun getDemoBell(): BellEntity? =
            withIdling {
                bellDao().getBuiltinByName(context.getString(BuiltinBells.DEMO_BELL_NAME_RES))
            }

        suspend fun getDemoBellIdOrThrow(): Int = getDemoBell()?.id ?: error("No builtin bells in database")

        suspend fun insertBell(bell: BellEntity): Long = withIdling { bellDao().insert(bell) }

        suspend fun updateBell(bell: BellEntity) = withIdling { bellDao().update(bell) }

        suspend fun deleteBellById(id: Int) = withIdling { bellDao().deleteById(id) }

        suspend fun deleteCustomBell(bellId: Int) =
            withIdling {
                appDb().withTransaction {
                    val demoBellName = context.getString(BuiltinBells.DEMO_BELL_NAME_RES)
                    val demoTarget =
                        bellDao().getBuiltinByName(demoBellName)
                            ?: bellDao().getBuiltinBells().firstOrNull()
                            ?: error("No builtin bells in database")
                    val targetBellId = demoTarget.id

                    reassignBellReferences(bellId, targetBellId)
                    sessionBellVolumeDao().deleteByBellId(bellId)
                    bellDao().deleteById(bellId)
                }
            }

        private suspend fun reassignBellReferences(
            oldBellId: Int,
            newBellId: Int,
        ) {
            val sections = sectionDao().getSectionsByBellId(oldBellId)
            for (section in sections) {
                section.bell_id = newBellId
                sectionDao().update(section)
            }
        }
    }
