package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseOwner
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var appDb: AppDatabase? = null

        init {
            openDatabase()
        }

        private fun openDatabase() {
            appDb =
                Room
                    .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
                    .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(AppDatabase.ON_CREATE_CALLBACK)
                    .build()
        }

        private val db: AppDatabase get() = appDb ?: error("Database is closed")

        fun close() {
            appDb?.let {
                runCatching {
                    it.openHelper.writableDatabase
                        .query("PRAGMA wal_checkpoint(TRUNCATE)")
                        .close()
                }
                it.close()
            }
            appDb = null
        }

        fun reopen() {
            close()
            openDatabase()
        }

        fun isConnected(): Boolean = appDb?.isOpen == true

        fun getActualDatabaseVersion(): Int {
            val v = appDb?.openHelper?.readableDatabase?.version
            return v ?: AppDatabase.CURRENT_VERSION
        }

        fun appDatabase(): AppDatabase = db

        fun sessionDao(): SessionDao = db.sessionDao()

        fun sectionDao(): SectionDao = db.sectionDao()

        fun bellDao(): BellDao = db.bellDao()

        fun sessionBellVolumeDao(): SessionBellVolumeDao = db.sessionBellVolumeDao()
    }
