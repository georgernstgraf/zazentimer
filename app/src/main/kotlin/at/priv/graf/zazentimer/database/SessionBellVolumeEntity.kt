package at.priv.graf.zazentimer.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import at.priv.graf.zazentimer.Constants

@Entity(
    tableName = "session_bell_volumes",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["fk_session"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BellEntity::class,
            parentColumns = ["_id"],
            childColumns = ["bellId"],
        ),
    ],
    indices = [Index("fk_session"), Index(value = ["fk_session", "bell", "belluri"], unique = true), Index("bellId")],
)
@Suppress("ConstructorParameterNaming")
data class SessionBellVolumeEntity(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,
    var fk_session: Int = 0,
    var bell: Int? = null,
    var belluri: String? = null,
    var bellId: Int = 0,
    var volume: Int = Constants.DEFAULT_BELL_VOLUME,
)
