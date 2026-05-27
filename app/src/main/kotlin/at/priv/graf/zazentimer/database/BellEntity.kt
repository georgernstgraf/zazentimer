package at.priv.graf.zazentimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bells")
data class BellEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var name: String = "",
    var uri: String = "",
    @ColumnInfo(name = "is_builtin") var isBuiltin: Boolean = false,
)
