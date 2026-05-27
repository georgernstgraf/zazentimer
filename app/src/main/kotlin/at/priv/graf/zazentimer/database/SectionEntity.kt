package at.priv.graf.zazentimer.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["fk_session"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BellEntity::class,
            parentColumns = ["id"],
            childColumns = ["bellId"],
        ),
    ],
    indices = [Index("fk_session"), Index("bellId")],
)
@Suppress("ConstructorParameterNaming")
data class SectionEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String = "",
    var duration: Int = 0,
    var rank: Int = 0,
    var bellcount: Int = 1,
    var bellpause: Int = 1,
    var bellId: Int = 0,
    var fk_session: Int = 0,
)
