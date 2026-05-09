package at.priv.graf.zazentimer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY name COLLATE NOCASE")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE _id = :id")
    suspend fun getSessionById(id: Int): SessionEntity?

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE _id = :id")
    suspend fun deleteById(id: Int)
}
