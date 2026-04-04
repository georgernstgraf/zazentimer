package de.gaffga.android.mapping;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.gaffga.android.mapping.annotations.DbColumn;
import de.gaffga.android.mapping.annotations.DbPrimaryKey;
import de.gaffga.android.mapping.annotations.DbTable;
import de.gaffga.android.mapping.exceptions.InvalidClassMappingException;
import de.gaffga.android.mapping.exceptions.InvalidTypeMappingException;
import de.gaffga.android.mapping.exceptions.MappingReflectionException;
import de.gaffga.android.mapping.exceptions.MappingSQLException;
import de.gaffga.android.mapping.impl.Column;
import de.gaffga.android.mapping.impl.MappingInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class DbMapper {
    private static final boolean DEBUG = false;
    private static final String TAG = "ZMT_DbMapper";

    public static <T> void insertObject(SQLiteDatabase sQLiteDatabase, T t) throws InvalidClassMappingException, InvalidTypeMappingException, MappingSQLException, MappingReflectionException {
        MappingInfo mappingInfo = getMappingInfo(t.getClass());
        if (mappingInfo == null) {
            Log.e(TAG, "Mapping Info is null! Can't handle this object!");
            throw new InvalidClassMappingException("Mapping-Info is null");
        }
        ContentValues contentValues = new ContentValues();
        for (Column column : mappingInfo.columns) {
            String str = column.name;
            if (!column.name.equals(mappingInfo.primaryKey)) {
                try {
                    if (column.type.equals("int")) {
                        contentValues.put(str, Integer.valueOf(t.getClass().getField(column.field).getInt(t)));
                    } else if (column.type.equals("String")) {
                        contentValues.put(str, (String) t.getClass().getField(column.field).get(t));
                    } else {
                        Log.e(TAG, "type '" + column.type + "' of column '" + column.name + "' is unknown");
                        throw new InvalidTypeMappingException("type '" + column.type + "' of column '" + column.name + "' is unknown");
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Setting insert values: Illegal access", e);
                    throw new MappingReflectionException("Setting insert values: Illegal access", e);
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Setting insert values: Illegal argument", e2);
                    throw new MappingReflectionException("Setting insert values: Illegal argument", e2);
                } catch (NoSuchFieldException e3) {
                    Log.e(TAG, "Setting insert values: Field not found", e3);
                    throw new MappingReflectionException("Setting insert values: Field not found", e3);
                } catch (SecurityException e4) {
                    Log.e(TAG, "Setting insert values: Security exception", e4);
                    throw new MappingReflectionException("Setting insert values: Security exception", e4);
                }
            }
        }
        try {
            int insertOrThrow = (int) sQLiteDatabase.insertOrThrow(mappingInfo.tableName, null, contentValues);
            Column primaryKey = mappingInfo.getPrimaryKey();
            if (primaryKey != null) {
                try {
                    t.getClass().getField(primaryKey.field).setInt(t, insertOrThrow);
                } catch (IllegalAccessException e5) {
                    Log.e(TAG, "Setting primary key id: Illegal access", e5);
                    throw new MappingReflectionException("Setting primary key id: Illegal access", e5);
                } catch (IllegalArgumentException e6) {
                    Log.e(TAG, "Setting primary key id: Illegal argument", e6);
                    throw new MappingReflectionException("Setting primary key id: Illegal argument", e6);
                } catch (NoSuchFieldException e7) {
                    Log.e(TAG, "Setting primary key id: Field not found", e7);
                    throw new MappingReflectionException("Setting primary key id: Field not found", e7);
                } catch (SecurityException e8) {
                    Log.e(TAG, "Setting primary key id: Security exception", e8);
                    throw new MappingReflectionException("Setting primary key id: Security exception", e8);
                }
            }
        } catch (SQLException e9) {
            Log.e(TAG, "Error inserting", e9);
            throw new MappingSQLException("Error inserting", e9);
        }
    }

    public static <T> void updateObject(SQLiteDatabase sQLiteDatabase, T t) throws InvalidClassMappingException, InvalidTypeMappingException, MappingSQLException, MappingReflectionException {
        MappingInfo mappingInfo = getMappingInfo(t.getClass());
        if (mappingInfo == null) {
            Log.e(TAG, "Mapping Info is null! Can't handle this object!");
            throw new InvalidClassMappingException("Mapping-Info is null");
        }
        ContentValues contentValues = new ContentValues();
        for (Column column : mappingInfo.columns) {
            String str = column.name;
            if (!column.name.equals(mappingInfo.primaryKey)) {
                try {
                    if (column.type.equals("int")) {
                        contentValues.put(str, Integer.valueOf(t.getClass().getField(column.field).getInt(t)));
                    } else if (column.type.equals("String")) {
                        contentValues.put(str, (String) t.getClass().getField(column.field).get(t));
                    } else {
                        Log.e(TAG, "type '" + column.type + "' of column '" + column.name + "' is unknown");
                        throw new InvalidTypeMappingException("type '" + column.type + "' of column '" + column.name + "' is unknown");
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Setting insert values: Illegal access", e);
                    throw new MappingReflectionException("Setting insert values: Illegal access", e);
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Setting insert values: Illegal argument", e2);
                    throw new MappingReflectionException("Setting insert values: Illegal argument", e2);
                } catch (NoSuchFieldException e3) {
                    Log.e(TAG, "Setting insert values: Field not found", e3);
                    throw new MappingReflectionException("Setting insert values: Field not found", e3);
                } catch (SecurityException e4) {
                    Log.e(TAG, "Setting insert values: Security exception", e4);
                    throw new MappingReflectionException("Setting insert values: Security exception", e4);
                }
            }
        }
        int i = -1;
        Column primaryKey = mappingInfo.getPrimaryKey();
        if (primaryKey != null) {
            try {
                i = t.getClass().getField(primaryKey.field).getInt(t);
            } catch (IllegalAccessException e5) {
                Log.e(TAG, "Setting primary key id: Illegal access", e5);
                throw new MappingReflectionException("Setting primary key id: Illegal access", e5);
            } catch (IllegalArgumentException e6) {
                Log.e(TAG, "Setting primary key id: Illegal argument", e6);
                throw new MappingReflectionException("Setting primary key id: Illegal argument", e6);
            } catch (NoSuchFieldException e7) {
                Log.e(TAG, "Setting primary key id: Field not found", e7);
                throw new MappingReflectionException("Setting primary key id: Field not found", e7);
            } catch (SecurityException e8) {
                Log.e(TAG, "Setting primary key id: Security exception", e8);
                throw new MappingReflectionException("Setting primary key id: Security exception", e8);
            }
        }
        try {
            int update = sQLiteDatabase.update(mappingInfo.tableName, contentValues, "_id=?", new String[]{"" + i});
            if (update != 1) {
                throw new MappingSQLException("Update should affect 1 row, but affected " + update);
            }
        } catch (SQLException e9) {
            Log.e(TAG, "Error inserting", e9);
            throw new MappingSQLException("Error inserting", e9);
        }
    }

    public static <T> T readObject(SQLiteDatabase sQLiteDatabase, Class<T> cls, int i) throws MappingReflectionException, InvalidClassMappingException, InvalidTypeMappingException {
        try {
            T newInstance = cls.newInstance();
            MappingInfo mappingInfo = getMappingInfo(cls);
            if (mappingInfo == null) {
                throw new InvalidClassMappingException("Invalid class: mapping info is null");
            }
            String str = mappingInfo.primaryKey + " = ?";
            String[] strArr = {"" + i};
            String[] strArr2 = new String[mappingInfo.columns.length];
            for (int i2 = 0; i2 < strArr2.length; i2++) {
                strArr2[i2] = mappingInfo.columns[i2].name;
            }
            Cursor query = sQLiteDatabase.query(mappingInfo.tableName, strArr2, str, strArr, null, null, null);
            if (query.getCount() > 0) {
                query.moveToFirst();
                for (Column column : mappingInfo.columns) {
                    try {
                        if (column.type.equals("String")) {
                            cls.getField(column.field).set(newInstance, query.getString(query.getColumnIndex(column.name)));
                        } else if (column.type.equals("int")) {
                            cls.getField(column.field).setInt(newInstance, query.getInt(query.getColumnIndex(column.name)));
                        } else {
                            Log.e(TAG, "type '" + column.type + "' of column '" + column.name + "' is unknown");
                            throw new InvalidTypeMappingException("type '" + column.type + "' of column '" + column.name + "' is unknown");
                        }
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "Setting field value: Illegal access", e);
                        throw new MappingReflectionException("Setting field value: Illegal access", e);
                    } catch (IllegalArgumentException e2) {
                        Log.e(TAG, "Setting field value: Illegal argument", e2);
                        throw new MappingReflectionException("Setting field value: Illegal argument", e2);
                    } catch (NoSuchFieldException e3) {
                        Log.e(TAG, "Setting field value: Field not found", e3);
                        throw new MappingReflectionException("Setting field value: Field not found", e3);
                    } catch (SecurityException e4) {
                        Log.e(TAG, "Setting field value: Security exception", e4);
                        throw new MappingReflectionException("Setting field value: Security exception", e4);
                    }
                }
                query.close();
                return newInstance;
            }
            query.close();
            return null;
        } catch (IllegalAccessException e5) {
            Log.e(TAG, "Illegal access when instantiating class '" + cls.getName() + "'", e5);
            throw new MappingReflectionException("Illegal access when instantiating class '" + cls.getName() + "'", e5);
        } catch (InstantiationException e6) {
            Log.e(TAG, "Could not instantiate class '" + cls.getName() + "'", e6);
            throw new MappingReflectionException("Could not instantiate class '" + cls.getName() + "'", e6);
        }
    }

    private static <T> MappingInfo getMappingInfo(Class<T> cls) {
        MappingInfo mappingInfo = new MappingInfo();
        if (!cls.isAnnotationPresent(DbTable.class)) {
            Log.e(TAG, "Missing @DbTable annotation for class: " + cls.getName());
            return null;
        }
        String name = ((DbTable) cls.getAnnotation(DbTable.class)).name();
        if (name.trim().length() == 0) {
            Log.e(TAG, "Empty name in @DbTable annotation for class: " + cls.getName());
            return null;
        }
        mappingInfo.tableName = name;
        ArrayList arrayList = new ArrayList();
        for (Field field : cls.getFields()) {
            if (field.isAnnotationPresent(DbColumn.class)) {
                String name2 = ((DbColumn) field.getAnnotation(DbColumn.class)).name();
                arrayList.add(new Column(name2, field.getType().getSimpleName(), field.getName()));
                if (field.isAnnotationPresent(DbPrimaryKey.class)) {
                    mappingInfo.primaryKey = name2;
                }
            }
        }
        mappingInfo.columns = (Column[]) arrayList.toArray(new Column[arrayList.size()]);
        if (mappingInfo.columns.length != 0) {
            return mappingInfo;
        }
        Log.e(TAG, "No columns found in class '" + cls.getName() + "'. Does it use public for persisted fields?");
        return null;
    }
}
