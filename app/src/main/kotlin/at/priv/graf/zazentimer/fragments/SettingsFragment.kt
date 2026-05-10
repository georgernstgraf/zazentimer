package at.priv.graf.zazentimer.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.backup.BackupManager
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.DbOperations
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var dbOperations: DbOperations

    private val backupManager: BackupManager by lazy {
        BackupManager(
            databaseFileProvider = { requireActivity().getDatabasePath(AppDatabase.DATABASE_NAME) },
            filesDirProvider = { requireActivity().filesDir },
            onCloseDatabase = { dbOperations.close() },
            onReopenDatabase = { dbOperations.reopen() },
        )
    }

    private val backupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            requireActivity().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            doBackup(uri)
        }

    private val restoreLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            requireActivity().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            doRestore(uri)
        }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialFadeThrough()
    }

    override fun onCreatePreferences(
        bundle: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupThemePreference()
        setupMuteModePreferences()
        setupBrightnessPreference()
        setupBackupPreferences()
    }

    private fun setupThemePreference() {
        findPreference<Preference>(ZazenTimerActivity.PREF_KEY_THEME)?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().runOnUiThread {
                val intent = Intent(requireActivity(), ZazenTimerActivity::class.java)
                intent.putExtra(ZazenTimerActivity.INTENT_DATA_SHOW_PREF_ON_START, true)
                requireActivity().finish()
                requireActivity().startActivity(intent)
            }
            true
        }
    }

    private fun setupMuteModePreferences() {
        val vibSoundPref =
            findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND)
        val vibPref =
            findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE)
        val nonePref =
            findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE)
        if (vibSoundPref == null || vibPref == null || nonePref == null) return
        val checkVibSound = vibSoundPref
        val checkVib = vibPref
        val checkNone = nonePref

        checkVibSound.setOnPreferenceChangeListener { _, obj ->
            if (!(obj as Boolean)) {
                return@setOnPreferenceChangeListener checkVib.isChecked || checkNone.isChecked
            }
            checkVib.isChecked = false
            checkNone.isChecked = false
            true
        }
        checkVib.setOnPreferenceChangeListener { _, obj ->
            if (!(obj as Boolean)) {
                return@setOnPreferenceChangeListener checkVibSound.isChecked || checkNone.isChecked
            }
            checkVibSound.isChecked = false
            checkNone.isChecked = false
            true
        }
        checkNone.setOnPreferenceChangeListener { _, obj ->
            if (!(obj as Boolean)) {
                return@setOnPreferenceChangeListener checkVibSound.isChecked || checkVib.isChecked
            }
            checkVibSound.isChecked = false
            checkVib.isChecked = false
            true
        }
    }

    private fun setupBrightnessPreference() {
        val keepScreenOnPref =
            findPreference<CheckBoxPreference>(ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON)
        val brightnessPref =
            findPreference<Preference>(ZazenTimerActivity.PREF_KEY_BRIGHTNESS)
        val checkKeepScreenOn = keepScreenOnPref ?: return
        val checkBrightness = brightnessPref ?: return

        checkKeepScreenOn.setOnPreferenceChangeListener { _, obj ->
            checkBrightness.isEnabled = obj as Boolean
            true
        }
        checkBrightness.isEnabled = checkKeepScreenOn.isChecked
    }

    private fun setupBackupPreferences() {
        val backupPref =
            findPreference<Preference>("backup_to_sd") ?: return
        backupPref.isEnabled = true
        backupPref.setSummary(R.string.pref_sum_backup)
        backupPref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "application/zip"
                intent.putExtra(Intent.EXTRA_TITLE, "zazentimer_backup.zip")
                backupLauncher.launch(intent)
                true
            }

        val restorePref =
            findPreference<Preference>("restore_from_sd") ?: return
        restorePref.isEnabled = true
        restorePref.setSummary(R.string.pref_sum_restore)
        restorePref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AlertDialog
                    .Builder(requireActivity())
                    .setTitle(R.string.restore_really_title)
                    .setMessage(R.string.restore_really_text)
                    .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.type = "application/zip"
                        restoreLauncher.launch(intent)
                    }.setNegativeButton(R.string.abbrechen) { _: DialogInterface, _: Int ->
                    }.show()
                true
            }
    }

    private fun doBackup(uri: Uri) {
        lifecycleScope.launch {
            val os = requireActivity().contentResolver.openOutputStream(uri)
            val success =
                if (os != null) {
                    backupManager.backup(os)
                } else {
                    Log.e(TAG, "Could not open output stream for URI")
                    dbOperations.reopen()
                    false
                }
            if (success) {
                Toast.makeText(requireActivity(), R.string.backup_success_text, 0).show()
            } else {
                Toast.makeText(requireActivity(), R.string.backup_error_text, 0).show()
            }
        }
    }

    private fun doRestore(uri: Uri) {
        lifecycleScope.launch {
            var result = 2
            try {
                val tempFile = File.createTempFile("restore", ".zip", requireActivity().cacheDir)
                val inputStream = requireActivity().contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open input stream for URI")
                    result = 1
                } else {
                    val fos = FileOutputStream(tempFile)
                    val buf = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while ((inputStream.read(buf).also { read = it }) > 0) {
                        fos.write(buf, 0, read)
                    }
                    fos.close()
                    inputStream.close()
                    result = backupManager.restore(tempFile)
                    tempFile.delete()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error restoring", e)
            }
            if (result == 0) {
                Toast.makeText(requireActivity(), R.string.restore_success_text, 0).show()
            } else if (result == 1) {
                Toast.makeText(requireActivity(), R.string.restore_backup_not_found, 0).show()
            } else if (result == 2) {
                Toast.makeText(requireActivity(), R.string.restore_error_text, 0).show()
            }
        }
    }

    companion object {
        private const val TAG = "ZMT_SettingsFragment"
        private const val BUFFER_SIZE = 32768
    }
}
