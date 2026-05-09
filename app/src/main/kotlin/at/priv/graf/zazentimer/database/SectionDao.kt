package at.priv.graf.zazentimer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE fk_session = :sessionId ORDER BY rank")
    suspend fun getSectionsForSession(sessionId: Int): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE _id = :id")
    suspend fun getSectionById(id: Int): SectionEntity?

    @Insert
    suspend fun insert(section: SectionEntity): Long

    @Update
    suspend fun update(section: SectionEntity)

    @Query("DELETE FROM sections WHERE _id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT max(rank) FROM sections WHERE fk_session = :sessionId")
    suspend fun getMaxRank(sessionId: Int): Int?

    @Query("UPDATE sections SET rank = :rank WHERE _id = :sectionId")
    suspend fun updateRank(
        sectionId: Int,
        rank: Int,
    )
}
