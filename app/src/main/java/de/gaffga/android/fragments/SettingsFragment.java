package de.gaffga.android.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.Toast;
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
import java.util.zip.ZipOutputStream;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "ZMT_SettingsFragment";
    private static final int REQUEST_BACKUP = 201;
    private static final int REQUEST_RESTORE = 202;

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

        // Backup via SAF — user picks where to save
        Preference backupPref = findPreference("backup_to_sd");
        backupPref.setEnabled(true);
        backupPref.setSummary(R.string.pref_sum_backup);
        backupPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, "zazentimer_backup.zip");
                startActivityForResult(intent, REQUEST_BACKUP);
                return true;
            }
        });

        // Restore via SAF — user picks the backup file
        Preference restorePref = findPreference("restore_from_sd");
        restorePref.setEnabled(true);
        restorePref.setSummary(R.string.pref_sum_restore);
        restorePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.restore_really_title)
                        .setMessage(R.string.restore_really_text)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("application/zip");
                                startActivityForResult(intent, REQUEST_RESTORE);
                            }
                        })
                        .setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .show();
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        requireActivity().getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (requestCode == REQUEST_BACKUP) {
            doBackup(uri);
        } else if (requestCode == REQUEST_RESTORE) {
            doRestore(uri);
        }
    }

    private void doBackup(Uri uri) {
        final Uri finalUri = uri;
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voidArr) {
                return Boolean.valueOf(doRealBackup(finalUri));
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success.booleanValue()) {
                    Toast.makeText(requireActivity(), R.string.backup_success_text, 0).show();
                } else {
                    Toast.makeText(requireActivity(), R.string.backup_error_text, 0).show();
                }
            }
        }.execute(new Void[0]);
    }

    private boolean doRealBackup(Uri uri) {
        Log.d(TAG, "Backup to URI: " + uri);
        boolean failed = false;
        try {
            OutputStream os = requireActivity().getContentResolver().openOutputStream(uri);
            if (os == null) {
                Log.e(TAG, "Could not open output stream for URI");
                return false;
            }
            ZipOutputStream zos = new ZipOutputStream(os);

            // Backup database
            ZipEntry ze = new ZipEntry("zentimer");
            zos.putNextEntry(ze);
            if (!sendFile(requireActivity().getDatabasePath("zentimer"), zos)) {
                failed = true;
            }
            zos.closeEntry();

            // Backup app files
            File filesDir = requireActivity().getFilesDir();
            File[] listFiles = filesDir.listFiles(f -> !f.getName().equals("InstantRun"));
            if (listFiles != null) {
                for (File file : listFiles) {
                    ZipEntry ze2 = new ZipEntry(file.getName());
                    zos.putNextEntry(ze2);
                    if (!sendFile(file, zos)) {
                        failed = true;
                    }
                    zos.closeEntry();
                }
            }
            zos.close();
        } catch (Exception e) {
            Log.e(TAG, "IO/Error during backup", e);
            failed = true;
        }
        return !failed;
    }

    private void doRestore(Uri uri) {
        final Uri finalUri = uri;
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voidArr) {
                return Integer.valueOf(doRealRestore(finalUri));
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

    private int doRealRestore(Uri uri) {
        Log.d(TAG, "Restore from URI: " + uri);
        boolean failed = false;
        try {
            // Copy URI content to a temp file so we can use ZipFile (needs seekable)
            File tempFile = File.createTempFile("restore", ".zip", requireActivity().getCacheDir());
            InputStream is = requireActivity().getContentResolver().openInputStream(uri);
            if (is == null) {
                Log.e(TAG, "Could not open input stream for URI");
                return 1;
            }
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buf = new byte[32768];
            int read;
            while ((read = is.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            fos.close();
            is.close();

            ZipFile zipFile = new ZipFile(tempFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(ZenTimerDatabase.DATABASE_NAME)) {
                    DbOperations.close();
                    if (!receiveFile(zipFile.getInputStream(entry), requireActivity().getDatabasePath(ZenTimerDatabase.DATABASE_NAME))) {
                        failed = true;
                    }
                    DbOperations.init(requireActivity());
                } else if (!receiveFile(zipFile.getInputStream(entry), new File(requireActivity().getFilesDir(), entry.getName()))) {
                    failed = true;
                }
            }
            zipFile.close();
            tempFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error restoring", e);
            failed = true;
        }
        return failed ? 2 : 0;
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
