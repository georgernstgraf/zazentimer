package de.gaffga.android.zazentimer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public int _id;
    public String name;
    public String description;
}
