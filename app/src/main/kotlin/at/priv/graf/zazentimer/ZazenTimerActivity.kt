package at.priv.graf.zazentimer

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import at.priv.graf.zazentimer.backup.BackupManager
import at.priv.graf.zazentimer.database.AppDatabase
import at.priv.graf.zazentimer.database.BellSanitizer
import at.priv.graf.zazentimer.database.DatabaseOwner
import at.priv.graf.zazentimer.database.DemoSessionCreator
import at.priv.graf.zazentimer.database.SectionRepository
import at.priv.graf.zazentimer.database.SessionRepository
import at.priv.graf.zazentimer.fragments.MainFragment
import at.priv.graf.zazentimer.service.MeditationService
import at.priv.graf.zazentimer.service.MeditationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class ZazenTimerActivity :
    AppCompatActivity(),
    MainFragment.OnFragmentInteractionListener {
    private var meditationEndReceiver: MeditationEndReceiver? = null
    private var pref: SharedPreferences? = null
    private var created: Boolean = false
    private var showPrefsOnStart: Boolean = false
    private var viewModel: MeditationViewModel? = null
    private var appRunning: Boolean = false
    private var handler: Handler? = null
    private var navController: NavController? = null
    private var appBarConfiguration: AppBarConfiguration? = null
    private var zenIndicator: ImageView? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            startMeditation()
        }

    @Inject
    lateinit var sessionRepo: SessionRepository

    @Inject
    lateinit var sectionRepo: SectionRepository

    @Inject
    lateinit var bellSanitizer: BellSanitizer

    @Inject
    lateinit var databaseOwner: DatabaseOwner

    @Inject
    lateinit var demoSessionCreator: DemoSessionCreator

    internal fun forceStopMeditationForTest() {
        viewModel?.stopUpdateThread()
        try {
            viewModel?.stopMeditation()
        } catch (_: Exception) {
        }
        try {
            viewModel?.unbindFromService(applicationContext as android.app.Application)
        } catch (_: Exception) {
        }
        try {
            val intent = viewModel?.getServiceIntent(applicationContext as android.app.Application)
            if (intent != null) {
                stopService(intent)
            }
        } catch (_: Exception) {
        }
        try {
            showMainScreen()
        } catch (_: Exception) {
        }
    }

    private fun getNavController(): NavController? {
        if (navController == null) {
            val navHostFragment =
                supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            navHostFragment?.let { navController = it.navController }
        }
        return navController
    }

    private class MeditationEndReceiver(
        private val activity: ZazenTimerActivity,
    ) : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            this.activity.serviceMeditationEndNotify()
        }
    }

    private fun applyTheme() {
        val preferences = getPreferences(this)
        val theme = preferences.getString(PREF_KEY_THEME, PREF_DEFAULT_THEME)
        if (theme == PREF_VALUE_THEME_DARK) {
            setTheme(R.style.DarkZenTheme)
        } else if (theme == PREF_VALUE_THEME_SYSTEM) {
            val nightMode =
                resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTheme(R.style.DarkZenTheme)
            } else {
                setTheme(R.style.LightZenTheme)
            }
        } else {
            setTheme(R.style.LightZenTheme)
        }
    }

    @Suppress("LongMethod")
    override fun onCreate(bundle: Bundle?) {
        Log.d(TAG, "onCreate")
        val preferences = getPreferences(this)
        applyTheme()
        super.onCreate(bundle)
        enableEdgeToEdge()
        if (bundle == null) {
            this.created = true
        }
        this.meditationEndReceiver = MeditationEndReceiver(this)
        this.showPrefsOnStart = intent.getBooleanExtra(INTENT_DATA_SHOW_PREF_ON_START, false)
        this.handler = Handler(Looper.getMainLooper())
        this.viewModel = ViewModelProvider(this).get(MeditationViewModel::class.java)
        this.viewModel?.setHandler(this.handler ?: Handler(Looper.getMainLooper()))
        this.pref = getPreferences(this)
        this.pref?.let { p ->
            if (p.contains(PREF_KEY_SHOW_ELAPSED_TIME)) {
                val mode = if (p.getBoolean(PREF_KEY_SHOW_ELAPSED_TIME, true)) 0 else 1
                p
                    .edit()
                    .putInt(PREF_KEY_SHOW_TIME_MODE, mode)
                    .remove(PREF_KEY_SHOW_ELAPSED_TIME)
                    .apply()
            }
        }
        setContentView(R.layout.main)
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                v.setPadding(0, statusBarInsets.top, 0, 0)
                insets
            }
        }
        zenIndicator = findViewById(R.id.zen_indicator)
        val nc = getNavController()
        if (nc != null) {
            appBarConfiguration =
                AppBarConfiguration
                    .Builder(
                        R.id.mainFragment,
                    ).build()
            appBarConfiguration?.let { NavigationUI.setupActionBarWithNavController(this, nc, it) }
        }
        observeViewModel()
        lifecycleScope.launch {
            bellSanitizer.sanitizeBellUris()
            val demoMarker = File(noBackupFilesDir, "demo_sessions_created")
            if (demoMarker.exists() && sessionRepo.readSessions().isEmpty()) {
                Log.d(TAG, "Marker exists but DB empty -- data lost, recreating demo sessions")
                demoMarker.delete()
            }
            if (!demoMarker.exists()) {
                Log.d(TAG, "No demo marker -- creating demo sessions")
                demoSessionCreator.createDemoSessions()
                demoMarker.createNewFile()
                withContext(Dispatchers.Main) {
                    findMainFragment()?.updateSessionList()
                }
            }
            if (intent.getStringExtra(INTENT_EXTRA_CREATE_BACKUP) == "true") {
                createBackupAndFinish()
                return@launch
            }
        }
    }

    private fun observeViewModel() {
        val vm = viewModel ?: return
        vm.getMeditationEnded().observe(this) { ended ->
            if (ended) {
                vm.consumeMeditationEnded()
                vm.stopUpdateThread()
                val app = applicationContext as android.app.Application
                vm.unbindFromService(app)
                stopService(vm.getServiceIntent(app))
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val attributes = window.attributes
                attributes.screenBrightness = -1.0f
                window.attributes = attributes
                vm.wakeLockManager.release()
                if (appRunning) {
                    showMainScreen()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        viewModel?.stopUpdateThread()
        meditationEndReceiver?.let { unregisterReceiver(it) }
        this.appRunning = false
    }

    override fun onResume() {
        super.onResume()
        zenIndicator?.visibility = if (MeditationService.isServiceRunning()) View.VISIBLE else View.GONE
        Log.d(TAG, "onResume")
        this.appRunning = true
        ContextCompat.registerReceiver(
            this,
            this.meditationEndReceiver,
            IntentFilter(MeditationService.ZAZENTIMER_SESSION_ENDED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val vm = viewModel
        if (MeditationService.isServiceRunning() && vm != null) {
            Log.d(TAG, "MeditationService currently running")
            vm.bindToService(
                applicationContext as android.app.Application,
                this.handler ?: Handler(Looper.getMainLooper()),
                Runnable {
                    if (vm.isServiceConnected()) {
                        Log.d(TAG, "A Meditation is currently running")
                        runOnUiThread {
                            if (created) {
                                showMeditationScreen()
                            }
                            vm.startUpdateThread()
                        }
                    } else {
                        Log.d(TAG, "No Meditation is currently running")
                        runOnUiThread {
                            showMainScreen()
                        }
                    }
                },
            )
            return
        }
        Log.d(TAG, "No MeditationService is currently running")
        if (this.showPrefsOnStart) {
            showSettingsScreen()
            this.showPrefsOnStart = false
        } else if (isMeditationScreenShown()) {
            showMainScreen()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val nc = getNavController()
        if (nc?.currentDestination?.id == R.id.meditationFragment &&
            MeditationService.isServiceRunning()
        ) {
            return true
        }
        return if (nc != null) {
            appBarConfiguration?.let { config -> NavigationUI.navigateUp(nc, config) }
                ?: super.onSupportNavigateUp()
        } else {
            super.onSupportNavigateUp()
        }
    }

    fun isMeditationScreenShown(): Boolean {
        val nc = getNavController() ?: return false
        return nc.currentDestination?.id == R.id.meditationFragment
    }

    fun showSettingsScreen() {
        getNavController()?.navigate(R.id.action_mainFragment_to_settingsFragment)
    }

    fun showMeditationScreen() {
        val nc = getNavController()
        if (nc?.currentDestination?.id != R.id.meditationFragment) {
            nc?.navigate(R.id.action_mainFragment_to_meditationFragment)
        }
    }

    fun showMainScreen() {
        getNavController()?.popBackStack(R.id.mainFragment, false)
    }

    fun showPrivacyScreen() {
        AlertDialog
            .Builder(
                this,
            ).setTitle(
                R.string.privacy_title,
            ).setMessage(R.string.privacy_message)
            .setPositiveButton(R.string.privacy_ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.create()
            .show()
    }

    fun showAboutScreen() {
        val dbVersion = databaseOwner.getActualDatabaseVersion()
        val message =
            "Version: ${BuildConfig.VERSION_DISPLAY}<br>" +
                "Details: ${BuildConfig.BUILD_TYPE}@${BuildConfig.BUILD_HOST}<br>" +
                "Commit: ${BuildConfig.GIT_HASH}<br>" +
                "Room DB: v$dbVersion<br><br>" +
                "${getString(R.string.about1)}<br><br>" +
                "${getString(R.string.about2)}<br><br>" +
                "${getString(R.string.about3)}"
        val textView = TextView(this)
        textView.text = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT)
        textView.movementMethod = LinkMovementMethod.getInstance()
        val pad = (ABOUT_PADDING_DP * resources.displayMetrics.density).toInt()
        textView.setPadding(pad, pad, 0, 0)
        AlertDialog
            .Builder(this)
            .setTitle(R.string.caption_zazen_meditation)
            .setView(textView)
            .setPositiveButton(R.string.privacy_ok) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    fun startMeditation() {
        doStartMediation()
    }

    fun doStartMediation() {
        val preferences = this.pref ?: return
        val keepScreenOn = preferences.getBoolean(PREF_KEY_KEEP_SCREEN_ON, false)
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val attributes = window.attributes
            attributes.screenBrightness = preferences.getInt(PREF_KEY_BRIGHTNESS, 0) / BRIGHTNESS_DIVISOR
            window.attributes = attributes
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val vm = viewModel ?: return
        vm.setSelectedSessionId(getSelectedSessionId())
        vm.wakeLockManager.acquire(preferences, vm.getSelectedSessionId())
        vm.startMeditation(applicationContext as android.app.Application, getSelectedSessionId())
        showMeditationScreen()
        vm.startUpdateThread()
        zenIndicator?.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.menu_settings -> {
                showSettingsScreen()
                true
            }
            R.id.menu_privacy -> {
                Log.d(TAG, "privacy")
                showPrivacyScreen()
                true
            }
            R.id.menu_add_session -> {
                findMainFragment()?.onFabNewSessionClicked()
                true
            }
            R.id.menu_about -> {
                showAboutScreen()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val nc = getNavController()
        if (nc?.currentDestination?.id == R.id.mainFragment) {
            menu.clear()
            menuInflater.inflate(R.menu.main_menu, menu)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun findMainFragment(): MainFragment? {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        return navHost?.childFragmentManager?.primaryNavigationFragment as? MainFragment
    }

    override fun onStartPressed() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        lifecycleScope.launch {
            if (sectionRepo.readSections(getSelectedSessionId()).isEmpty()) {
                if (getSelectedSessionId() == -1) {
                    Toast.makeText(this@ZazenTimerActivity, R.string.no_session_exists, Toast.LENGTH_SHORT).show()
                    return@launch
                } else {
                    Toast.makeText(this@ZazenTimerActivity, R.string.no_sections_in_session, Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
            startMeditation()
        }
    }

    fun serviceMeditationEndNotify() {
        zenIndicator?.visibility = View.GONE
        viewModel?.notifyMeditationEnded()
    }

    fun getSelectedSessionId(): Int {
        val f = findMainFragment()
        if (f != null) {
            return f.getSelectedSessionId()
        }
        return viewModel?.getSelectedSessionId() ?: -1
    }

    fun resetDatabaseForTest() {
        kotlinx.coroutines.runBlocking {
            withContext(Dispatchers.IO) {
                val readSessions = sessionRepo.readSessions()
                for (i in readSessions.indices) {
                    for (section in sectionRepo.readSections(readSessions[i].id)) {
                        sectionRepo.deleteSection(section.id.toLong())
                    }
                    sessionRepo.deleteSession(readSessions[i].id)
                }
                bellSanitizer.sanitizeBellUris()
                demoSessionCreator.createDemoSessions()
            }
        }
        File(noBackupFilesDir, "demo_sessions_created").createNewFile()
        val f = this@ZazenTimerActivity.findMainFragment()
        kotlinx.coroutines.runBlocking {
            withContext(Dispatchers.Main) {
                f?.suspendUpdateSessionList()
            }
        }
    }

    private fun createBackupAndFinish() {
        Log.d(TAG, "create_backup intent received — writing backup ZIP")
        val dbFile = getDatabasePath(AppDatabase.DATABASE_NAME)
        if (!dbFile.exists()) {
            Log.e(TAG, "Database file does not exist: ${dbFile.absolutePath}")
            finish()
            return
        }
        Log.d(TAG, "Database file: ${dbFile.absolutePath} (${dbFile.length()} bytes)")
        val zipFile = File(cacheDir, BACKUP_ZIP_NAME)
        val ok =
            BackupManager(
                databaseFileProvider = { dbFile },
                filesDirProvider = { filesDir },
                onCloseDatabase = { databaseOwner.close() },
                onReopenDatabase = { databaseOwner.reopen() },
            ).backup(FileOutputStream(zipFile))
        if (!ok) {
            Log.e(TAG, "BackupManager.backup() returned false")
        }
        Log.d(TAG, "Backup written to ${zipFile.absolutePath} (${zipFile.length()} bytes, success=$ok)")
        finish()
    }

    companion object {
        const val INTENT_DATA_SHOW_PREF_ON_START: String = "gotoPrefs"
        const val INTENT_EXTRA_CREATE_BACKUP: String = "create_backup"
        const val BACKUP_ZIP_NAME: String = "zazentimer_backup.zip"
        const val PREF_DEFAULT_KEEP_SCREEN_ON: Boolean = false
        const val PREF_DEFAULT_THEME: String = "system"
        const val PREF_VALUE_THEME_DARK: String = "dark"
        const val PREF_VALUE_THEME_LIGHT: String = "light"
        const val PREF_VALUE_THEME_SYSTEM: String = "system"
        const val PREF_KEY_BRIGHTNESS: String = "brightness"
        const val PREF_KEY_KEEP_SCREEN_ON: String = "keep_screen_on"
        const val PREF_KEY_LAST_SESSION: String = "last_session"
        const val PREF_KEY_SHOW_ELAPSED_TIME: String = "show_elapsed_time"
        const val PREF_KEY_SHOW_SESSION_EDIT_HELP_V13: String = "session_edit_help_13"
        const val PREF_KEY_SHOW_TIME_MODE: String = "view_time_mode"
        const val PREF_KEY_THEME: String = "theme"

        @JvmStatic
        fun getPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        private const val TAG = "ZMT_ZazenTimerActivity"
        private const val ABOUT_PADDING_DP = 24
        private const val BRIGHTNESS_DIVISOR = 100.0f
    }
}
