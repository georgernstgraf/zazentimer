package at.priv.graf.zazentimer.database

import android.database.Cursor
import androidx.annotation.NonNull
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SessionEntity::class, SectionEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun sectionDao(): SectionDao

    companion object {
        const val DATABASE_NAME = "zentimer"

        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(
                    @NonNull db: SupportSQLiteDatabase,
                ) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS settings(_id INTEGER PRIMARY KEY AUTOINCREMENT, param TEXT NOT NULL, value TEXT NOT NULL, def TEXT NOT NULL)",
                    )
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_PHONE_OFF_DURING_MEDITATION', '1', '1')")
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1')")
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_LAST_SELECTED_SESSION', '-1', '-1')")
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_BELL_VOLUME', '20', '20')")
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(
                    @NonNull db: SupportSQLiteDatabase,
                ) {
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(
                    @NonNull db: SupportSQLiteDatabase,
                ) {
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
                        db.execSQL("ALTER TABLE sections ADD COLUMN volume INTEGER DEFAULT 100")
                    }

                    db.execSQL(
                        "CREATE TABLE sessions_new (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT NOT NULL)",
                    )
                    db.execSQL("INSERT INTO sessions_new SELECT * FROM sessions")
                    db.execSQL("DROP TABLE sessions")
                    db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

                    db.execSQL(
                        "CREATE TABLE sections_new (_id INTEGER PRIMARY KEY AUTOINCREMENT, fk_session INTEGER NOT NULL, name TEXT NOT NULL, duration INTEGER NOT NULL, bell INTEGER NOT NULL, rank INTEGER, bellcount INTEGER, bellpause INTEGER, belluri TEXT, volume INTEGER DEFAULT 100, FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL("INSERT INTO sections_new SELECT * FROM sections")
                    db.execSQL("DROP TABLE sections")
                    db.execSQL("ALTER TABLE sections_new RENAME TO sections")

                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_fk_session ON sections(fk_session)")
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(
                    @NonNull db: SupportSQLiteDatabase,
                ) {
                    db.execSQL(
                        "CREATE TABLE sessions_new (_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT NOT NULL)",
                    )
                    db.execSQL("INSERT INTO sessions_new SELECT * FROM sessions")
                    db.execSQL("DROP TABLE sessions")
                    db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

                    db.execSQL(
                        "CREATE TABLE sections_new (_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, fk_session INTEGER NOT NULL, name TEXT NOT NULL, duration INTEGER NOT NULL, bell INTEGER NOT NULL, rank INTEGER, bellcount INTEGER, bellpause INTEGER, belluri TEXT, volume INTEGER DEFAULT 100, FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE)",
                    )
                    db.execSQL("INSERT INTO sections_new SELECT * FROM sections")
                    db.execSQL("DROP TABLE sections")
                    db.execSQL("ALTER TABLE sections_new RENAME TO sections")

                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_fk_session ON sections(fk_session)")
                }
            }

        val ON_CREATE_CALLBACK =
            object : RoomDatabase.Callback() {
                override fun onCreate(
                    @NonNull db: SupportSQLiteDatabase,
                ) {
                    super.onCreate(db)
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS settings(_id INTEGER PRIMARY KEY AUTOINCREMENT, param TEXT NOT NULL, value TEXT NOT NULL, def TEXT NOT NULL)",
                    )
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_PHONE_OFF_DURING_MEDITATION', '1', '1')")
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1')")
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_LAST_SELECTED_SESSION', '-1', '-1')")
                    db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_BELL_VOLUME', '20', '20')")
                }
            }
    }
}
