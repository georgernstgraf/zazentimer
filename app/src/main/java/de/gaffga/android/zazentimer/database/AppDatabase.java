package de.gaffga.android.zazentimer.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SessionEntity.class, SectionEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public static final String DATABASE_NAME = "zentimer";

    public abstract SessionDao sessionDao();
    public abstract SectionDao sectionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .addCallback(ON_CREATE_CALLBACK)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS settings(_id INTEGER PRIMARY KEY AUTOINCREMENT, param TEXT NOT NULL, value TEXT NOT NULL, def TEXT NOT NULL)");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_PHONE_OFF_DURING_MEDITATION', '1', '1')");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1')");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_LAST_SELECTED_SESSION', '-1', '-1')");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_BELL_VOLUME', '20', '20')");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE sections ADD COLUMN volume INTEGER DEFAULT 100");
        }
    };

    static final RoomDatabase.Callback ON_CREATE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("CREATE TABLE IF NOT EXISTS settings(_id INTEGER PRIMARY KEY AUTOINCREMENT, param TEXT NOT NULL, value TEXT NOT NULL, def TEXT NOT NULL)");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_PHONE_OFF_DURING_MEDITATION', '1', '1')");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1')");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_LAST_SELECTED_SESSION', '-1', '-1')");
            db.execSQL("INSERT OR IGNORE INTO settings(param,value,def) VALUES('I_BELL_VOLUME', '20', '20')");
        }
    };
}
