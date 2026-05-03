package at.priv.graf.zazentimer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SectionDao {
    @Query("SELECT * FROM sections WHERE fk_session = :sessionId ORDER BY rank")
    List<SectionEntity> getSectionsForSession(int sessionId);

    @Query("SELECT * FROM sections WHERE _id = :id")
    SectionEntity getSectionById(int id);

    @Insert
    long insert(SectionEntity section);

    @Update
    void update(SectionEntity section);

    @Query("DELETE FROM sections WHERE _id = :id")
    void deleteById(long id);

    @Query("SELECT max(rank) FROM sections WHERE fk_session = :sessionId")
    Integer getMaxRank(int sessionId);

    @Query("UPDATE sections SET rank = :rank WHERE _id = :sectionId")
    void updateRank(int sectionId, int rank);
}
