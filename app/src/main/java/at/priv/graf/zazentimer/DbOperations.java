package at.priv.graf.zazentimer;

import android.content.Context;
import androidx.room.Room;
import at.priv.graf.zazentimer.bo.Section;
import at.priv.graf.zazentimer.bo.Session;
import at.priv.graf.zazentimer.database.AppDatabase;
import at.priv.graf.zazentimer.database.SectionDao;
import at.priv.graf.zazentimer.database.SectionEntity;
import at.priv.graf.zazentimer.database.SessionDao;
import at.priv.graf.zazentimer.database.SessionEntity;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DbOperations {
    private final Context context;
    private AppDatabase appDb;
    private SessionDao sessionDao;
    private SectionDao sectionDao;

    @Inject
    public DbOperations(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
        openDatabase();
    }

    private void openDatabase() {
        this.appDb = Room.databaseBuilder(context,
                AppDatabase.class, AppDatabase.DATABASE_NAME)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .addCallback(AppDatabase.ON_CREATE_CALLBACK)
                .allowMainThreadQueries()
                .build();
        this.sessionDao = appDb.sessionDao();
        this.sectionDao = appDb.sectionDao();
    }

    public void close() {
        if (appDb != null) {
            appDb.close();
            appDb = null;
            sessionDao = null;
            sectionDao = null;
        }
    }

    public void reopen() {
        close();
        openDatabase();
    }

    public boolean isConnected() {
        return appDb != null && appDb.isOpen();
    }

    public Session readSession(int id) {
        SessionEntity entity = sessionDao.getSessionById(id);
        return entity != null ? toBo(entity) : null;
    }

    public void updateSession(Session session) {
        sessionDao.update(toEntity(session));
    }

    public void deleteSession(int id) {
        sessionDao.deleteById(id);
    }

    public int duplicateSession(int sourceId, String newName) {
        Session source = readSession(sourceId);
        source.name = newName;
        source.id = 0;
        Section[] sections = readSections(sourceId);
        insertSession(source);
        for (Section section : sections) {
            section.id = 0;
            insertSection(source, section);
        }
        return source.id;
    }

    public void deleteSection(long id) {
        sectionDao.deleteById(id);
    }

    public Section readSection(int id) {
        SectionEntity entity = sectionDao.getSectionById(id);
        return entity != null ? toBo(entity) : null;
    }

    public void updateSection(Section section) {
        sectionDao.update(toEntity(section));
    }

    public void switchPositions(long id1, long id2) {
        SectionEntity s1 = sectionDao.getSectionById((int) id1);
        SectionEntity s2 = sectionDao.getSectionById((int) id2);
        if (s1 != null && s2 != null) {
            int rank1 = s1.rank != null ? s1.rank : 0;
            int rank2 = s2.rank != null ? s2.rank : 0;
            sectionDao.updateRank((int) id1, rank2);
            sectionDao.updateRank((int) id2, rank1);
        }
    }

    public void insertSection(Session session, Section section) {
        if (section.rank == -1) {
            Integer maxRank = sectionDao.getMaxRank(session.id);
            section.rank = (maxRank != null ? maxRank : 0) + 1;
        }
        section.fkSession = session.id;
        SectionEntity entity = toEntity(section);
        long newId = sectionDao.insert(entity);
        section.id = (int) newId;
    }

    public void insertSession(Session session) {
        SessionEntity entity = toEntity(session);
        long newId = sessionDao.insert(entity);
        session.id = (int) newId;
    }

    public Section[] readSections(int sessionId) {
        List<SectionEntity> entities = sectionDao.getSectionsForSession(sessionId);
        ArrayList<Section> result = new ArrayList<>();
        for (SectionEntity entity : entities) {
            result.add(toBo(entity));
        }
        return result.toArray(new Section[0]);
    }

    public Session[] readSessions() {
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
