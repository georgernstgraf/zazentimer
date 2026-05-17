package at.priv.graf.zazentimer.database

import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber")
@Database(
    entities = [SessionEntity::class, SectionEntity::class, SessionBellVolumeEntity::class, BellEntity::class],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun sectionDao(): SectionDao

    abstract fun sessionBellVolumeDao(): SessionBellVolumeDao

    abstract fun bellDao(): BellDao

    companion object {
        const val DATABASE_NAME = "zentimer"

        const val VERSION_1 = 1
        const val VERSION_2 = 2
        const val VERSION_3 = 3
        const val VERSION_4 = 4
        const val VERSION_5 = 5
        const val VERSION_6 = 6
        const val VERSION_7 = 7

        const val DEFAULT_VOLUME = 100

        val MIGRATION_1_2 =
            object : Migration(VERSION_1, VERSION_2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS settings(" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "param TEXT NOT NULL, " +
                            "value TEXT NOT NULL, " +
                            "def TEXT NOT NULL)",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('B_PHONE_OFF_DURING_MEDITATION', '1', '1')",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1')",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('I_LAST_SELECTED_SESSION', '-1', '-1')",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('I_BELL_VOLUME', '20', '20')",
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(VERSION_2, VERSION_3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No-op: placeholder migration, no schema changes needed
                }
            }

        val MIGRATION_3_4 =
            object : Migration(VERSION_3, VERSION_4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val cursor: Cursor = db.query("PRAGMA table_info(sections)")
                    var hasVolume = false
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if ("volume" == cursor.getString(nameIndex)) {
                            hasVolume = true
                            break
                        }
                    }
                    cursor.close()
                    if (!hasVolume) {
                        db.execSQL(
                            "ALTER TABLE sections ADD COLUMN volume " +
                                "INTEGER DEFAULT $DEFAULT_VOLUME",
                        )
                    }

                    db.execSQL(
                        "CREATE TABLE sessions_new (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "description TEXT NOT NULL)",
                    )
                    db.execSQL("INSERT INTO sessions_new SELECT * FROM sessions")
                    db.execSQL("DROP TABLE sessions")
                    db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

                    db.execSQL(
                        "CREATE TABLE sections_new (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "fk_session INTEGER NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "duration INTEGER NOT NULL, " +
                            "bell INTEGER NOT NULL, " +
                            "rank INTEGER, " +
                            "bellcount INTEGER, " +
                            "bellpause INTEGER, " +
                            "belluri TEXT, " +
                            "volume INTEGER DEFAULT $DEFAULT_VOLUME, " +
                            "FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL("INSERT INTO sections_new SELECT * FROM sections")
                    db.execSQL("DROP TABLE sections")
                    db.execSQL("ALTER TABLE sections_new RENAME TO sections")

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_sections_fk_session " +
                            "ON sections(fk_session)",
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(VERSION_4, VERSION_5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE sessions_new (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "description TEXT NOT NULL)",
                    )
                    db.execSQL("INSERT INTO sessions_new SELECT * FROM sessions")
                    db.execSQL("DROP TABLE sessions")
                    db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

                    db.execSQL(
                        "CREATE TABLE sections_new (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "fk_session INTEGER NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "duration INTEGER NOT NULL, " +
                            "bell INTEGER NOT NULL, " +
                            "rank INTEGER, " +
                            "bellcount INTEGER, " +
                            "bellpause INTEGER, " +
                            "belluri TEXT, " +
                            "volume INTEGER DEFAULT $DEFAULT_VOLUME, " +
                            "FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL("INSERT INTO sections_new SELECT * FROM sections")
                    db.execSQL("DROP TABLE sections")
                    db.execSQL("ALTER TABLE sections_new RENAME TO sections")

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_sections_fk_session " +
                            "ON sections(fk_session)",
                    )
                }
            }

        val MIGRATION_5_6 =
            object : Migration(VERSION_5, VERSION_6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS session_bell_volumes (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "fk_session INTEGER NOT NULL, " +
                            "bell INTEGER, " +
                            "belluri TEXT, " +
                            "volume INTEGER NOT NULL DEFAULT $DEFAULT_VOLUME, " +
                            "FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_session_bell_volumes_session_bell_uri " +
                            "ON session_bell_volumes(fk_session, bell, belluri)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session " +
                            "ON session_bell_volumes(fk_session)",
                    )

                    db.execSQL(
                        "INSERT INTO session_bell_volumes (fk_session, bell, belluri, volume) " +
                            "SELECT fk_session, bell, belluri, " +
                            "CAST(AVG(COALESCE(volume, $DEFAULT_VOLUME)) AS INTEGER) " +
                            "FROM sections " +
                            "GROUP BY fk_session, bell, belluri",
                    )

                    db.execSQL(
                        "CREATE TABLE sections_new (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "fk_session INTEGER NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "duration INTEGER NOT NULL, " +
                            "bell INTEGER NOT NULL, " +
                            "rank INTEGER, " +
                            "bellcount INTEGER, " +
                            "bellpause INTEGER, " +
                            "belluri TEXT, " +
                            "FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL(
                        "INSERT INTO sections_new " +
                            "(_id, fk_session, name, duration, bell, rank, bellcount, bellpause, belluri) " +
                            "SELECT _id, fk_session, name, duration, bell, " +
                            "rank, bellcount, bellpause, belluri FROM sections",
                    )
                    db.execSQL("DROP TABLE sections")
                    db.execSQL("ALTER TABLE sections_new RENAME TO sections")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_sections_fk_session ON sections(fk_session)",
                    )
                }
            }

        val MIGRATION_6_7 =
            object : Migration(VERSION_6, VERSION_7) {
                @Suppress("LongMethod")
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 1. Create bells table
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS bells (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL DEFAULT '', " +
                            "uri TEXT NOT NULL DEFAULT '', " +
                            "is_builtin INTEGER NOT NULL DEFAULT 0, " +
                            "resourceName TEXT)",
                    )

                    // 2. Seed from existing section bell URIs
                    db.execSQL(
                        "INSERT INTO bells (name, uri, is_builtin) " +
                            "SELECT DISTINCT COALESCE(belluri, ''), COALESCE(belluri, ''), 0 " +
                            "FROM sections WHERE belluri IS NOT NULL AND belluri != ''",
                    )

                    // 2b. Seed from existing volume bell URIs (not already in bells)
                    db.execSQL(
                        "INSERT INTO bells (name, uri, is_builtin) " +
                            "SELECT DISTINCT v.belluri, v.belluri, 0 " +
                            "FROM session_bell_volumes v " +
                            "WHERE v.belluri IS NOT NULL AND v.belluri != '' " +
                            "AND NOT EXISTS (SELECT 1 FROM bells b WHERE b.uri = v.belluri)",
                    )

                    // 3. Rebuild sections with bellId column
                    db.execSQL(
                        "CREATE TABLE sections_new (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "fk_session INTEGER NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "duration INTEGER NOT NULL, " +
                            "bell INTEGER NOT NULL, " +
                            "rank INTEGER, " +
                            "bellcount INTEGER, " +
                            "bellpause INTEGER, " +
                            "belluri TEXT, " +
                            "bellId INTEGER NOT NULL, " +
                            "FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL(
                        "INSERT INTO sections_new " +
                            "SELECT s._id, s.fk_session, s.name, s.duration, s.bell, " +
                            "s.rank, s.bellcount, s.bellpause, s.belluri, " +
                            "COALESCE(" +
                            "(SELECT b._id FROM bells b WHERE b.uri = s.belluri AND s.belluri != ''), " +
                            "0) " +
                            "FROM sections s",
                    )
                    db.execSQL("DROP TABLE sections")
                    db.execSQL("ALTER TABLE sections_new RENAME TO sections")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_sections_fk_session ON sections(fk_session)",
                    )

                    // 4. Rebuild session_bell_volumes with bellId column
                    db.execSQL(
                        "CREATE TABLE session_bell_volumes_new (" +
                            "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "fk_session INTEGER NOT NULL, " +
                            "bell INTEGER, " +
                            "belluri TEXT, " +
                            "bellId INTEGER NOT NULL, " +
                            "volume INTEGER NOT NULL, " +
                            "FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL(
                        "INSERT INTO session_bell_volumes_new " +
                            "SELECT v._id, v.fk_session, v.bell, v.belluri, " +
                            "COALESCE(" +
                            "(SELECT b._id FROM bells b WHERE b.uri = v.belluri AND v.belluri != ''), " +
                            "0), " +
                            "v.volume " +
                            "FROM session_bell_volumes v",
                    )
                    db.execSQL("DROP TABLE session_bell_volumes")
                    db.execSQL("ALTER TABLE session_bell_volumes_new RENAME TO session_bell_volumes")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session " +
                            "ON session_bell_volumes(fk_session)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session_bell_belluri " +
                            "ON session_bell_volumes(fk_session, bell, belluri)",
                    )
                }
            }

        val ON_CREATE_CALLBACK =
            object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS settings(" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "param TEXT NOT NULL, " +
                            "value TEXT NOT NULL, " +
                            "def TEXT NOT NULL)",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('B_PHONE_OFF_DURING_MEDITATION', '1', '1')",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1')",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('I_LAST_SELECTED_SESSION', '-1', '-1')",
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO settings(param,value,def) " +
                            "VALUES('I_BELL_VOLUME', '20', '20')",
                    )
                }
            }
    }
}
