package de.gaffga.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import de.gaffga.android.base.preferences.BrightnessPreference;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.ZenTimerDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* loaded from: classes.dex */
public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = "ZMT_SettingsFragment";
    private int ASK_BACKUP_EXTERNAL_STORAGE = 123;
    private int ASK_RESTORE_EXTERNAL_STORAGE = 124;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.preferences);
        findPreference(ZazenTimerActivity.PREF_KEY_THEME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.1
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                SettingsFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: de.gaffga.android.fragments.SettingsFragment.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        Intent intent = new Intent(SettingsFragment.this.getActivity(), (Class<?>) ZazenTimerActivity.class);
                        intent.putExtra(ZazenTimerActivity.INTENT_DATA_SHOW_PREF_ON_START, true);
                        SettingsFragment.this.getActivity().finish();
                        SettingsFragment.this.getActivity().startActivity(intent);
                    }
                });
                return true;
            }
        });
        final CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_OUTPUT_CHANNEL_ALARM);
        final CheckBoxPreference checkBoxPreference2 = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_OUTPUT_CHANNEL_MUSIC);
        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.2
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference2.isChecked();
                }
                checkBoxPreference2.setChecked(false);
                return true;
            }
        });
        checkBoxPreference2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.3
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference.isChecked();
                }
                checkBoxPreference.setChecked(false);
                return true;
            }
        });
        final CheckBoxPreference checkBoxPreference3 = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND);
        final CheckBoxPreference checkBoxPreference4 = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE);
        final CheckBoxPreference checkBoxPreference5 = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE);
        CheckBoxPreference checkBoxPreference6 = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON);
        final BrightnessPreference brightnessPreference = (BrightnessPreference) findPreference(ZazenTimerActivity.PREF_KEY_BRIGHTNESS);
        checkBoxPreference3.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.4
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference4.isChecked() || checkBoxPreference5.isChecked();
                }
                checkBoxPreference4.setChecked(false);
                checkBoxPreference5.setChecked(false);
                return true;
            }
        });
        checkBoxPreference4.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.5
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference3.isChecked() || checkBoxPreference5.isChecked();
                }
                checkBoxPreference3.setChecked(false);
                checkBoxPreference5.setChecked(false);
                return true;
            }
        });
        checkBoxPreference5.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.6
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference3.isChecked() || checkBoxPreference4.isChecked();
                }
                checkBoxPreference3.setChecked(false);
                checkBoxPreference4.setChecked(false);
                return true;
            }
        });
        checkBoxPreference6.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: de.gaffga.android.fragments.SettingsFragment.7
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                brightnessPreference.setEnabled(((Boolean) obj).booleanValue());
                return true;
            }
        });
        brightnessPreference.setEnabled(checkBoxPreference6.isChecked());
        Preference findPreference = findPreference("backup_to_sd");
        Preference findPreference2 = findPreference("restore_from_sd");
        boolean equals = Environment.getExternalStorageState().equals("mounted");
        if (Environment.getExternalStorageState().equals("mounted") || Environment.getExternalStorageState().equals("mounted_ro")) {
            findPreference2.setEnabled(true);
            findPreference2.setSummary(R.string.pref_sum_restore);
            findPreference2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: de.gaffga.android.fragments.SettingsFragment.8
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(SettingsFragment.this.getActivity()).setTitle(R.string.restore_really_title).setMessage(R.string.restore_really_text).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.fragments.SettingsFragment.8.2
                        @Override // android.content.DialogInterface.OnClickListener
                        public void onClick(DialogInterface dialogInterface, int i) {
                            SettingsFragment.this.doRestore();
                        }
                    }).setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.fragments.SettingsFragment.8.1
                        @Override // android.content.DialogInterface.OnClickListener
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    }).show();
                    return true;
                }
            });
        } else {
            String externalStorageState = Environment.getExternalStorageState();
            if (externalStorageState.equals("bad_removal")) {
                findPreference.setSummary(R.string.pref_sum_bad_removal);
            } else if (externalStorageState.equals("checking")) {
                findPreference.setSummary(R.string.pref_sum_checking);
            } else if (externalStorageState.equals("ejecting")) {
                findPreference.setSummary(R.string.pref_sum_ejecting);
            } else if (externalStorageState.equals("nofs")) {
                findPreference.setSummary(R.string.pref_sum_nofs);
            } else if (externalStorageState.equals("removed")) {
                findPreference.setSummary(R.string.pref_sum_removed);
            } else if (externalStorageState.equals("shared")) {
                findPreference.setSummary(R.string.pref_sum_shared);
            }
        }
        if (equals) {
            findPreference.setEnabled(true);
            findPreference.setSummary(R.string.pref_sum_backup);
            findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: de.gaffga.android.fragments.SettingsFragment.9
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    SettingsFragment.this.doBackup();
                    return true;
                }
            });
            return;
        }
        findPreference.setEnabled(false);
        String externalStorageState2 = Environment.getExternalStorageState();
        if (externalStorageState2.equals("bad_removal")) {
            findPreference.setSummary(R.string.pref_sum_bad_removal);
            return;
        }
        if (externalStorageState2.equals("checking")) {
            findPreference.setSummary(R.string.pref_sum_checking);
            return;
        }
        if (externalStorageState2.equals("ejecting")) {
            findPreference.setSummary(R.string.pref_sum_ejecting);
            return;
        }
        if (externalStorageState2.equals("mounted_ro")) {
            findPreference.setSummary(R.string.pref_sum_read_only);
            return;
        }
        if (externalStorageState2.equals("nofs")) {
            findPreference.setSummary(R.string.pref_sum_nofs);
        } else if (externalStorageState2.equals("removed")) {
            findPreference.setSummary(R.string.pref_sum_removed);
        } else if (externalStorageState2.equals("shared")) {
            findPreference.setSummary(R.string.pref_sum_shared);
        }
    }

    private File getBackupDir() {
        File file = new File(Environment.getExternalStorageDirectory(), "zazentimer");
        Log.d(TAG, "targetDir=" + file.getAbsolutePath());
        if (file.exists() || file.mkdirs()) {
            return file;
        }
        Log.e(TAG, "error creating dir " + file.getAbsolutePath());
        return null;
    }

    private File getBackupFile() {
        File backupDir = getBackupDir();
        if (backupDir == null) {
            return null;
        }
        return new File(backupDir, "backup.zip");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doRestore() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getActivity(), "android.permission.WRITE_EXTERNAL_STORAGE") == -1) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, this.ASK_RESTORE_EXTERNAL_STORAGE);
        } else {
            new AsyncTask<Void, Void, Integer>() { // from class: de.gaffga.android.fragments.SettingsFragment.10
                /* JADX INFO: Access modifiers changed from: protected */
                @Override // android.os.AsyncTask
                public Integer doInBackground(Void... voidArr) {
                    return Integer.valueOf(SettingsFragment.this.doRealRestore());
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // android.os.AsyncTask
                public void onPostExecute(Integer num) {
                    if (num.intValue() == 0) {
                        Toast.makeText(SettingsFragment.this.getActivity(), R.string.restore_success_text, 0).show();
                    } else if (num.intValue() == 1) {
                        Toast.makeText(SettingsFragment.this.getActivity(), R.string.restore_backup_not_found, 0).show();
                    } else if (num.intValue() == 2) {
                        Toast.makeText(SettingsFragment.this.getActivity(), R.string.restore_error_text, 0).show();
                    }
                }
            }.execute(new Void[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int doRealRestore() {
        File backupFile = getBackupFile();
        boolean z = true;
        if (backupFile == null || !backupFile.exists()) {
            Log.e(TAG, "no backup file found");
            return 1;
        }
        try {
            ZipFile zipFile = new ZipFile(backupFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            boolean z2 = false;
            while (entries.hasMoreElements()) {
                ZipEntry nextElement = entries.nextElement();
                if (nextElement.getName().equals(ZenTimerDatabase.DATABASE_NAME)) {
                    DbOperations.close();
                    if (!receiveFile(zipFile.getInputStream(nextElement), getActivity().getDatabasePath(ZenTimerDatabase.DATABASE_NAME))) {
                        z2 = true;
                    }
                    DbOperations.init(getActivity());
                } else if (!receiveFile(zipFile.getInputStream(nextElement), new File(getActivity().getFilesDir(), nextElement.getName()))) {
                    z2 = true;
                }
            }
            z = z2;
        } catch (Exception e) {
            Log.e(TAG, "Error restoring", e);
        }
        return z ? 2 : 0;
    }

    @Override // android.app.Fragment
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == this.ASK_BACKUP_EXTERNAL_STORAGE) {
            if (iArr.length > 0 && iArr[0] == 0) {
                doBackup();
                return;
            } else {
                Toast.makeText(getActivity(), R.string.backup_no_permission, 0).show();
                return;
            }
        }
        if (i == this.ASK_RESTORE_EXTERNAL_STORAGE) {
            if (iArr.length > 0 && iArr[0] == 0) {
                doRestore();
            } else {
                Toast.makeText(getActivity(), R.string.restore_no_permission, 0).show();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doBackup() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getActivity(), "android.permission.WRITE_EXTERNAL_STORAGE") == -1) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, this.ASK_BACKUP_EXTERNAL_STORAGE);
        } else {
            new AsyncTask<Activity, Void, Boolean>() { // from class: de.gaffga.android.fragments.SettingsFragment.11
                /* JADX INFO: Access modifiers changed from: protected */
                @Override // android.os.AsyncTask
                public Boolean doInBackground(Activity... activityArr) {
                    return Boolean.valueOf(SettingsFragment.this.doRealBackup());
                }

                /* JADX INFO: Access modifiers changed from: protected */
                @Override // android.os.AsyncTask
                public void onPostExecute(Boolean bool) {
                    if (bool.booleanValue()) {
                        Toast.makeText(SettingsFragment.this.getActivity(), R.string.backup_success_text, 0).show();
                    } else {
                        Toast.makeText(SettingsFragment.this.getActivity(), R.string.backup_error_text, 0).show();
                    }
                }
            }.execute(new Activity[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x009f, code lost:
    
        if (getBackupFile().exists() == false) goto L18;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean doRealBackup() {
        /*
            r10 = this;
            java.io.File r0 = r10.getBackupFile()
            r1 = 0
            if (r0 != 0) goto L22
            java.lang.String r2 = "ZMT_SettingsFragment"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Error creating backup file "
            r3.append(r4)
            java.lang.String r0 = r0.getAbsolutePath()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.e(r2, r0)
            return r1
        L22:
            java.lang.String r2 = "ZMT_SettingsFragment"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Trying to backup to:"
            r3.append(r4)
            java.lang.String r4 = r0.getAbsolutePath()
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.d(r2, r3)
            r2 = 1
            java.io.FileOutputStream r3 = new java.io.FileOutputStream     // Catch: java.io.IOException -> La2
            r3.<init>(r0)     // Catch: java.io.IOException -> La2
            java.util.zip.ZipOutputStream r0 = new java.util.zip.ZipOutputStream     // Catch: java.io.IOException -> La2
            r0.<init>(r3)     // Catch: java.io.IOException -> La2
            java.util.zip.ZipEntry r3 = new java.util.zip.ZipEntry     // Catch: java.io.IOException -> La2
            java.lang.String r4 = "zentimer"
            r3.<init>(r4)     // Catch: java.io.IOException -> La2
            r0.putNextEntry(r3)     // Catch: java.io.IOException -> La2
            android.app.Activity r3 = r10.getActivity()     // Catch: java.io.IOException -> La2
            java.lang.String r4 = "zentimer"
            java.io.File r3 = r3.getDatabasePath(r4)     // Catch: java.io.IOException -> La2
            boolean r3 = r10.sendFile(r3, r0)     // Catch: java.io.IOException -> La2
            r3 = r3 ^ r2
            r0.closeEntry()     // Catch: java.io.IOException -> La2
            android.app.Activity r4 = r10.getActivity()     // Catch: java.io.IOException -> La2
            java.io.File r4 = r4.getFilesDir()     // Catch: java.io.IOException -> La2
            de.gaffga.android.fragments.SettingsFragment$12 r5 = new de.gaffga.android.fragments.SettingsFragment$12     // Catch: java.io.IOException -> La2
            r5.<init>()     // Catch: java.io.IOException -> La2
            java.io.File[] r4 = r4.listFiles(r5)     // Catch: java.io.IOException -> La2
            int r5 = r4.length     // Catch: java.io.IOException -> La2
            r6 = r3
            r3 = r1
        L77:
            if (r3 >= r5) goto L94
            r7 = r4[r3]     // Catch: java.io.IOException -> La2
            java.util.zip.ZipEntry r8 = new java.util.zip.ZipEntry     // Catch: java.io.IOException -> La2
            java.lang.String r9 = r7.getName()     // Catch: java.io.IOException -> La2
            r8.<init>(r9)     // Catch: java.io.IOException -> La2
            r0.putNextEntry(r8)     // Catch: java.io.IOException -> La2
            boolean r7 = r10.sendFile(r7, r0)     // Catch: java.io.IOException -> La2
            if (r7 != 0) goto L8e
            r6 = r2
        L8e:
            r0.closeEntry()     // Catch: java.io.IOException -> La2
            int r3 = r3 + 1
            goto L77
        L94:
            r0.close()     // Catch: java.io.IOException -> La2
            java.io.File r0 = r10.getBackupFile()     // Catch: java.io.IOException -> La2
            boolean r0 = r0.exists()     // Catch: java.io.IOException -> La2
            if (r0 != 0) goto Lab
            goto Laa
        La2:
            r0 = move-exception
            java.lang.String r3 = "ZMT_SettingsFragment"
            java.lang.String r4 = "IO/Error"
            android.util.Log.e(r3, r4, r0)
        Laa:
            r6 = r2
        Lab:
            if (r6 != 0) goto Lae
            r1 = r2
        Lae:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: de.gaffga.android.fragments.SettingsFragment.doRealBackup():boolean");
    }

    private boolean receiveFile(InputStream inputStream, File file) {
        Log.i(TAG, "receiving File from zip: " + file.getName());
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] bArr = new byte[32768];
            int read = inputStream.read(bArr);
            while (read > 0) {
                fileOutputStream.write(bArr, 0, read);
                read = inputStream.read(bArr);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error receiving file from zip archive", e);
            return false;
        }
    }

    private boolean sendFile(File file, OutputStream outputStream) {
        Log.i(TAG, "sending File to zip: " + file.getName());
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bArr = new byte[32768];
            for (int read = fileInputStream.read(bArr); read > 0; read = fileInputStream.read(bArr)) {
                outputStream.write(bArr, 0, read);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending file to zip archive", e);
            return false;
        }
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }
}
