package de.gaffga.android.zazentimer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ZenTimerDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "zentimer";
    private static String[][] sqlCommands = {new String[0], new String[]{"create table sessions(_id integer primary key autoincrement, name text not null, description text not null);", "create table sections(  _id         integer primary key autoincrement,   fk_session  integer not null,   name        text not null,   duration    integer not null,   bell        integer not null,   rank        integer,  bellcount   integer,  bellpause   integer,  belluri     text);"}, new String[]{"create table settings(_id integer primary key autoincrement, param text not null, value text not null, def text not null);", "insert into settings(param,value,def) values('B_PHONE_OFF_DURING_MEDITATION', '1', '1');", "insert into settings(param,value,def) values('B_NOTIFICATIONS_OFF_DURING_MEDITATION', '1', '1');", "insert into settings(param,value,def) values('I_LAST_SELECTED_SESSION', '-1', '-1');", "insert into settings(param,value,def) values('I_BELL_VOLUME', '20', '20');"}, new String[]{"alter table sections add column volume integer default 100;"}};
    private static int DATABASE_VERSION = sqlCommands.length - 1;

    public ZenTimerDatabase(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, DATABASE_VERSION);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        for (String[] strArr : sqlCommands) {
            for (String str : strArr) {
                sQLiteDatabase.execSQL(str);
            }
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        while (true) {
            i++;
            if (i > i2) {
                return;
            }
            for (String str : sqlCommands[i]) {
                sQLiteDatabase.execSQL(str);
            }
        }
    }

    public boolean isSessionExisting(String str) {
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor query = readableDatabase.query("sessions", new String[]{"name"}, "name = '" + str + "'", null, null, null, null);
        boolean z = query.getCount() > 0;
        query.close();
        readableDatabase.close();
        return z;
    }
}
