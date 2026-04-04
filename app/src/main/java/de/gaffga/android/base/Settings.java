package de.gaffga.android.base;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.ZenTimerDatabase;

public class Settings {
    public static final String PARAM_B_FIRST_START = "B_FIRST_START";
    public static final String PARAM_B_KEEP_SCREEN_ON = "B_KEEP_SCREEN_ON";
    public static final String PARAM_B_NOTIFICATIONS_OFF_DURING_MEDITATION = "B_NOTIFICATIONS_OFF_DURING_MEDITATION";
    public static final String PARAM_B_PHONE_OFF_DURING_MEDITATION = "B_PHONE_OFF_DURING_MEDITATION";
    public static final String PARAM_B_SHOW_SLIDE_HINT = "B_SHOW_SLIDE_HINT";
    public static final String PARAM_I_BELL_VOLUME = "I_BELL_VOLUME";
    public static final String PARAM_I_LAST_SELECTED_SESSION = "I_LAST_SELECTED_SESSION";
    public static final String PARAM_I_THEME = "I_THEME";
    public static final String TAG = "ZMT_Settings";
    public static final String VALUE_B_FALSE = "0";
    public static final String VALUE_B_TRUE = "1";
    public static final String VALUE_THEME_DARK = "dark";
    public static final String VALUE_THEME_LIGHT = "light";
    private static SQLiteDatabase db;
    public static int theme;

    public static void init(Context context) {
        if (db == null || !db.isOpen()) {
            db = new ZenTimerDatabase(context).getWritableDatabase();
        }
    }

    public static void close() {
        if (db == null || !db.isOpen()) {
            return;
        }
        db.close();
        db = null;
    }

    public static String getValue(String str, String str2) {
        Cursor query = db.query(ZazenTimerActivity.FRAGMENT_SETTINGS, new String[]{"value"}, "param = ?", new String[]{str}, null, null, null);
        if (query.moveToFirst()) {
            String string = query.getString(0);
            query.close();
            return string;
        }
        query.close();
        setValue(str, str2);
        return str2;
    }

    public static int getIntValue(String str, int i) {
        try {
            return Integer.parseInt(getValue(str, String.valueOf(i)));
        } catch (NumberFormatException unused) {
            return i;
        }
    }

    public static boolean getBooleanValue(String str, boolean z) {
        return getIntValue(str, z ? 1 : 0) == 1;
    }

    public static void setBooleanValue(String str, boolean z) {
        if (z) {
            setIntValue(str, 1);
        } else {
            setIntValue(str, 0);
        }
    }

    public static void setValue(String str, String str2) {
        Cursor query = db.query(ZazenTimerActivity.FRAGMENT_SETTINGS, new String[]{"value"}, "param = ?", new String[]{str}, null, null, null);
        if (query.getCount() > 0) {
            query.close();
            db.execSQL("update settings set value='" + str2 + "' where param='" + str + "'");
            return;
        }
        query.close();
        Log.d(TAG, "insert into settings (value, param, def) values (?, ?, ?): " + str2 + ", " + str + ", ''");
        db.execSQL("insert into settings (value, param, def) values (?, ?, ?)", new Object[]{str2, str, ""});
    }

    public static void setIntValue(String str, int i) {
        setValue(str, String.valueOf(i));
    }

    public static boolean paramExists(SQLiteDatabase sQLiteDatabase, String str) {
        Cursor query = sQLiteDatabase.query(ZazenTimerActivity.FRAGMENT_SETTINGS, new String[]{"value"}, "param = ?", new String[]{str}, null, null, null);
        boolean z = query.getCount() > 0;
        query.close();
        return z;
    }
}
