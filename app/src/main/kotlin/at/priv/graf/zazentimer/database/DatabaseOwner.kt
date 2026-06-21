package at.priv.graf.zazentimer.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
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
            appDb?.let { database ->
                checkpointWal(database)
                database.close()
            }
            appDb = null
        }

        /**
         * Forces a synchronous WAL checkpoint before the database is closed.
         *
         * Without this, recent writes (e.g. demo sessions inserted in a test's `@Before`, or
         * `sanitizeBellUris()` updates) remain in the `-wal` file. `BackupManager.backup()` ZIPs
         * only the main database file — the `-wal` companion is not included — so on restore the
         * backup is missing rows that the test then expects (issue #290: lost 1 of 8 sections on
         * API 31, 1 of 2 sessions on API 35).
         *
         * The Cursor returned by `query("PRAGMA wal_checkpoint(TRUNCATE)")` is executed lazily;
         * the underlying statement is never run unless the cursor is read. Calling `.close()` on
         * an unconsumed cursor — as the previous implementation did — silently skips the
         * checkpoint. On API ≤ 27 Room's `RoomDatabase.close()` triggers SQLite's auto-checkpoint
         * on last-connection-close, masking the bug; on API ≥ 31 that auto-checkpoint is no
         * longer reliable.
         *
         * Reading the cursor's result row also surfaces the `busy` flag, which is logged for
         * diagnostics.
         */
        private fun checkpointWal(database: AppDatabase) {
            runCatching {
                database.openHelper.writableDatabase
                    .query("PRAGMA wal_checkpoint(TRUNCATE)")
                    .use { cursor ->
                        if (cursor.moveToFirst()) {
                            val busy = cursor.getInt(0)
                            val log = cursor.getInt(1)
                            val checkpointed = cursor.getInt(2)
                            if (busy != 0) {
                                Log.w(
                                    TAG,
                                    "WAL checkpoint incomplete: busy=$busy log=$log checkpointed=$checkpointed",
                                )
                            } else {
                                Log.d(
                                    TAG,
                                    "WAL checkpoint complete: log=$log checkpointed=$checkpointed",
                                )
                            }
                        }
                    }
            }.onFailure { e ->
                Log.w(TAG, "WAL checkpoint failed before close", e)
            }
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

        companion object {
            private const val TAG = "ZMT_DatabaseOwner"
        }
    }
