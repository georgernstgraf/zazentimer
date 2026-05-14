package at.priv.graf.zazentimer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
@Suppress("ConstructorParameterNaming")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,
    var name: String = "",
    var description: String = "",
)
