package at.priv.graf.zazentimer.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,
    @field:NonNull
    var name: String = "",
    @field:NonNull
    var description: String = ""
)
