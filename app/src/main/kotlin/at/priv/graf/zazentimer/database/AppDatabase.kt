package at.priv.graf.zazentimer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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

        const val CURRENT_VERSION = 2

        @Suppress("MaxLineLength")
        val MIGRATION_1_2 =
            Migration(1, 2) { db ->
                db.execSQL("PRAGMA foreign_keys=OFF")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS bells_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    is_builtin INTEGER NOT NULL DEFAULT 0
                )""",
                )
                db.execSQL("INSERT INTO bells_new (id, name, uri, is_builtin) SELECT _id, name, uri, is_builtin FROM bells")
                db.execSQL("DROP TABLE bells")
                db.execSQL("ALTER TABLE bells_new RENAME TO bells")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS sessions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    rank INTEGER NOT NULL
                )""",
                )
                db.execSQL(
                    "INSERT INTO sessions_new (id, name, description, rank) SELECT _id, name, description, rank FROM sessions",
                )
                db.execSQL("DROP TABLE sessions")
                db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS sections_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    duration INTEGER NOT NULL,
                    rank INTEGER NOT NULL,
                    bellcount INTEGER NOT NULL,
                    bellpause INTEGER NOT NULL,
                    bellId INTEGER NOT NULL,
                    fk_session INTEGER NOT NULL,
                    FOREIGN KEY (fk_session) REFERENCES sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY (bellId) REFERENCES bells(id)
                )""",
                )
                db.execSQL(
                    """INSERT INTO sections_new (id, name, duration, rank, bellcount, bellpause, bellId, fk_session)
                   SELECT _id, name, duration, rank, bellcount, bellpause, bellId, fk_session FROM sections""",
                )
                db.execSQL("DROP TABLE sections")
                db.execSQL("ALTER TABLE sections_new RENAME TO sections")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS session_bell_volumes_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    fk_session INTEGER NOT NULL,
                    bellId INTEGER NOT NULL,
                    volume INTEGER NOT NULL,
                    FOREIGN KEY (fk_session) REFERENCES sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY (bellId) REFERENCES bells(id)
                )""",
                )
                db.execSQL(
                    """INSERT INTO session_bell_volumes_new (id, fk_session, bellId, volume)
                   SELECT _id, fk_session, bellId, volume FROM session_bell_volumes""",
                )
                db.execSQL("DROP TABLE session_bell_volumes")
                db.execSQL("ALTER TABLE session_bell_volumes_new RENAME TO session_bell_volumes")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_fk_session ON sections(fk_session)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_bellId ON sections(bellId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session_bellId ON session_bell_volumes(fk_session, bellId)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session ON session_bell_volumes(fk_session)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_bell_volumes_bellId ON session_bell_volumes(bellId)")

                db.execSQL("PRAGMA foreign_keys=ON")
            }

        val ON_CREATE_CALLBACK =
            object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                }
            }
    }
}
