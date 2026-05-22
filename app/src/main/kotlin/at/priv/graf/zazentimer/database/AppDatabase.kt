package at.priv.graf.zazentimer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber")
@Database(
    entities = [SessionEntity::class, SectionEntity::class, SessionBellVolumeEntity::class, BellEntity::class],
    version = AppDatabase.CURRENT_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun sectionDao(): SectionDao

    abstract fun sessionBellVolumeDao(): SessionBellVolumeDao

    abstract fun bellDao(): BellDao

    companion object {
        const val DATABASE_NAME = "zazentimer.sqlite"

        const val CURRENT_VERSION = 1

        val ON_CREATE_CALLBACK =
            object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                }
            }
    }
}
