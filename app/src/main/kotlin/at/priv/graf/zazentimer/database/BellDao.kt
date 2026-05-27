package at.priv.graf.zazentimer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BellDao {
    @Query("SELECT * FROM bells")
    suspend fun getAll(): List<BellEntity>

    @Query("SELECT * FROM bells WHERE id = :id")
    suspend fun getById(id: Int): BellEntity?

    @Query("SELECT * FROM bells WHERE uri = :uri")
    suspend fun getByUri(uri: String): BellEntity?

    @Query("SELECT * FROM bells WHERE is_builtin = 1")
    suspend fun getBuiltinBells(): List<BellEntity>

    @Query("SELECT * FROM bells WHERE is_builtin = 0")
    suspend fun getNonBuiltinBells(): List<BellEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bell: BellEntity): Long

    @Update
    suspend fun update(bell: BellEntity)

    @Query("DELETE FROM bells WHERE id = :id")
    suspend fun deleteById(id: Int)
}
