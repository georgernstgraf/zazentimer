package at.priv.graf.zazentimer

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.DbOperations
import at.priv.graf.zazentimer.fragments.MainFragment
import at.priv.graf.zazentimer.service.MeditationService
import at.priv.graf.zazentimer.service.MeditationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ZazenTimerActivity :
    AppCompatActivity(),
    MainFragment.OnFragmentInteractionListener {
    private var meditationEndReceiver: MeditationEndReceiver? = null
    private var pref: SharedPreferences? = null
    private val intentAllowMuting: Intent = Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS")
    private var created: Boolean = false
    private var showPrefsOnStart: Boolean = false
    private var viewModel: MeditationViewModel? = null
    private var appRunning: Boolean = false
    private var handler: Handler? = null
    private var navController: NavController? = null
    private var appBarConfiguration: AppBarConfiguration? = null
    private var zenIndicator: ImageView? = null

    @Inject
    lateinit var dbOperations: DbOperations

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

    override fun onCreate(bundle: Bundle?) {
        Log.d(TAG, "onCreate")
        val preferences = getPreferences(this)
        val theme = preferences.getString(PREF_KEY_THEME, PREF_DEFAULT_THEME)
        if (theme == PREF_VALUE_THEME_DARK) {
            setTheme(R.style.DarkZenTheme)
        } else if (theme == PREF_VALUE_THEME_SYSTEM) {
            val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTheme(R.style.DarkZenTheme)
            } else {
                setTheme(R.style.LightZenTheme)
            }
        } else {
            setTheme(R.style.LightZenTheme)
        }
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
        BellCollection.initialize(this)
        convertFromOldVersions()
        setContentView(R.layout.main)
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        toolbar?.let { setSupportActionBar(it) }
        zenIndicator = findViewById(R.id.zenIndicator)
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
        if (preferences.getBoolean(PREF_KEY_FIRST_START, true)) {
            Log.d(TAG, "This is the first run - create demo sessions")
            lifecycleScope.launch {
                createDemoSessions()
                withContext(Dispatchers.Main) {
                    findMainFragment()?.updateSessionList()
                }
            }
            preferences.edit().putBoolean(PREF_KEY_FIRST_START, false).apply()
        }
        BellCollection.initialize(this)
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
                vm.releaseScreenWakeLock()
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
        if (nc != null) {
            appBarConfiguration?.let { config ->
                return NavigationUI.navigateUp(nc, config)
            }
        }
        return super.onSupportNavigateUp()
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

    fun showSessionEditFragment(sessionId: Int) {
        val nc = getNavController() ?: return
        val args = Bundle()
        args.putInt("sessionId", sessionId)
        nc.navigate(R.id.action_mainFragment_to_sessionEditFragment, args)
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
        val message = "Commit: ${BuildConfig.GIT_HASH}<br><br>" +
            "${getString(R.string.about1)}<br><br>" +
            "${getString(R.string.about2)}<br><br>" +
            "${getString(R.string.about3)}"
        val textView = TextView(this)
        textView.text = Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT)
        textView.movementMethod = LinkMovementMethod.getInstance()
        val pad = (24 * resources.displayMetrics.density).toInt()
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
            attributes.screenBrightness = preferences.getInt(PREF_KEY_BRIGHTNESS, 0) / 100.0f
            window.attributes = attributes
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val vm = viewModel ?: return
        vm.setSelectedSessionId(getSelectedSessionId())
        vm.acquireScreenWakeLock(applicationContext as android.app.Application, preferences)
        vm.startMeditation(applicationContext as android.app.Application, getSelectedSessionId())
        showMeditationScreen()
        vm.startUpdateThread()
        zenIndicator?.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        val id = menuItem.itemId
        if (id == R.id.menu_settings) {
            showSettingsScreen()
            return true
        } else if (id == R.id.menu_privacy) {
            Log.d(TAG, "privacy")
            showPrivacyScreen()
            return true
        } else if (id == R.id.menu_add_session) {
            val mf = findMainFragment()
            mf?.onFabNewSessionClicked()
            return true
        } else if (id == R.id.menu_about) {
            showAboutScreen()
            return true
        } else if (id == R.id.menu_session_edit_help) {
            return super.onOptionsItemSelected(menuItem)
        } else {
            return super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        val nc = getNavController()
        if (nc?.currentDestination?.id == R.id.mainFragment) {
            menuInflater.inflate(R.menu.main_menu, menu)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun findMainFragment(): MainFragment? {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        return navHost?.childFragmentManager?.primaryNavigationFragment as? MainFragment
    }

    private suspend fun createDemoSessions() {
        withContext(Dispatchers.IO) {
            val session = Session()
            session.description = resources.getString(R.string.demo_sess1_description)
            session.name = resources.getString(R.string.demo_sess1_name)
            dbOperations.insertSession(session)
            val section = Section()
            section.bell = -2
            section.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88)?.uri?.toString() ?: return@withContext
            section.bellcount = 1
            section.bellpause = 1
            section.duration = 30
            section.name = resources.getString(R.string.demo_sess1_sec1_name)
            section.rank = 1
            dbOperations.insertSection(session, section)
            val section2 = Section()
            section2.bell = -2
            section2.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107)?.uri?.toString() ?: return@withContext
            section2.bellcount = 2
            section2.bellpause = 3
            section2.duration = 900
            section2.name = resources.getString(R.string.demo_sess1_sec2_name)
            section2.rank = 2
            dbOperations.insertSection(session, section2)
            val section3 = Section()
            section3.bell = -2
            section3.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88)?.uri?.toString() ?: return@withContext
            section3.bellcount = 2
            section3.bellpause = 3
            section3.duration = 300
            section3.name = resources.getString(R.string.demo_sess1_sec3_name)
            section3.rank = 3
            dbOperations.insertSection(session, section3)
            val section4 = Section()
            section4.bell = -2
            section4.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107)?.uri?.toString() ?: return@withContext
            section4.bellcount = 2
            section4.bellpause = 3
            section4.duration = 900
            section4.name = resources.getString(R.string.demo_sess1_sec4_name)
            section4.rank = 4
            dbOperations.insertSection(session, section4)
            val section5 = Section()
            section5.bell = -2
            section5.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88)?.uri?.toString() ?: return@withContext
            section5.bellcount = 2
            section5.bellpause = 3
            section5.duration = 300
            section5.name = resources.getString(R.string.demo_sess1_sec5_name)
            section5.rank = 5
            dbOperations.insertSection(session, section5)
            val section6 = Section()
            section6.bell = -2
            section6.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107)?.uri?.toString() ?: return@withContext
            section6.bellcount = 2
            section6.bellpause = 3
            section6.duration = 900
            section6.name = resources.getString(R.string.demo_sess1_sec6_name)
            section6.rank = 6
            dbOperations.insertSection(session, section6)
            val session2 = Session()
            session2.description = resources.getString(R.string.demo_sess2_description)
            session2.name = resources.getString(R.string.demo_sess2_name)
            dbOperations.insertSession(session2)
            val section7 = Section()
            section7.bell = -2
            section7.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_TIB_RHINBOWL_230)?.uri?.toString() ?: return@withContext
            section7.bellcount = 1
            section7.bellpause = 1
            section7.duration = 5
            section7.name = resources.getString(R.string.demo_sess1_sec1_name)
            section7.rank = 1
            dbOperations.insertSection(session2, section7)
            val section8 = Section()
            section8.bell = -2
            section8.bellUri = BellCollection.getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107)?.uri?.toString() ?: return@withContext
            section8.bellcount = 2
            section8.bellpause = 3
            section8.duration = 600
            section8.name = resources.getString(R.string.demo_sess1_sec2_name)
            section8.rank = 2
            dbOperations.insertSection(session2, section8)
        }
    }

    override fun onStartPressed() {
        lifecycleScope.launch {
            if (dbOperations.readSections(getSelectedSessionId()).isEmpty()) {
                if (getSelectedSessionId() == -1) {
                    Toast.makeText(this@ZazenTimerActivity, R.string.no_session_exists, 0).show()
                    return@launch
                } else {
                    Toast.makeText(this@ZazenTimerActivity, R.string.no_sections_in_session, 0).show()
                    return@launch
                }
            }
            val preferences = this@ZazenTimerActivity.pref ?: return@launch
            if (preferences.getBoolean(PREF_KEY_MUTE_MODE_NONE, true) || preferences.getBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false)) {
                if (Build.VERSION.SDK_INT >= 23) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (!notificationManager.isNotificationPolicyAccessGranted()) {
                        if (isCallable(this@ZazenTimerActivity.intentAllowMuting)) {
                            showMessageAllowMute(this@ZazenTimerActivity.intentAllowMuting)
                            return@launch
                        } else {
                            showMessageNoMuteSettings()
                            return@launch
                        }
                    }
                    startMeditation()
                    return@launch
                }
                startMeditation()
                return@launch
            }
            startMeditation()
        }
    }

    private fun showMessageAllowMute(intent: Intent) {
        AlertDialog
            .Builder(
                this,
            ).setTitle(
                R.string.title_mute_perm_request,
            ).setMessage(R.string.test_mute_perm_request)
            .setIcon(R.drawable.icon)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    this@ZazenTimerActivity.runOnUiThread {
                        try {
                            this@ZazenTimerActivity.startActivity(intent)
                        } catch (unused: Exception) {
                            this@ZazenTimerActivity.showMessageNoMuteSettings()
                        }
                    }
                } catch (unused: Exception) {
                    this@ZazenTimerActivity.runOnUiThread {
                        this@ZazenTimerActivity.showMessageNoMuteSettings()
                    }
                }
            }.setNegativeButton(R.string.abbrechen) { _, _ ->
            }.show()
    }

    fun showMessageNoMuteSettings() {
        AlertDialog
            .Builder(
                this,
            ).setTitle(
                R.string.title_mute_perm_request,
            ).setMessage(R.string.text_no_notify_access_settings)
            .setIcon(R.drawable.icon)
            .setPositiveButton(R.string.ok) { _, _ ->
            }.show()
    }

    private fun isCallable(intent: Intent): Boolean {
        val queryIntentActivities = packageManager.queryIntentActivities(intent, 65536)
        return queryIntentActivities.isNotEmpty()
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

    private fun convertFromOldVersions() {
        this.pref = getPreferences(this)
        val preferences = this.pref ?: return
        if (!preferences.getBoolean(PREF_KEY_CONVERTED_FROM_DB, false)) {
            Log.d(TAG, "marking settings as converted from DB to preferences...")
            preferences
                .edit()
                .putBoolean(PREF_KEY_CONVERTED_FROM_DB, true)
                .apply()
            Log.d(TAG, "done converting settings")
        }
        if (!preferences.getBoolean(PREF_KEY_CONVERTED_BELL_INDICES, false)) {
            Log.d(TAG, "converting Bell Indices to URIs...")
            lifecycleScope.launch { convertBellIndices() }
            Log.d(TAG, "done converting Bell Indices")
        }
        if (preferences.contains(PREF_KEY_PHONE_OFF)) {
            if (preferences.getBoolean(PREF_KEY_PHONE_OFF, true)) {
                preferences
                    .edit()
                    .putBoolean(
                        PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                        false,
                    ).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false)
                    .putBoolean(PREF_KEY_MUTE_MODE_NONE, true)
                    .remove(PREF_KEY_PHONE_OFF)
                    .apply()
            } else {
                preferences
                    .edit()
                    .putBoolean(
                        PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                        true,
                    ).putBoolean(
                        PREF_KEY_MUTE_MODE_VIBRATE,
                        false,
                    ).putBoolean(PREF_KEY_MUTE_MODE_NONE, false)
                    .remove(PREF_KEY_PHONE_OFF)
                    .apply()
            }
        }
    }

    private suspend fun convertBellIndices() {
        for (session in dbOperations.readSessions()) {
            for (section in dbOperations.readSections(session.id)) {
                val bellUri = section.bellUri
                if (bellUri == null || bellUri.trim().isEmpty()) {
                    val bell = BellCollection.getBell(section.bell)
                    if (bell != null) {
                        section.bellUri = bell.uri.toString()
                        section.bell = -2
                        dbOperations.updateSection(section)
                    } else {
                        section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: continue
                        section.bell = -2
                        dbOperations.updateSection(section)
                    }
                } else if (section.bell == -1) {
                    section.bell = -2
                    section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: continue
                    dbOperations.updateSection(section)
                }
            }
        }
    }

    fun resetSettingsForTest() {
        getPreferences(
            this,
        ).edit()
            .putBoolean(
                PREF_KEY_MUTE_MODE_VIBRATE_SOUND,
                false,
            ).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false)
            .putBoolean(PREF_KEY_MUTE_MODE_NONE, true)
            .apply()
    }

    fun resetDatabaseForTest() {
        kotlinx.coroutines.runBlocking {
            withContext(Dispatchers.IO) {
                val readSessions = dbOperations.readSessions()
                for (i in readSessions.indices) {
                    for (section in dbOperations.readSections(readSessions[i].id)) {
                        dbOperations.deleteSection(section.id.toLong())
                    }
                    dbOperations.deleteSession(readSessions[i].id)
                }
                createDemoSessions()
            }
        }
        runOnUiThread {
            val f = this@ZazenTimerActivity.findMainFragment()
            f?.updateSessionList()
        }
    }

    companion object {
        const val INTENT_DATA_SHOW_PREF_ON_START: String = "gotoPrefs"
        const val PREF_DEFAULT_BRIGHTNESS: Int = 0
        const val PREF_DEFAULT_CONVERTED_BELL_INDICES: Boolean = false
        const val PREF_DEFAULT_CONVERTED_FROM_DB: Boolean = false
        const val PREF_DEFAULT_FIRST_START: Boolean = true
        const val PREF_DEFAULT_KEEP_SCREEN_ON: Boolean = false
        const val PREF_DEFAULT_LAST_SESSION: Int = -1
        const val PREF_DEFAULT_MUTE_MODE_NONE: Boolean = true
        const val PREF_DEFAULT_MUTE_MODE_VIBRATE: Boolean = false
        const val PREF_DEFAULT_MUTE_MODE_VIBRATE_SOUND: Boolean = false
        const val PREF_DEFAULT_PHONE_OFF: Boolean = true
        const val PREF_DEFAULT_SHOW_ELAPSED_TIME: Boolean = true
        const val PREF_DEFAULT_SHOW_SESSION_EDIT_HELP_V13: Boolean = false
        const val PREF_DEFAULT_SHOW_TIME_MODE: Int = 0
        const val PREF_DEFAULT_THEME: String = "system"
        const val PREF_VALUE_THEME_DARK: String = "dark"
        const val PREF_VALUE_THEME_LIGHT: String = "light"
        const val PREF_VALUE_THEME_SYSTEM: String = "system"
        const val PREF_DEFAULT_VOLUME: Int = 100
        const val PREF_KEY_BRIGHTNESS: String = "brightness"
        const val PREF_KEY_CONVERTED_BELL_INDICES: String = "bell_indices_converted"
        const val PREF_KEY_CONVERTED_FROM_DB: String = "pref_converted"
        const val PREF_KEY_FIRST_START: String = "first_start"
        const val PREF_KEY_KEEP_SCREEN_ON: String = "keep_screen_on"
        const val PREF_KEY_LAST_SESSION: String = "last_session"
        const val PREF_KEY_MUTE_MODE_NONE: String = "mute_mode_none"
        const val PREF_KEY_MUTE_MODE_VIBRATE: String = "mute_mode_vibrate"
        const val PREF_KEY_MUTE_MODE_VIBRATE_SOUND: String = "mute_mode_vibrate_sound"
        const val PREF_KEY_PHONE_OFF: String = "phone_off"
        const val PREF_KEY_SHOW_ELAPSED_TIME: String = "show_elapsed_time"
        const val PREF_KEY_SHOW_SESSION_EDIT_HELP_V13: String = "session_edit_help_13"
        const val PREF_KEY_SHOW_TIME_MODE: String = "view_time_mode"
        const val PREF_KEY_THEME: String = "theme"
        const val PREF_KEY_VOLUME: String = "volume"

        @JvmStatic
        fun getPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        private const val TAG = "ZMT_ZazenTimerActivity"
    }
}
