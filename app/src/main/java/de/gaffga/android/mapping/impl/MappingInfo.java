package de.gaffga.android.mapping.impl;

public class MappingInfo {
    public Column[] columns;
    public String primaryKey;
    public String tableName;

    public Column getPrimaryKey() {
        for (int i = 0; i < this.columns.length; i++) {
            if (this.columns[i].name.equals(this.primaryKey)) {
                return this.columns[i];
            }
        }
        return null;
    }
}
