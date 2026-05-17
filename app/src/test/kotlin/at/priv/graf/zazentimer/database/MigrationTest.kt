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
class MigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_migration.db")
    }

    private fun createV1Database(): SupportSQLiteDatabase {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper =
            factory.create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name("test_migration.db")
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE sessions (" +
                                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "name TEXT NOT NULL, " +
                                        "description TEXT NOT NULL)",
                                )
                                db.execSQL(
                                    "CREATE TABLE sections (" +
                                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "fk_session INTEGER NOT NULL, " +
                                        "name TEXT NOT NULL, " +
                                        "duration INTEGER NOT NULL, " +
                                        "bell INTEGER NOT NULL, " +
                                        "rank INTEGER, " +
                                        "bellcount INTEGER, " +
                                        "bellpause INTEGER, " +
                                        "belluri TEXT)",
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) {
                                // no-op: migration tested via explicit runMigrations()
                            }
                        },
                    ).build(),
            )
        return helper.writableDatabase
    }

    @After
    fun tearDown() {
        context.deleteDatabase("test_migration.db")
    }

    @Test
    fun migrateFrom1To2_createsSettingsTable() {
        val db = createV1Database()

        AppDatabase.MIGRATION_1_2.migrate(db)

        val cursor =
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='settings'",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom1To2_setsDefaultSettings() {
        val db = createV1Database()

        AppDatabase.MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT param FROM settings WHERE param = 'I_BELL_VOLUME'")
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom2To3_noopDoesNotChangeSchema() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)

        AppDatabase.MIGRATION_2_3.migrate(db)

        val cursor =
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='settings'",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom3To4_addsVolumeColumn() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)

        AppDatabase.MIGRATION_3_4.migrate(db)

        val cursor = db.query("PRAGMA table_info(sections)")
        val names = mutableListOf<String>()
        while (cursor.moveToNext()) {
            names.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertThat(names).contains("volume")
        db.close()
    }

    @Test
    fun migrateFrom3To4_volumeColumnIdempotent() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)

        AppDatabase.MIGRATION_3_4.migrate(db)

        val cursor = db.query("PRAGMA table_info(sections)")
        var volumeCount = 0
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "volume") {
                volumeCount++
            }
        }
        cursor.close()
        assertThat(volumeCount).isEqualTo(1)
        db.close()
    }

    @Test
    fun migrateFrom4To5_setsIdNotNull() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)

        AppDatabase.MIGRATION_4_5.migrate(db)

        val cursor = db.query("PRAGMA table_info(sessions)")
        val notNull = mutableMapOf<String, Boolean>()
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val isNotNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1
            notNull[name] = isNotNull
        }
        cursor.close()
        assertThat(notNull["_id"]).isTrue()
        db.close()
    }

    @Test
    fun fullChainFrom1To5_dataSurvives() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('Meditation', 'A test session')",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause) " +
                "VALUES (1, 'Zazen', 600, -1, 1, 3, 5)",
        )

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        val cursor =
            db.query(
                "SELECT name, description FROM sessions WHERE _id = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            .isEqualTo("Meditation")
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("description")))
            .isEqualTo("A test session")
        cursor.close()
        db.close()
    }

    @Test
    fun fullChainFrom1To5_sectionsDataSurvives() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('S', 'D')",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause, belluri) " +
                "VALUES (1, 'Kinhin', 300, 0, 2, 1, 3, NULL)",
        )

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        val cursor =
            db.query(
                "SELECT name, duration FROM sections WHERE _id = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            .isEqualTo("Kinhin")
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("duration")))
            .isEqualTo(300)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom5To6_createsSessionBellVolumesTable() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor =
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='session_bell_volumes'",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom5To6_removesVolumeColumnFromSections() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("PRAGMA table_info(sections)")
        val names = mutableListOf<String>()
        while (cursor.moveToNext()) {
            names.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertThat(names).doesNotContain("volume")
        db.close()
    }

    @Test
    fun migrateFrom5To6_migratesAvgVolumesToSessionBellVolumes() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('S', 'D')",
        )
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause, volume) " +
                "VALUES (1, 'Zazen1', 600, 1, 1, 3, 5, 80)",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause, volume) " +
                "VALUES (1, 'Zazen2', 300, 1, 2, 1, 3, 60)",
        )
        AppDatabase.MIGRATION_4_5.migrate(db)

        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor =
            db.query(
                "SELECT volume FROM session_bell_volumes WHERE fk_session = 1 AND bell = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("volume"))).isEqualTo(70)
        cursor.close()
        db.close()
    }

    @Test
    fun fullChainFrom1To6_dataSurvives() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('Meditation', 'A test session')",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause) " +
                "VALUES (1, 'Zazen', 600, -1, 1, 3, 5)",
        )

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor =
            db.query(
                "SELECT name, description FROM sessions WHERE _id = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            .isEqualTo("Meditation")
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("description")))
            .isEqualTo("A test session")
        cursor.close()
        db.close()
    }

    @Test
    fun fullChainFrom1To6_sectionsDataSurvives() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('S', 'D')",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause, belluri) " +
                "VALUES (1, 'Kinhin', 300, 0, 2, 1, 3, NULL)",
        )

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor =
            db.query(
                "SELECT name, duration FROM sections WHERE _id = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            .isEqualTo("Kinhin")
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("duration")))
            .isEqualTo(300)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom3To4_recreatesSectionsAndSessions() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('S', 'D')",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank) " +
                "VALUES (1, 'Test', 100, -1, 1)",
        )
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)

        AppDatabase.MIGRATION_3_4.migrate(db)

        val cursor = db.query("SELECT description FROM sessions WHERE _id = 1")
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("description")))
            .isEqualTo("D")
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom6To7_createsBellsTable() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)

        AppDatabase.MIGRATION_6_7.migrate(db)

        val cursor =
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='bells'",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test
    fun migrateFrom6To7_seedsBellsFromSectionUris() {
        val db = createV1Database()
        db.execSQL("INSERT INTO sessions (name, description) VALUES ('S', 'D')")
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause, belluri, volume) " +
                "VALUES (1, 'Zazen', 600, -2, 1, 1, 3, 'android.resource://test.pkg/123', 100)",
        )
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        AppDatabase.MIGRATION_6_7.migrate(db)

        val bellsCursor = db.query("SELECT _id FROM bells WHERE uri = 'android.resource://test.pkg/123'")
        assertThat(bellsCursor.count).isEqualTo(1)
        bellsCursor.moveToFirst()
        val bellId = bellsCursor.getInt(bellsCursor.getColumnIndexOrThrow("_id"))
        bellsCursor.close()

        val secCursor = db.query("SELECT bellId FROM sections WHERE _id = 1")
        assertThat(secCursor.count).isEqualTo(1)
        secCursor.moveToFirst()
        assertThat(secCursor.getInt(secCursor.getColumnIndexOrThrow("bellId"))).isEqualTo(bellId)
        secCursor.close()
        db.close()
    }

    @Test
    fun migrateFrom6To7_sectionsHaveBellIdColumn() {
        val db = createV1Database()
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        AppDatabase.MIGRATION_6_7.migrate(db)

        val cursor = db.query("PRAGMA table_info(sections)")
        val names = mutableListOf<String>()
        while (cursor.moveToNext()) {
            names.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertThat(names).contains("bellId")
        db.close()
    }

    @Test
    fun fullChainFrom1To7_dataSurvives() {
        val db = createV1Database()
        db.execSQL(
            "INSERT INTO sessions (name, description) VALUES ('Meditation', 'A test session')",
        )
        db.execSQL(
            "INSERT INTO sections (fk_session, name, duration, bell, rank, bellcount, bellpause) " +
                "VALUES (1, 'Zazen', 600, -1, 1, 3, 5)",
        )

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        AppDatabase.MIGRATION_6_7.migrate(db)

        val cursor =
            db.query(
                "SELECT name, description FROM sessions WHERE _id = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            .isEqualTo("Meditation")
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("description")))
            .isEqualTo("A test session")
        cursor.close()

        val secCursor =
            db.query(
                "SELECT name, duration, bellId FROM sections WHERE _id = 1",
            )
        assertThat(secCursor.count).isEqualTo(1)
        secCursor.moveToFirst()
        assertThat(secCursor.getString(secCursor.getColumnIndexOrThrow("name")))
            .isEqualTo("Zazen")
        assertThat(secCursor.getInt(secCursor.getColumnIndexOrThrow("duration")))
            .isEqualTo(600)
        assertThat(secCursor.getInt(secCursor.getColumnIndexOrThrow("bellId")))
            .isEqualTo(0)
        secCursor.close()
        db.close()
    }

    @Test
    fun migrateFrom6To7_volumeEntriesGetBellId() {
        val db = createV1Database()
        db.execSQL("INSERT INTO sessions (name, description) VALUES ('S', 'D')")
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        db.execSQL(
            "INSERT INTO session_bell_volumes (fk_session, bell, belluri, volume) " +
                "VALUES (1, -2, 'android.resource://test.pkg/456', 80)",
        )
        AppDatabase.MIGRATION_6_7.migrate(db)

        val cursor =
            db.query(
                "SELECT v.bellId FROM session_bell_volumes v " +
                    "JOIN bells b ON b.uri = 'android.resource://test.pkg/456' " +
                    "WHERE v.fk_session = 1",
            )
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("bellId"))).isGreaterThan(0)
        cursor.close()
        db.close()
    }
}
