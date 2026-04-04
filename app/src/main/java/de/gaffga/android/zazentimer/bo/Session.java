package de.gaffga.android.zazentimer.bo;

import de.gaffga.android.base.annotations.SaveToBundle;
import de.gaffga.android.mapping.annotations.DbColumn;
import de.gaffga.android.mapping.annotations.DbPrimaryKey;
import de.gaffga.android.mapping.annotations.DbTable;

@DbTable(name = "sessions")
public class Session {

    @SaveToBundle
    @DbColumn(name = "description")
    public String description;

    @DbPrimaryKey
    @SaveToBundle
    @DbColumn(name = "_id")
    public int id;

    @SaveToBundle
    @DbColumn(name = "name")
    public String name;

    public Session() {
    }

    public Session(String str, String str2) {
        this.name = str;
        this.description = str2;
    }

    public String toString() {
        return this.name;
    }
}
