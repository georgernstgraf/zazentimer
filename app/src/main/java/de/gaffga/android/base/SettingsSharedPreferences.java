package de.gaffga.android.base;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SettingsSharedPreferences implements SharedPreferences {
    private SQLiteDatabase db;
    private ArrayList<SharedPreferences.OnSharedPreferenceChangeListener> listeners = new ArrayList<>();

    @Override // android.content.SharedPreferences
    public Map<String, ?> getAll() {
        return null;
    }

    @Override // android.content.SharedPreferences
    public Set<String> getStringSet(String str, Set<String> set) {
        return null;
    }

    public class SettingsEditor implements SharedPreferences.Editor {
        private HashMap<String, String> changedValues = new HashMap<>();
        private SQLiteDatabase db;
        private SettingsSharedPreferences ssp;

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor putStringSet(String str, Set<String> set) {
            return null;
        }

        public SettingsEditor(SettingsSharedPreferences settingsSharedPreferences, SQLiteDatabase sQLiteDatabase) {
            this.db = sQLiteDatabase;
            this.ssp = settingsSharedPreferences;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor clear() {
            throw new RuntimeException("NOT SUPPORTED");
        }

        @Override // android.content.SharedPreferences.Editor
        public boolean commit() {
            for (String str : this.changedValues.keySet()) {
                Settings.setValue(str, this.changedValues.get(str));
            }
            return true;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor putBoolean(String str, boolean z) {
            if (z) {
                this.changedValues.put(str, Settings.VALUE_B_TRUE);
            } else {
                this.changedValues.put(str, Settings.VALUE_B_FALSE);
            }
            this.ssp.fireOnSharedPreferenceChange(str);
            return this;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor putFloat(String str, float f) {
            this.changedValues.put(str, String.valueOf(f));
            this.ssp.fireOnSharedPreferenceChange(str);
            return this;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor putInt(String str, int i) {
            this.changedValues.put(str, String.valueOf(i));
            this.ssp.fireOnSharedPreferenceChange(str);
            return this;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor putLong(String str, long j) {
            this.changedValues.put(str, String.valueOf(j));
            this.ssp.fireOnSharedPreferenceChange(str);
            return this;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor putString(String str, String str2) {
            this.changedValues.put(str, str2);
            this.ssp.fireOnSharedPreferenceChange(str);
            return this;
        }

        @Override // android.content.SharedPreferences.Editor
        public SharedPreferences.Editor remove(String str) {
            throw new RuntimeException("NOT SUPPORTED");
        }

        @Override // android.content.SharedPreferences.Editor
        public void apply() {
            commit();
        }
    }

    public SettingsSharedPreferences(SQLiteDatabase sQLiteDatabase) {
        this.db = sQLiteDatabase;
    }

    @Override // android.content.SharedPreferences
    public boolean contains(String str) {
        return Settings.paramExists(this.db, str);
    }

    @Override // android.content.SharedPreferences
    public SharedPreferences.Editor edit() {
        return new SettingsEditor(this, this.db);
    }

    @Override // android.content.SharedPreferences
    public boolean getBoolean(String str, boolean z) {
        return Settings.VALUE_B_TRUE.equals(Settings.getValue(str, z ? Settings.VALUE_B_TRUE : Settings.VALUE_B_FALSE));
    }

    @Override // android.content.SharedPreferences
    public float getFloat(String str, float f) {
        return Float.valueOf(Settings.getValue(str, String.valueOf(f))).floatValue();
    }

    @Override // android.content.SharedPreferences
    public int getInt(String str, int i) {
        return Settings.getIntValue(str, i);
    }

    @Override // android.content.SharedPreferences
    public long getLong(String str, long j) {
        return Long.valueOf(Settings.getValue(str, String.valueOf(j))).longValue();
    }

    @Override // android.content.SharedPreferences
    public String getString(String str, String str2) {
        return Settings.getValue(str, str2);
    }

    @Override // android.content.SharedPreferences
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        this.listeners.add(onSharedPreferenceChangeListener);
    }

    @Override // android.content.SharedPreferences
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        this.listeners.remove(onSharedPreferenceChangeListener);
    }

    public void fireOnSharedPreferenceChange(String str) {
        Iterator<SharedPreferences.OnSharedPreferenceChangeListener> it = this.listeners.iterator();
        while (it.hasNext()) {
            it.next().onSharedPreferenceChanged(this, str);
        }
    }
}
