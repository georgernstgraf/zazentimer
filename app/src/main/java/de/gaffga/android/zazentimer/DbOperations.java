package de.gaffga.android.zazentimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.gaffga.android.mapping.DbMapper;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import java.util.ArrayList;

public class DbOperations {
    private static SQLiteDatabase db;

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

    public static boolean isConnected() {
        return db != null && db.isOpen();
    }

    public static Session readSession(int i) {
        return (Session) DbMapper.readObject(db, Session.class, i);
    }

    public static void updateSession(Session session) {
        db.beginTransaction();
        DbMapper.updateObject(db, session);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public static void deleteSession(int i) {
        db.beginTransaction();
        db.delete("sections", "fk_session = ?", new String[]{"" + i});
        db.delete("sessions", "_id = ?", new String[]{"" + i});
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public static int duplicateSession(int i, String str) {
        Session readSession = readSession(i);
        readSession.name = str;
        Section[] readSections = readSections(i);
        insertSession(readSession);
        for (Section section : readSections) {
            insertSection(readSession, section);
        }
        return readSession.id;
    }

    public static void deleteSection(long j) {
        db.beginTransaction();
        db.delete("sections", "_id = ?", new String[]{"" + j});
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public static Section readSection(int i) {
        return (Section) DbMapper.readObject(db, Section.class, i);
    }

    public static void updateSection(Section section) {
        DbMapper.updateObject(db, section);
    }

    public static void switchPositions(long j, long j2) {
        Cursor query = db.query("sections", new String[]{"rank"}, "_id=?", new String[]{"" + j}, null, null, null);
        query.moveToFirst();
        int i = query.getInt(0);
        query.close();
        Cursor query2 = db.query("sections", new String[]{"rank"}, "_id=?", new String[]{"" + j2}, null, null, null);
        query2.moveToFirst();
        int i2 = query2.getInt(0);
        query2.close();
        ContentValues contentValues = new ContentValues();
        contentValues.put("rank", Integer.valueOf(i2));
        db.update("sections", contentValues, "_id=?", new String[]{"" + j});
        contentValues.put("rank", Integer.valueOf(i));
        db.update("sections", contentValues, "_id=?", new String[]{"" + j2});
        query2.close();
    }

    public static void insertSection(Session session, Section section) {
        Cursor rawQuery = db.rawQuery("SELECT max(rank) from sections where fk_session=" + session.id, null);
        if (rawQuery.moveToFirst()) {
            if (section.rank == -1) {
                section.rank = rawQuery.getInt(0) + 1;
            }
            section.fkSession = session.id;
            DbMapper.insertObject(db, section);
            rawQuery.close();
        }
    }

    public static void insertSession(Session session) {
        DbMapper.insertObject(db, session);
    }

    public static Section[] readSections(int i) {
        ArrayList arrayList = new ArrayList();
        Cursor query = db.query("sections", new String[]{"_id"}, "fk_session=?", new String[]{"" + i}, null, null, "rank");
        query.moveToFirst();
        while (!query.isAfterLast()) {
            arrayList.add((Section) DbMapper.readObject(db, Section.class, query.getInt(0)));
            query.moveToNext();
        }
        if (query != null) {
            query.close();
        }
        return (Section[]) arrayList.toArray(new Section[arrayList.size()]);
    }

    public static Session[] readSessions() {
        ArrayList arrayList = new ArrayList();
        Cursor query = db.query("sessions", new String[]{"_id"}, null, null, null, null, "name collate nocase");
        query.moveToFirst();
        while (!query.isAfterLast()) {
            arrayList.add((Session) DbMapper.readObject(db, Session.class, query.getInt(0)));
            query.moveToNext();
        }
        if (query != null) {
            query.close();
        }
        return (Session[]) arrayList.toArray(new Session[arrayList.size()]);
    }
}
