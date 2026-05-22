package at.priv.graf.zazentimer

import android.content.Context
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.DbOperations

object MigrationHelper {
    private const val BUILTIN_BELL_COUNT = 8

    suspend fun seedBuiltinBells(
        context: Context,
        db: DbOperations,
    ) {
        val existingBells = db.getAllBells()
        if (existingBells.isNotEmpty()) return

        BellCollection.initialize(context)
        for (i in 0 until BUILTIN_BELL_COUNT) {
            val bell = BellCollection.getBell(i) ?: continue
            db.insertBell(
                BellEntity(
                    name = bell.getName(),
                    uri = bell.uri.toString(),
                    isBuiltin = true,
                ),
            )
        }
    }
}
