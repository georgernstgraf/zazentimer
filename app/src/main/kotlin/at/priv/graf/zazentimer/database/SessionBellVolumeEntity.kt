package at.priv.graf.zazentimer.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_bell_volumes",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["fk_session"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fk_session"), Index(value = ["fk_session", "bell", "belluri"], unique = true)],
)
@Suppress("ConstructorParameterNaming")
data class SessionBellVolumeEntity(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,
    var fk_session: Int = 0,
    var bell: Int? = null,
    var belluri: String? = null,
    var volume: Int = 100,
)
