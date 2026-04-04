package de.gaffga.android.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
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

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "ZMT_SettingsFragment";
    private int ASK_BACKUP_EXTERNAL_STORAGE = 123;
    private int ASK_RESTORE_EXTERNAL_STORAGE = 124;

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference(ZazenTimerActivity.PREF_KEY_THEME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(requireActivity(), ZazenTimerActivity.class);
                        intent.putExtra(ZazenTimerActivity.INTENT_DATA_SHOW_PREF_ON_START, true);
                        requireActivity().finish();
                        requireActivity().startActivity(intent);
                    }
                });
                return true;
            }
        });
        final CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_OUTPUT_CHANNEL_ALARM);
        final CheckBoxPreference checkBoxPreference2 = (CheckBoxPreference) findPreference(ZazenTimerActivity.PREF_KEY_OUTPUT_CHANNEL_MUSIC);
        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference2.isChecked();
                }
                checkBoxPreference2.setChecked(false);
                return true;
            }
        });
        checkBoxPreference2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
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
        final Preference brightnessPreference = findPreference(ZazenTimerActivity.PREF_KEY_BRIGHTNESS);
        checkBoxPreference3.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference4.isChecked() || checkBoxPreference5.isChecked();
                }
                checkBoxPreference4.setChecked(false);
                checkBoxPreference5.setChecked(false);
                return true;
            }
        });
        checkBoxPreference4.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference3.isChecked() || checkBoxPreference5.isChecked();
                }
                checkBoxPreference3.setChecked(false);
                checkBoxPreference5.setChecked(false);
                return true;
            }
        });
        checkBoxPreference5.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (!((Boolean) obj).booleanValue()) {
                    return checkBoxPreference3.isChecked() || checkBoxPreference4.isChecked();
                }
                checkBoxPreference3.setChecked(false);
                checkBoxPreference4.setChecked(false);
                return true;
            }
        });
        checkBoxPreference6.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
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
            findPreference2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(requireActivity()).setTitle(R.string.restore_really_title).setMessage(R.string.restore_really_text).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            doRestore();
                        }
                    }).setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    }).show();
                    return true;
                }
            });
        }
        if (equals) {
            findPreference.setEnabled(true);
            findPreference.setSummary(R.string.pref_sum_backup);
            findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    doBackup();
                    return true;
                }
            });
        } else {
            findPreference.setEnabled(false);
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

    public void doRestore() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(requireActivity(), "android.permission.WRITE_EXTERNAL_STORAGE") == -1) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, this.ASK_RESTORE_EXTERNAL_STORAGE);
        } else {
            new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... voidArr) {
                    return Integer.valueOf(doRealRestore());
                }

                @Override
                protected void onPostExecute(Integer num) {
                    if (num.intValue() == 0) {
                        Toast.makeText(requireActivity(), R.string.restore_success_text, 0).show();
                    } else if (num.intValue() == 1) {
                        Toast.makeText(requireActivity(), R.string.restore_backup_not_found, 0).show();
                    } else if (num.intValue() == 2) {
                        Toast.makeText(requireActivity(), R.string.restore_error_text, 0).show();
                    }
                }
            }.execute(new Void[0]);
        }
    }

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
                    if (!receiveFile(zipFile.getInputStream(nextElement), requireActivity().getDatabasePath(ZenTimerDatabase.DATABASE_NAME))) {
                        z2 = true;
                    }
                    DbOperations.init(requireActivity());
                } else if (!receiveFile(zipFile.getInputStream(nextElement), new File(requireActivity().getFilesDir(), nextElement.getName()))) {
                    z2 = true;
                }
            }
            z = z2;
        } catch (Exception e) {
            Log.e(TAG, "Error restoring", e);
        }
        return z ? 2 : 0;
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == this.ASK_BACKUP_EXTERNAL_STORAGE) {
            if (iArr.length > 0 && iArr[0] == 0) {
                doBackup();
            } else {
                Toast.makeText(requireActivity(), R.string.backup_no_permission, 0).show();
            }
        } else if (i == this.ASK_RESTORE_EXTERNAL_STORAGE) {
            if (iArr.length > 0 && iArr[0] == 0) {
                doRestore();
            } else {
                Toast.makeText(requireActivity(), R.string.restore_no_permission, 0).show();
            }
        }
    }

    public void doBackup() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(requireActivity(), "android.permission.WRITE_EXTERNAL_STORAGE") == -1) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, this.ASK_BACKUP_EXTERNAL_STORAGE);
        } else {
            new AsyncTask<Activity, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Activity... activityArr) {
                    return Boolean.valueOf(doRealBackup());
                }

                @Override
                protected void onPostExecute(Boolean bool) {
                    if (bool.booleanValue()) {
                        Toast.makeText(requireActivity(), R.string.backup_success_text, 0).show();
                    } else {
                        Toast.makeText(requireActivity(), R.string.backup_error_text, 0).show();
                    }
                }
            }.execute(new Activity[0]);
        }
    }

    public boolean doRealBackup() {
        File backupFile = getBackupFile();
        if (backupFile == null) {
            Log.e(TAG, "Error creating backup file ");
            return false;
        }
        Log.d(TAG, "Trying to backup to:" + backupFile.getAbsolutePath());
        boolean r6 = false;
        try {
            FileOutputStream fos = new FileOutputStream(backupFile);
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
            java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry("zentimer");
            zos.putNextEntry(ze);
            if (!sendFile(requireActivity().getDatabasePath("zentimer"), zos)) {
                r6 = true;
            }
            zos.closeEntry();
            File filesDir = requireActivity().getFilesDir();
            File[] listFiles = filesDir.listFiles(f -> !f.getName().equals("InstantRun"));
            if (listFiles != null) {
                for (File file : listFiles) {
                    java.util.zip.ZipEntry ze2 = new java.util.zip.ZipEntry(file.getName());
                    zos.putNextEntry(ze2);
                    if (!sendFile(file, zos)) {
                        r6 = true;
                    }
                    zos.closeEntry();
                }
            }
            zos.close();
        } catch (Exception e) {
            Log.e(TAG, "IO/Error", e);
            r6 = true;
        }
        return !r6;
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
            fileOutputStream.close();
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
            fileInputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending file to zip archive", e);
            return false;
        }
    }
}
