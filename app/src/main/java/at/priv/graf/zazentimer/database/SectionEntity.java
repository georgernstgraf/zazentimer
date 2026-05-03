package at.priv.graf.zazentimer.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sections",
    foreignKeys = @ForeignKey(entity = SessionEntity.class, parentColumns = "_id", childColumns = "fk_session", onDelete = ForeignKey.CASCADE),
    indices = {@Index("fk_session")})
public class SectionEntity {
    @PrimaryKey(autoGenerate = true)
    public int _id;
    public int fk_session;
    @NonNull
    public String name;
    public int duration;
    public int bell;
    public Integer rank;
    public Integer bellcount;
    public Integer bellpause;
    public String belluri;
    public Integer volume;
}
