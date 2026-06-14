package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
@Suppress("MaxLineLength")
class RoomMigrationTest {
    private lateinit var db: SupportSQLiteDatabase
    private val dbName = "room_migration_test.db"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()
        val config =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """CREATE TABLE IF NOT EXISTS bells (
                                    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                                    name TEXT NOT NULL,
                                    uri TEXT NOT NULL,
                                    is_builtin INTEGER NOT NULL
                                )""",
                            )
                            db.execSQL(
                                """CREATE TABLE IF NOT EXISTS sessions (
                                    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                                    name TEXT NOT NULL,
                                    description TEXT NOT NULL,
                                    rank INTEGER NOT NULL
                                )""",
                            )
                            db.execSQL(
                                """CREATE TABLE IF NOT EXISTS sections (
                                    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                                    name TEXT NOT NULL,
                                    duration INTEGER NOT NULL,
                                    rank INTEGER NOT NULL,
                                    bellcount INTEGER NOT NULL,
                                    bellpause INTEGER NOT NULL,
                                    bellId INTEGER NOT NULL,
                                    fk_session INTEGER NOT NULL,
                                    FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE,
                                    FOREIGN KEY (bellId) REFERENCES bells(_id)
                                )""",
                            )
                            db.execSQL(
                                """CREATE TABLE IF NOT EXISTS session_bell_volumes (
                                    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                                    fk_session INTEGER NOT NULL,
                                    bellId INTEGER NOT NULL,
                                    volume INTEGER NOT NULL,
                                    FOREIGN KEY (fk_session) REFERENCES sessions(_id) ON DELETE CASCADE,
                                    FOREIGN KEY (bellId) REFERENCES bells(_id)
                                )""",
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_fk_session ON sections(fk_session)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_bellId ON sections(bellId)")
                            db.execSQL(
                                "CREATE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session ON session_bell_volumes(fk_session)",
                            )
                            db.execSQL(
                                "CREATE UNIQUE INDEX IF NOT EXISTS index_session_bell_volumes_fk_session_bellId ON session_bell_volumes(fk_session, bellId)",
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_session_bell_volumes_bellId ON session_bell_volumes(bellId)")
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()
        val openHelper = factory.create(config)
        db = openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(dbName)
    }

    private fun seedData(
        bellCount: Int,
        sessionCount: Int,
    ) {
        for (b in 1..bellCount) {
            db.execSQL(
                "INSERT INTO bells (_id, name, uri, is_builtin) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(b, "Bell $b", "uri://$b", if (b <= 3) 1 else 0),
            )
        }
        for (s in 1..sessionCount) {
            db.execSQL(
                "INSERT INTO sessions (_id, name, description, rank) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(s, "Session $s", "Desc $s", s),
            )
            for (se in 1..3) {
                val sectionId = (s - 1) * 3 + se
                db.execSQL(
                    """INSERT INTO sections (_id, fk_session, name, duration, rank, bellcount, bellpause, bellId)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf<Any>(
                        sectionId,
                        s,
                        "Section $sectionId",
                        se * 300,
                        se,
                        if (se % 2 ==
                            1
                        ) {
                            1
                        } else {
                            3
                        },
                        1,
                        ((sectionId - 1) % bellCount) + 1,
                    ),
                )
                db.execSQL(
                    "INSERT INTO session_bell_volumes (_id, fk_session, bellId, volume) VALUES (?, ?, ?, ?)",
                    arrayOf<Any>(sectionId, s, ((sectionId - 1) % bellCount) + 1, 50 + se * 10),
                )
            }
        }
    }

    @Test
    fun migrate_1_2_withData_succeeds() {
        seedData(bellCount = 3, sessionCount = 2)

        AppDatabase.MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT id, name, description, rank FROM sessions ORDER BY id")
        assertThat(cursor.count).isEqualTo(2)
        cursor.moveToFirst()
        assertThat(cursor.getInt(0)).isEqualTo(1)
        assertThat(cursor.getString(1)).isEqualTo("Session 1")
        assertThat(cursor.getString(2)).isEqualTo("Desc 1")
        assertThat(cursor.getInt(3)).isEqualTo(1)
        cursor.moveToNext()
        assertThat(cursor.getInt(0)).isEqualTo(2)
        assertThat(cursor.getString(1)).isEqualTo("Session 2")
        cursor.close()
    }

    @Test
    fun migrate_1_2_withData_preservesForeignKeyRelations() {
        seedData(bellCount = 3, sessionCount = 2)

        AppDatabase.MIGRATION_1_2.migrate(db)

        val cursor =
            db.query(
                """SELECT s.id, s.name, sec.id, sec.name, sec.fk_session, sec.bellId
                   FROM sessions s
                   JOIN sections sec ON sec.fk_session = s.id
                   ORDER BY s.id, sec.id""",
            )
        assertThat(cursor.count).isEqualTo(6)
        cursor.moveToFirst()
        assertThat(cursor.getInt(0)).isEqualTo(1) // session id
        assertThat(cursor.getInt(2)).isEqualTo(1) // section id
        assertThat(cursor.getInt(4)).isEqualTo(1) // fk_session
        assertThat(cursor.getInt(5)).isGreaterThan(0) // bellId
        cursor.close()
    }

    @Test
    fun migrate_1_2_emptyDatabase_succeeds() {
        AppDatabase.MIGRATION_1_2.migrate(db)

        assertThat(
            db.query("SELECT COUNT(*) FROM sessions").let {
                it.moveToFirst()
                it.getInt(0)
            },
        ).isEqualTo(0)
        assertThat(
            db.query("SELECT COUNT(*) FROM sections").let {
                it.moveToFirst()
                it.getInt(0)
            },
        ).isEqualTo(0)
        assertThat(
            db.query("SELECT COUNT(*) FROM bells").let {
                it.moveToFirst()
                it.getInt(0)
            },
        ).isEqualTo(0)
    }

    @Test
    fun migrate_1_2_allPkColumnsHaveNotNull() {
        seedData(bellCount = 3, sessionCount = 1)

        AppDatabase.MIGRATION_1_2.migrate(db)

        for (table in listOf("bells", "sessions", "sections", "session_bell_volumes")) {
            val info = db.query("PRAGMA table_info($table)")
            var idNotNull = false
            while (info.moveToNext()) {
                if (info.getString(1) == "id" && info.getInt(3) == 1) {
                    idNotNull = true
                }
            }
            info.close()
            assertThat(idNotNull).isTrue()
        }
    }

    @Test
    fun migrate_1_2_rankColumnHasNoDefault() {
        seedData(bellCount = 3, sessionCount = 1)

        AppDatabase.MIGRATION_1_2.migrate(db)

        val info = db.query("PRAGMA table_info(sessions)")
        while (info.moveToNext()) {
            if (info.getString(1) == "rank") {
                assertThat(info.getString(4)).isNull()
            }
        }
        info.close()
    }

    @Test
    fun migrate_1_2_indicesExist() {
        seedData(bellCount = 3, sessionCount = 1)

        AppDatabase.MIGRATION_1_2.migrate(db)

        val expectedIndices =
            listOf(
                "index_sections_fk_session",
                "index_sections_bellId",
                "index_session_bell_volumes_fk_session",
                "index_session_bell_volumes_fk_session_bellId",
                "index_session_bell_volumes_bellId",
            )
        val existingIndices = mutableListOf<String>()
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
        while (cursor.moveToNext()) {
            existingIndices.add(cursor.getString(0))
        }
        cursor.close()

        for (idx in expectedIndices) {
            assertThat(existingIndices).contains(idx)
        }
    }

    private fun migrateToV2() {
        seedData(bellCount = 3, sessionCount = 2)
        AppDatabase.MIGRATION_1_2.migrate(db)
    }

    @Test
    fun migrate_2_3_withData_preservesBellId() {
        migrateToV2()

        AppDatabase.MIGRATION_2_3.migrate(db)

        val cursor =
            db.query(
                """SELECT s.id, s.name, sec.id, sec.name, sec.fk_session, sec.bell_id
                   FROM sessions s
                   JOIN sections sec ON sec.fk_session = s.id
                   ORDER BY s.id, sec.id""",
            )
        assertThat(cursor.count).isEqualTo(6)
        cursor.moveToFirst()
        assertThat(cursor.getInt(0)).isEqualTo(1)
        assertThat(cursor.getInt(2)).isEqualTo(1)
        assertThat(cursor.getInt(4)).isEqualTo(1)
        assertThat(cursor.getInt(5)).isGreaterThan(0)
        cursor.close()
    }

    @Test
    fun migrate_2_3_columnRenamedToBellId() {
        migrateToV2()

        AppDatabase.MIGRATION_2_3.migrate(db)

        for (table in listOf("sections", "session_bell_volumes")) {
            val info = db.query("PRAGMA table_info($table)")
            var hasBellId = false
            var hasOldBellId = false
            while (info.moveToNext()) {
                val colName = info.getString(1)
                if (colName == "bell_id") hasBellId = true
                if (colName == "bellId") hasOldBellId = true
            }
            info.close()
            assertThat(hasBellId).isTrue()
            assertThat(hasOldBellId).isFalse()
        }
    }

    @Test
    fun migrate_2_3_indicesRenamed() {
        migrateToV2()

        AppDatabase.MIGRATION_2_3.migrate(db)

        val expectedIndices =
            listOf(
                "index_sections_fk_session",
                "index_sections_bell_id",
                "index_session_bell_volumes_fk_session",
                "index_session_bell_volumes_fk_session_bell_id",
                "index_session_bell_volumes_bell_id",
            )
        val oldIndices =
            listOf(
                "index_sections_bellId",
                "index_session_bell_volumes_fk_session_bellId",
                "index_session_bell_volumes_bellId",
            )
        val existingIndices = mutableListOf<String>()
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
        while (cursor.moveToNext()) {
            existingIndices.add(cursor.getString(0))
        }
        cursor.close()

        for (idx in expectedIndices) {
            assertThat(existingIndices).contains(idx)
        }
        for (idx in oldIndices) {
            assertThat(existingIndices).doesNotContain(idx)
        }
    }

    @Test
    fun migrate_2_3_preservesForeignKeyRelations() {
        migrateToV2()

        AppDatabase.MIGRATION_2_3.migrate(db)

        val orphanedSections =
            db.query(
                """SELECT COUNT(*) FROM sections s LEFT JOIN sessions
                   ON s.fk_session = sessions.id WHERE sessions.id IS NULL""",
            )
        orphanedSections.moveToFirst()
        assertThat(orphanedSections.getInt(0)).isEqualTo(0)
        orphanedSections.close()

        val orphanedVolumes =
            db.query(
                """SELECT COUNT(*) FROM session_bell_volumes v LEFT JOIN bells
                   ON v.bell_id = bells.id WHERE bells.id IS NULL""",
            )
        orphanedVolumes.moveToFirst()
        assertThat(orphanedVolumes.getInt(0)).isEqualTo(0)
        orphanedVolumes.close()
    }

    @Test
    fun migrate_2_3_allPkColumnsHaveNotNull() {
        migrateToV2()

        AppDatabase.MIGRATION_2_3.migrate(db)

        for (table in listOf("bells", "sessions", "sections", "session_bell_volumes")) {
            val info = db.query("PRAGMA table_info($table)")
            var idNotNull = false
            while (info.moveToNext()) {
                if (info.getString(1) == "id" && info.getInt(3) == 1) {
                    idNotNull = true
                }
            }
            info.close()
            assertThat(idNotNull).isTrue()
        }
    }
}
