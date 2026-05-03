package at.priv.graf.zazentimer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY name COLLATE NOCASE")
    List<SessionEntity> getAllSessions();

    @Query("SELECT * FROM sessions WHERE _id = :id")
    SessionEntity getSessionById(int id);

    @Insert
    long insert(SessionEntity session);

    @Update
    void update(SessionEntity session);

    @Query("DELETE FROM sessions WHERE _id = :id")
    void deleteById(int id);
}
