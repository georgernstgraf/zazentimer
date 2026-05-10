package at.priv.graf.zazentimer.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["fk_session"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fk_session")],
)
@Suppress("ConstructorParameterNaming")
data class SectionEntity(
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0,
    var fk_session: Int = 0,
    @field:NonNull
    var name: String = "",
    var duration: Int = 0,
    var bell: Int = 0,
    var rank: Int? = null,
    var bellcount: Int? = null,
    var bellpause: Int? = null,
    var belluri: String? = null,
    var volume: Int? = null,
)
