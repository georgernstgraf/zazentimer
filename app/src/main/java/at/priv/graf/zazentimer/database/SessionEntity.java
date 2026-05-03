package at.priv.graf.zazentimer.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public int _id;
    @NonNull
    public String name;
    @NonNull
    public String description;
}
