package de.gaffga.android.zazentimer;

import android.content.Context;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.android.zazentimer.database.AppDatabase;
import de.gaffga.android.zazentimer.database.SectionDao;
import de.gaffga.android.zazentimer.database.SectionEntity;
import de.gaffga.android.zazentimer.database.SessionDao;
import de.gaffga.android.zazentimer.database.SessionEntity;
import java.util.ArrayList;
import java.util.List;

public class DbOperations {
    private static AppDatabase appDb;
    private static SessionDao sessionDao;
    private static SectionDao sectionDao;

    public static void init(Context context) {
        if (appDb == null) {
            appDb = AppDatabase.getInstance(context);
            sessionDao = appDb.sessionDao();
            sectionDao = appDb.sectionDao();
        }
    }

    public static void close() {
        if (appDb != null) {
            appDb.close();
            appDb = null;
            sessionDao = null;
            sectionDao = null;
        }
    }

    public static boolean isConnected() {
        return appDb != null && appDb.isOpen();
    }

    public static Session readSession(int id) {
        SessionEntity entity = sessionDao.getSessionById(id);
        return entity != null ? toBo(entity) : null;
    }

    public static void updateSession(Session session) {
        sessionDao.update(toEntity(session));
    }

    public static void deleteSession(int id) {
        sessionDao.deleteById(id);
    }

    public static int duplicateSession(int sourceId, String newName) {
        Session source = readSession(sourceId);
        source.name = newName;
        Section[] sections = readSections(sourceId);
        insertSession(source);
        for (Section section : sections) {
            insertSection(source, section);
        }
        return source.id;
    }

    public static void deleteSection(long id) {
        sectionDao.deleteById(id);
    }

    public static Section readSection(int id) {
        SectionEntity entity = sectionDao.getSectionById(id);
        return entity != null ? toBo(entity) : null;
    }

    public static void updateSection(Section section) {
        sectionDao.update(toEntity(section));
    }

    public static void switchPositions(long id1, long id2) {
        SectionEntity s1 = sectionDao.getSectionById((int) id1);
        SectionEntity s2 = sectionDao.getSectionById((int) id2);
        if (s1 != null && s2 != null) {
            int rank1 = s1.rank != null ? s1.rank : 0;
            int rank2 = s2.rank != null ? s2.rank : 0;
            sectionDao.updateRank((int) id1, rank2);
            sectionDao.updateRank((int) id2, rank1);
        }
    }

    public static void insertSection(Session session, Section section) {
        if (section.rank == -1) {
            Integer maxRank = sectionDao.getMaxRank(session.id);
            section.rank = (maxRank != null ? maxRank : 0) + 1;
        }
        section.fkSession = session.id;
        SectionEntity entity = toEntity(section);
        long newId = sectionDao.insert(entity);
        section.id = (int) newId;
    }

    public static void insertSession(Session session) {
        SessionEntity entity = toEntity(session);
        long newId = sessionDao.insert(entity);
        session.id = (int) newId;
    }

    public static Section[] readSections(int sessionId) {
        List<SectionEntity> entities = sectionDao.getSectionsForSession(sessionId);
        ArrayList<Section> result = new ArrayList<>();
        for (SectionEntity entity : entities) {
            result.add(toBo(entity));
        }
        return result.toArray(new Section[0]);
    }

    public static Session[] readSessions() {
        List<SessionEntity> entities = sessionDao.getAllSessions();
        ArrayList<Session> result = new ArrayList<>();
        for (SessionEntity entity : entities) {
            result.add(toBo(entity));
        }
        return result.toArray(new Session[0]);
    }

    private static SessionEntity toEntity(Session bo) {
        SessionEntity entity = new SessionEntity();
        entity._id = bo.id;
        entity.name = bo.name;
        entity.description = bo.description;
        return entity;
    }

    private static Session toBo(SessionEntity entity) {
        Session bo = new Session();
        bo.id = entity._id;
        bo.name = entity.name;
        bo.description = entity.description;
        return bo;
    }

    private static SectionEntity toEntity(Section bo) {
        SectionEntity entity = new SectionEntity();
        entity._id = bo.id;
        entity.fk_session = bo.fkSession;
        entity.name = bo.name;
        entity.duration = bo.duration;
        entity.bell = bo.bell;
        entity.rank = bo.rank;
        entity.bellcount = bo.bellcount;
        entity.bellpause = bo.bellpause;
        entity.belluri = bo.bellUri;
        entity.volume = bo.volume;
        return entity;
    }

    private static Section toBo(SectionEntity entity) {
        Section bo = new Section();
        bo.id = entity._id;
        bo.fkSession = entity.fk_session;
        bo.name = entity.name;
        bo.duration = entity.duration;
        bo.bell = entity.bell;
        bo.rank = entity.rank != null ? entity.rank : -1;
        bo.bellcount = entity.bellcount != null ? entity.bellcount : 1;
        bo.bellpause = entity.bellpause != null ? entity.bellpause : 1;
        bo.bellUri = entity.belluri;
        bo.volume = entity.volume != null ? entity.volume : 100;
        return bo;
    }
}
