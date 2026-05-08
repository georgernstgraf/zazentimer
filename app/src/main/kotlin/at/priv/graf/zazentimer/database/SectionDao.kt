package at.priv.graf.zazentimer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE fk_session = :sessionId ORDER BY rank")
    fun getSectionsForSession(sessionId: Int): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE _id = :id")
    fun getSectionById(id: Int): SectionEntity?

    @Insert
    fun insert(section: SectionEntity): Long

    @Update
    fun update(section: SectionEntity)

    @Query("DELETE FROM sections WHERE _id = :id")
    fun deleteById(id: Long)

    @Query("SELECT max(rank) FROM sections WHERE fk_session = :sessionId")
    fun getMaxRank(sessionId: Int): Int?

    @Query("UPDATE sections SET rank = :rank WHERE _id = :sectionId")
    fun updateRank(sectionId: Int, rank: Int)
}
