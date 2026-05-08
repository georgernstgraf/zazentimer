package at.priv.graf.zazentimer.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.DbOperations
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Enumeration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val uiHandler: Handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var dbOperations: DbOperations

    private val backupLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
        val uri = result.data!!.data
        if (uri == null) return@registerForActivityResult
        requireActivity().contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        doBackup(uri)
    }

    private val restoreLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
        val uri = result.data!!.data
        if (uri == null) return@registerForActivityResult
        requireActivity().contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        doRestore(uri)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialFadeThrough()
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>(ZazenTimerActivity.PREF_KEY_THEME)!!.setOnPreferenceChangeListener { _, _ ->
            requireActivity().runOnUiThread {
                val intent = Intent(requireActivity(), ZazenTimerActivity::class.java)
                intent.putExtra(ZazenTimerActivity.INTENT_DATA_SHOW_PREF_ON_START, true)
                requireActivity().finish()
                requireActivity().startActivity(intent)
            }
            true
        }

        val checkBoxPreference3 = findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND)!!
        val checkBoxPreference4 = findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE)!!
        val checkBoxPreference5 = findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE)!!
        val checkBoxPreference6 = findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON)!!
        val brightnessPreference = findPreference<Preference>(ZazenTimerActivity.PREF_KEY_BRIGHTNESS)!!

        checkBoxPreference3.setOnPreferenceChangeListener { _, obj ->
            if (!(obj as Boolean)) {
                return@setOnPreferenceChangeListener checkBoxPreference4.isChecked || checkBoxPreference5.isChecked
            }
            checkBoxPreference4.isChecked = false
            checkBoxPreference5.isChecked = false
            true
        }
        checkBoxPreference4.setOnPreferenceChangeListener { _, obj ->
            if (!(obj as Boolean)) {
                return@setOnPreferenceChangeListener checkBoxPreference3.isChecked || checkBoxPreference5.isChecked
            }
            checkBoxPreference3.isChecked = false
            checkBoxPreference5.isChecked = false
            true
        }
        checkBoxPreference5.setOnPreferenceChangeListener { _, obj ->
            if (!(obj as Boolean)) {
                return@setOnPreferenceChangeListener checkBoxPreference3.isChecked || checkBoxPreference4.isChecked
            }
            checkBoxPreference3.isChecked = false
            checkBoxPreference4.isChecked = false
            true
        }
        checkBoxPreference6.setOnPreferenceChangeListener { _, obj ->
            brightnessPreference.isEnabled = obj as Boolean
            true
        }
        brightnessPreference.isEnabled = checkBoxPreference6.isChecked

        val backupPref = findPreference<Preference>("backup_to_sd")!!
        backupPref.isEnabled = true
        backupPref.setSummary(R.string.pref_sum_backup)
        backupPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/zip"
            intent.putExtra(Intent.EXTRA_TITLE, "zazentimer_backup.zip")
            backupLauncher.launch(intent)
            true
        }

        val restorePref = findPreference<Preference>("restore_from_sd")!!
        restorePref.isEnabled = true
        restorePref.setSummary(R.string.pref_sum_restore)
        restorePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.restore_really_title)
                .setMessage(R.string.restore_really_text)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/zip"
                    restoreLauncher.launch(intent)
                }
                .setNegativeButton(R.string.abbrechen) { _: DialogInterface, _: Int ->
                }
                .show()
            true
        }
    }

    private fun doBackup(uri: Uri) {
        executor.execute {
            val success = doRealBackup(uri)
            uiHandler.post {
                if (success) {
                    Toast.makeText(requireActivity(), R.string.backup_success_text, 0).show()
                } else {
                    Toast.makeText(requireActivity(), R.string.backup_error_text, 0).show()
                }
            }
        }
    }

    private fun doRealBackup(uri: Uri): Boolean {
        Log.d(TAG, "Backup to URI: $uri")
        var failed = false
        try {
            dbOperations.close()
            val os = requireActivity().contentResolver.openOutputStream(uri)
            if (os == null) {
                Log.e(TAG, "Could not open output stream for URI")
                dbOperations.reopen()
                return false
            }
            val zos = ZipOutputStream(os)
            val ze = ZipEntry("zentimer")
            zos.putNextEntry(ze)
            if (!sendFile(requireActivity().getDatabasePath("zentimer"), zos)) {
                failed = true
            }
            zos.closeEntry()
            val filesDir = requireActivity().filesDir
            val listFiles = filesDir.listFiles { f -> f.name != "InstantRun" }
            if (listFiles != null) {
                for (file in listFiles) {
                    val ze2 = ZipEntry(file.name)
                    zos.putNextEntry(ze2)
                    if (!sendFile(file, zos)) {
                        failed = true
                    }
                    zos.closeEntry()
                }
            }
            zos.close()
            dbOperations.reopen()
        } catch (e: Exception) {
            Log.e(TAG, "IO/Error during backup", e)
            failed = true
            dbOperations.reopen()
        }
        return !failed
    }

    private fun doRestore(uri: Uri) {
        executor.execute {
            val result = doRealRestore(uri)
            uiHandler.post {
                if (result == 0) {
                    Toast.makeText(requireActivity(), R.string.restore_success_text, 0).show()
                } else if (result == 1) {
                    Toast.makeText(requireActivity(), R.string.restore_backup_not_found, 0).show()
                } else if (result == 2) {
                    Toast.makeText(requireActivity(), R.string.restore_error_text, 0).show()
                }
            }
        }
    }

    private fun doRealRestore(uri: Uri): Int {
        Log.d(TAG, "Restore from URI: $uri")
        var failed = false
        try {
            val tempFile = File.createTempFile("restore", ".zip", requireActivity().cacheDir)
            val `is` = requireActivity().contentResolver.openInputStream(uri)
            if (`is` == null) {
                Log.e(TAG, "Could not open input stream for URI")
                return 1
            }
            val fos = FileOutputStream(tempFile)
            val buf = ByteArray(32768)
            var read: Int
            while ((`is`.read(buf).also { read = it }) > 0) {
                fos.write(buf, 0, read)
            }
            fos.close()
            `is`.close()
            val zipFile = ZipFile(tempFile)
            val entries: Enumeration<out ZipEntry> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name == AppDatabase.DATABASE_NAME) {
                    dbOperations.close()
                    if (!receiveFile(zipFile.getInputStream(entry), requireActivity().getDatabasePath(AppDatabase.DATABASE_NAME))) {
                        failed = true
                    }
                    dbOperations.reopen()
                } else if (!receiveFile(zipFile.getInputStream(entry), File(requireActivity().filesDir, entry.name))) {
                    failed = true
                }
            }
            zipFile.close()
            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring", e)
            failed = true
        }
        return if (failed) 2 else 0
    }

    private fun receiveFile(inputStream: InputStream, file: File): Boolean {
        Log.i(TAG, "receiving File from zip: " + file.name)
        try {
            val fileOutputStream = FileOutputStream(file)
            val bArr = ByteArray(32768)
            var read = inputStream.read(bArr)
            while (read > 0) {
                fileOutputStream.write(bArr, 0, read)
                read = inputStream.read(bArr)
            }
            fileOutputStream.close()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving file from zip archive", e)
            return false
        }
    }

    private fun sendFile(file: File, outputStream: OutputStream): Boolean {
        Log.i(TAG, "sending File to zip: " + file.name)
        try {
            val fileInputStream = FileInputStream(file)
            val bArr = ByteArray(32768)
            var read = fileInputStream.read(bArr)
            while (read > 0) {
                outputStream.write(bArr, 0, read)
                read = fileInputStream.read(bArr)
            }
            fileInputStream.close()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file to zip archive", e)
            return false
        }
    }

    companion object {
        private const val TAG = "ZMT_SettingsFragment"
    }
}
