package at.priv.graf.zazentimer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionBellVolumeDao {
    @Query("SELECT * FROM session_bell_volumes WHERE fk_session = :sessionId")
    suspend fun getBellVolumesForSession(sessionId: Int): List<SessionBellVolumeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(volume: SessionBellVolumeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(volumes: List<SessionBellVolumeEntity>)

    @Update
    suspend fun update(volume: SessionBellVolumeEntity)

    @Query("DELETE FROM session_bell_volumes WHERE fk_session = :sessionId")
    suspend fun deleteForSession(sessionId: Int)

    @Query("DELETE FROM session_bell_volumes WHERE _id = :id")
    suspend fun deleteById(id: Long)
}
