package de.gaffga.android.zazentimer;

import android.app.ActivityManager;
import android.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import de.gaffga.android.base.Settings;
import de.gaffga.android.fragments.AboutFragment;
import de.gaffga.android.fragments.MainFragment;
import de.gaffga.android.fragments.MeditationFragment;
import de.gaffga.android.fragments.SessionEditFragment;
import de.gaffga.android.fragments.SettingsFragment;
import de.gaffga.android.zazentimer.audio.BellCollection;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.android.zazentimer.service.MeditationService;
import de.gaffga.android.zazentimer.service.ServCon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZazenTimerActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener, MeditationFragment.OnFragmentInteractionListener {
    public static final String FRAGMENT_ABOUT = "about";
    public static final String FRAGMENT_MAIN = "main";
    public static final String FRAGMENT_MEDITATION = "meditation";
    public static final String FRAGMENT_SECTION_EDIT = "fragment_edit_section";
    public static final String FRAGMENT_SESSION_EDIT = "session";
    public static final String FRAGMENT_SETTINGS = "settings";
    public static final String INTENT_DATA_SHOW_PREF_ON_START = "gotoPrefs";
    public static final int PREF_DEFAULT_BRIGHTNESS = 0;
    public static final boolean PREF_DEFAULT_CONVERTED_BELL_INDICES = false;
    public static final boolean PREF_DEFAULT_CONVERTED_FROM_DB = false;
    public static final boolean PREF_DEFAULT_FIRST_START = true;
    public static final boolean PREF_DEFAULT_KEEP_SCREEN_ON = false;
    public static final int PREF_DEFAULT_LAST_SESSION = -1;
    public static final boolean PREF_DEFAULT_MUTE_ALARM = true;
    public static final boolean PREF_DEFAULT_MUTE_MODE_NONE = true;
    public static final boolean PREF_DEFAULT_MUTE_MODE_VIBRATE = false;
    public static final boolean PREF_DEFAULT_MUTE_MODE_VIBRATE_SOUND = false;
    public static final boolean PREF_DEFAULT_MUTE_MUSIC = false;
    public static final boolean PREF_DEFAULT_OUTPUT_CHANNEL_ALARM = true;
    public static final boolean PREF_DEFAULT_OUTPUT_CHANNEL_MUSIC = false;
    public static final boolean PREF_DEFAULT_PHONE_OFF = true;
    public static final boolean PREF_DEFAULT_SHOW_ELAPSED_TIME = true;
    public static final boolean PREF_DEFAULT_SHOW_SESSION_EDIT_HELP_V13 = false;
    public static final int PREF_DEFAULT_SHOW_TIME_MODE = 0;
    public static final String PREF_DEFAULT_THEME = "light";
    public static final int PREF_DEFAULT_VOLUME = 100;
    public static final String PREF_KEY_BRIGHTNESS = "brightness";
    public static final String PREF_KEY_CONVERTED_BELL_INDICES = "bell_indices_converted";
    public static final String PREF_KEY_CONVERTED_FROM_DB = "pref_converted";
    public static final String PREF_KEY_FIRST_START = "first_start";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String PREF_KEY_LAST_SESSION = "last_session";
    public static final String PREF_KEY_MUTE_ALARM = "mute_alarm";
    public static final String PREF_KEY_MUTE_MODE_NONE = "mute_mode_none";
    public static final String PREF_KEY_MUTE_MODE_VIBRATE = "mute_mode_vibrate";
    public static final String PREF_KEY_MUTE_MODE_VIBRATE_SOUND = "mute_mode_vibrate_sound";
    public static final String PREF_KEY_MUTE_MUSIC = "mute_music";
    public static final String PREF_KEY_OUTPUT_CHANNEL_ALARM = "pref_output_channel_alarm";
    public static final String PREF_KEY_OUTPUT_CHANNEL_MUSIC = "pref_output_channel_music";
    public static final String PREF_KEY_PHONE_OFF = "phone_off";
    public static final String PREF_KEY_SHOW_ELAPSED_TIME = "show_elapsed_time";
    public static final String PREF_KEY_SHOW_SESSION_EDIT_HELP_V13 = "session_edit_help_13";
    public static final String PREF_KEY_SHOW_TIME_MODE = "view_time_mode";
    public static final String PREF_KEY_THEME = "theme";
    public static final String PREF_KEY_VOLUME = "volume";
    public static final String PREF_VALUE_THEME_DARK = "dark";
    public static final String PREF_VALUE_THEME_LIGHT = "light";
    private static final String TAG = "ZMT_ZazenTimerActivity";
    private AboutFragment aboutFragment;
    private Handler handler;
    private MainFragment mainFragment;
    private MeditationEndReceiver meditationEndReceiver;
    private MeditationFragment meditationFragment;
    private MessageView messageView;
    private SharedPreferences pref;
    private ArrayList<ServerMessage> serverMessages;
    private Intent serviceIntent;
    private SessionEditFragment sessionEditFragment;
    private SettingsFragment settingsFragment;
    final Intent intentAllowMuting = new Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS");
    private boolean created = false;
    private boolean showPrefsOnStart = false;
    private ServCon serviceConnection = null;
    private MeditationUIUpdateThread updateThread = null;
    private PowerManager.WakeLock wakeLock = null;
    private boolean appRunning = false;

    private static class MeditationEndReceiver extends BroadcastReceiver {
        private final ZazenTimerActivity activity;

        MeditationEndReceiver(ZazenTimerActivity zazenTimerActivity) {
            this.activity = zazenTimerActivity;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            this.activity.serviceMeditationEndNotify();
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.BaseFragmentActivityGingerbread, android.app.Activity
    protected void onCreate(Bundle bundle) {
        Log.d(TAG, "onCreate");
        SharedPreferences preferences = getPreferences(this);
        if (preferences.getString(PREF_KEY_THEME, "light").equals("dark")) {
            setTheme(R.style.DarkZenTheme);
        } else {
            setTheme(R.style.LightZenTheme);
        }
        super.onCreate(bundle);
        if (bundle == null) {
            this.created = true;
        }
        this.meditationEndReceiver = new MeditationEndReceiver(this);
        this.showPrefsOnStart = getIntent().getBooleanExtra(INTENT_DATA_SHOW_PREF_ON_START, false);
        this.handler = new Handler(Looper.getMainLooper());
        DbOperations.init(this);
        BellCollection.getInstance().init(this);
        convertFromOldVersions();
        initView();
        this.serviceIntent = new Intent(this, (Class<?>) MeditationService.class);
        if (preferences.getBoolean(PREF_KEY_FIRST_START, true)) {
            Log.d(TAG, "This is the first run - create demo sessions");
            createDemoSessions();
            preferences.edit().putBoolean(PREF_KEY_FIRST_START, false).apply();
        }
        DbOperations.init(this);
        BellCollection.getInstance().init(this);
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        stopUpdateThread();
        unregisterReceiver(this.meditationEndReceiver);
        this.appRunning = false;
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        this.appRunning = true;
        ContextCompat.registerReceiver(this, this.meditationEndReceiver, new IntentFilter(MeditationService.ZAZENTIMER_SESSION_ENDED), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (isMyServiceRunning(MeditationService.class)) {
            Log.d(TAG, "MeditationService currently running");
            bindToService(this.handler, new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.1
                @Override // java.lang.Runnable
                public void run() {
                    if (ZazenTimerActivity.this.serviceConnection.getRunningMeditation() != null) {
                        Log.d(ZazenTimerActivity.TAG, "A Meditation is currently running");
                        ZazenTimerActivity.this.runOnUiThread(new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.1.1
                            @Override // java.lang.Runnable
                            public void run() {
                                if (ZazenTimerActivity.this.created) {
                                    ZazenTimerActivity.this.showMeditationScreen();
                                }
                                ZazenTimerActivity.this.startUpdateThread();
                            }
                        });
                    } else {
                        Log.d(ZazenTimerActivity.TAG, "No Meditation is currently running");
                        ZazenTimerActivity.this.runOnUiThread(new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.1.2
                            @Override // java.lang.Runnable
                            public void run() {
                                ZazenTimerActivity.this.showMainScreen();
                            }
                        });
                    }
                }
            });
            return;
        }
        Log.d(TAG, "No MeditationService is currently running");
        if (this.showPrefsOnStart) {
            showSettingsScreen();
            this.showPrefsOnStart = false;
        } else if (isMeditationScreenShown()) {
            showMainScreen();
        }
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
    }

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    protected void initView() {
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        this.mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAIN);
        this.meditationFragment = (MeditationFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_MEDITATION);
        this.settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_SETTINGS);
        this.aboutFragment = (AboutFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_ABOUT);
        this.sessionEditFragment = (SessionEditFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_SESSION_EDIT);
        if (this.mainFragment == null) {
            this.mainFragment = new MainFragment();
        }
        if (this.meditationFragment == null) {
            this.meditationFragment = new MeditationFragment();
        }
        if (this.settingsFragment == null) {
            this.settingsFragment = new SettingsFragment();
        }
        if (this.aboutFragment == null) {
            this.aboutFragment = new AboutFragment();
        }
        if (this.sessionEditFragment == null) {
            this.sessionEditFragment = new SessionEditFragment();
        }
        if (this.created) {
            showMainScreen();
        }
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public boolean isMeditationScreenShown() {
        return getSupportFragmentManager().findFragmentByTag(FRAGMENT_MEDITATION) != null;
    }

    public void showAboutScreen() {
        FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
        beginTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in, R.animator.fade_out);
        beginTransaction.replace(R.id.content, this.aboutFragment, FRAGMENT_ABOUT);
        beginTransaction.addToBackStack(null);
        beginTransaction.commit();
    }

    public void showSettingsScreen() {
        FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
        beginTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in, R.animator.fade_out);
        beginTransaction.replace(R.id.content, this.settingsFragment, FRAGMENT_SETTINGS);
        beginTransaction.addToBackStack(null);
        beginTransaction.commit();
    }

    public void showMeditationScreen() {
        FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
        beginTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in, R.animator.fade_out);
        beginTransaction.replace(R.id.content, this.meditationFragment, FRAGMENT_MEDITATION);
        beginTransaction.commit();
    }

    public void showMainScreen() {
        FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
        beginTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in, R.animator.fade_out);
        beginTransaction.replace(R.id.content, this.mainFragment, FRAGMENT_MAIN);
        beginTransaction.commit();
    }

    public void showSessionEditFragment() {
        this.sessionEditFragment.setSessionId(getSelectedSessionId());
        FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
        beginTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in, R.animator.fade_out);
        beginTransaction.replace(R.id.content, this.sessionEditFragment, FRAGMENT_SESSION_EDIT);
        beginTransaction.addToBackStack(null);
        beginTransaction.commit();
    }

    public void showPrivacyScreen() {
        new AlertDialog.Builder(this).setTitle(R.string.privacy_title).setMessage(R.string.privacy_message).setPositiveButton(R.string.privacy_ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).create().show();
    }

    public void startMeditation() {
        doStartMediation();
    }

    public void doStartMediation() {
        boolean z = this.pref.getBoolean(PREF_KEY_KEEP_SCREEN_ON, false);
        if (z) {
            getWindow().addFlags(128);
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.screenBrightness = this.pref.getInt(PREF_KEY_BRIGHTNESS, 0) / 100.0f;
            getWindow().setAttributes(attributes);
        } else {
            getWindow().clearFlags(128);
        }
        PowerManager powerManager = (PowerManager) getSystemService("power");
        if (powerManager != null) {
            int i = 0;
            for (Section section : DbOperations.readSections(getSelectedSessionId())) {
                i += section.duration;
            }
            this.wakeLock = null;
            if (z) {
                this.wakeLock = powerManager.newWakeLock(1, "ScreenOnWakeLock");
                int i2 = i + 60;
                this.wakeLock.acquire(i2 * 1000);
                Log.i(TAG, "Aquired WakeLock to keep screen on for " + i2 + " seconds");
            }
        }
        startService(this.serviceIntent);
        bindToService(this.handler, new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.6
            @Override // java.lang.Runnable
            public void run() {
                ZazenTimerActivity.this.showMeditationScreen();
                ZazenTimerActivity.this.startUpdateThread();
                ZazenTimerActivity.this.serviceConnection.startMeditation(ZazenTimerActivity.this.getSelectedSessionId());
            }
        });
    }

    public void startUpdateThread() {
        if (this.handler == null) {
            this.handler = new Handler(Looper.getMainLooper());
        }
        this.updateThread = new MeditationUIUpdateThread(this.handler, this.meditationFragment, this.serviceConnection.getBinder());
        this.handler.postDelayed(this.updateThread, 300L);
    }

    public void stopUpdateThread() {
        if (this.updateThread != null) {
            this.updateThread.stopUpdates();
            this.updateThread = null;
        }
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_about /* 2131296358 */:
                Log.d(TAG, FRAGMENT_ABOUT);
                showAboutScreen();
                return true;
            case R.id.menu_copy_session /* 2131296359 */:
                Log.d(TAG, "duplicate session");
                int selectedSessionId = this.mainFragment.getSelectedSessionId();
                int duplicateSession = DbOperations.duplicateSession(selectedSessionId, getString(R.string.copy_prefix) + " " + DbOperations.readSession(selectedSessionId).name);
                this.mainFragment.updateSessionList();
                this.mainFragment.setSelectedSessionId(duplicateSession);
                return true;
            case R.id.menu_delete_session /* 2131296360 */:
                Log.d(TAG, "delete session");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_question_delete_session);
                builder.setMessage(R.string.text_question_delete_session);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.7
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DbOperations.deleteSession(ZazenTimerActivity.this.getSelectedSessionId());
                        ZazenTimerActivity.this.mainFragment.updateSessionList();
                        ZazenTimerActivity.this.mainFragment.selectLastSession();
                    }
                });
                builder.setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.8
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                builder.create().show();
                return true;
            case R.id.menu_edit_session /* 2131296361 */:
                Log.d(TAG, "edit session");
                showSessionEditFragment();
                return true;
            case R.id.menu_new_session /* 2131296362 */:
                Log.d(TAG, "new session");
                Session session = new Session();
                session.name = "";
                session.description = "";
                DbOperations.insertSession(session);
                this.sessionEditFragment.setSessionId(session.id);
                this.mainFragment.updateSessionList();
                this.mainFragment.setSelectedSessionId(session.id);
                showSessionEditFragment();
                return true;
            case R.id.menu_privacy /* 2131296363 */:
                Log.d(TAG, "privacy");
                showPrivacyScreen();
                return true;
            case R.id.menu_session_edit_help /* 2131296364 */:
            default:
                return super.onOptionsItemSelected(menuItem);
            case R.id.menu_settings /* 2131296365 */:
                Log.d(TAG, FRAGMENT_SETTINGS);
                showSettingsScreen();
                return true;
        }
    }

    @Override // android.app.Activity
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        Fragment findFragmentByTag = getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAIN);
        if (findFragmentByTag != null && findFragmentByTag.isVisible()) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        }
        MenuItem findItem = menu.findItem(R.id.menu_copy_session);
        if (findItem != null) {
            findItem.setEnabled(getSelectedSessionId() != -1);
        }
        MenuItem findItem2 = menu.findItem(R.id.menu_delete_session);
        if (findItem2 != null) {
            findItem2.setEnabled(getSelectedSessionId() != -1);
        }
        MenuItem findItem3 = menu.findItem(R.id.menu_edit_session);
        if (findItem3 != null) {
            findItem3.setEnabled(getSelectedSessionId() != -1);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void createDemoSessions() {
        Session session = new Session();
        session.description = getResources().getString(R.string.demo_sess1_description);
        session.name = getResources().getString(R.string.demo_sess1_name);
        DbOperations.insertSession(session);
        Section section = new Section();
        section.bell = -2;
        section.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88).getUri().toString();
        section.bellcount = 1;
        section.bellpause = 1;
        section.duration = 30;
        section.name = getResources().getString(R.string.demo_sess1_sec1_name);
        section.rank = 1;
        DbOperations.insertSection(session, section);
        Section section2 = new Section();
        section2.bell = -2;
        section2.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section2.bellcount = 2;
        section2.bellpause = 3;
        section2.duration = 900;
        section2.name = getResources().getString(R.string.demo_sess1_sec2_name);
        section2.rank = 2;
        DbOperations.insertSection(session, section2);
        Section section3 = new Section();
        section3.bell = -2;
        section3.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88).getUri().toString();
        section3.bellcount = 2;
        section3.bellpause = 3;
        section3.duration = 300;
        section3.name = getResources().getString(R.string.demo_sess1_sec3_name);
        section3.rank = 3;
        DbOperations.insertSection(session, section3);
        Section section4 = new Section();
        section4.bell = -2;
        section4.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section4.bellcount = 2;
        section4.bellpause = 3;
        section4.duration = 900;
        section4.name = getResources().getString(R.string.demo_sess1_sec4_name);
        section4.rank = 4;
        DbOperations.insertSection(session, section4);
        Section section5 = new Section();
        section5.bell = -2;
        section5.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88).getUri().toString();
        section5.bellcount = 2;
        section5.bellpause = 3;
        section5.duration = 300;
        section5.name = getResources().getString(R.string.demo_sess1_sec5_name);
        section5.rank = 5;
        DbOperations.insertSection(session, section5);
        Section section6 = new Section();
        section6.bell = -2;
        section6.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section6.bellcount = 2;
        section6.bellpause = 3;
        section6.duration = 900;
        section6.name = getResources().getString(R.string.demo_sess1_sec6_name);
        section6.rank = 6;
        DbOperations.insertSection(session, section6);
        Session session2 = new Session();
        session2.description = getResources().getString(R.string.demo_sess2_description);
        session2.name = getResources().getString(R.string.demo_sess2_name);
        DbOperations.insertSession(session2);
        Section section7 = new Section();
        section7.bell = -2;
        section7.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_TIB_RHINBOWL_230).getUri().toString();
        section7.bellcount = 1;
        section7.bellpause = 1;
        section7.duration = 5;
        section7.name = getResources().getString(R.string.demo_sess1_sec1_name);
        section7.rank = 1;
        DbOperations.insertSection(session2, section7);
        Section section8 = new Section();
        section8.bell = -2;
        section8.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section8.bellcount = 2;
        section8.bellpause = 3;
        section8.duration = 600;
        section8.name = getResources().getString(R.string.demo_sess1_sec2_name);
        section8.rank = 2;
        DbOperations.insertSection(session2, section8);
    }

    @Override // de.gaffga.android.fragments.MainFragment.OnFragmentInteractionListener
    public void onStartPressed() {
        if (DbOperations.readSections(getSelectedSessionId()).length == 0) {
            if (getSelectedSessionId() == -1) {
                Toast.makeText(this, R.string.no_session_exists, 0).show();
                return;
            } else {
                Toast.makeText(this, R.string.no_sections_in_session, 0).show();
                return;
            }
        }
        if (this.pref.getBoolean(PREF_KEY_MUTE_MODE_NONE, true) || this.pref.getBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false)) {
            if (Build.VERSION.SDK_INT >= 23) {
                NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
                if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted()) {
                    if (isCallable(this.intentAllowMuting)) {
                        showMessageAllowMute(this.intentAllowMuting);
                        return;
                    } else {
                        showMessageNoMuteSettings();
                        return;
                    }
                }
                startMeditation();
                return;
            }
            startMeditation();
            return;
        }
        startMeditation();
    }

    private void showMessageAllowMute(final Intent intent) {
        new AlertDialog.Builder(this).setTitle(R.string.title_mute_perm_request).setMessage(R.string.test_mute_perm_request).setIcon(R.drawable.icon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.10
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    ZazenTimerActivity.this.runOnUiThread(new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.10.1
                        @Override // java.lang.Runnable
                        public void run() {
                            try {
                                ZazenTimerActivity.this.startActivity(intent);
                            } catch (Exception unused) {
                                ZazenTimerActivity.this.showMessageNoMuteSettings();
                            }
                        }
                    });
                } catch (Exception unused) {
                    ZazenTimerActivity.this.runOnUiThread(new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.10.2
                        @Override // java.lang.Runnable
                        public void run() {
                            ZazenTimerActivity.this.showMessageNoMuteSettings();
                        }
                    });
                }
            }
        }).setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.9
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    public void showMessageNoMuteSettings() {
        new AlertDialog.Builder(this).setTitle(R.string.title_mute_perm_request).setMessage(R.string.text_no_notify_access_settings).setIcon(R.drawable.icon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.11
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> queryIntentActivities = getPackageManager().queryIntentActivities(intent, 65536);
        return queryIntentActivities != null && queryIntentActivities.size() > 0;
    }

    public void serviceMeditationEndNotify() {
        runOnUiThread(new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.12
            @Override // java.lang.Runnable
            public void run() {
                ZazenTimerActivity.this.getWindow().clearFlags(128);
                WindowManager.LayoutParams attributes = ZazenTimerActivity.this.getWindow().getAttributes();
                attributes.screenBrightness = -1.0f;
                ZazenTimerActivity.this.getWindow().setAttributes(attributes);
                ZazenTimerActivity.this.stopUpdateThread();
                ZazenTimerActivity.this.unbindFromService();
                ZazenTimerActivity.this.stopService(ZazenTimerActivity.this.serviceIntent);
                if (ZazenTimerActivity.this.appRunning) {
                    ZazenTimerActivity.this.showMainScreen();
                }
                if (ZazenTimerActivity.this.wakeLock != null) {
                    try {
                        if (ZazenTimerActivity.this.wakeLock.isHeld()) {
                            ZazenTimerActivity.this.wakeLock.release();
                        }
                    } catch (Exception e) {
                        Log.d(ZazenTimerActivity.TAG, "wakeLock release error", e);
                    }
                    ZazenTimerActivity.this.wakeLock = null;
                    Log.i(ZazenTimerActivity.TAG, "ScreenOn-WakeLock released");
                }
            }
        });
    }

    @Override // de.gaffga.android.fragments.MeditationFragment.OnFragmentInteractionListener
    public void onPauseClicked() {
        if (this.serviceConnection != null) {
            this.serviceConnection.pauseMeditation();
        }
    }

    @Override // de.gaffga.android.fragments.MeditationFragment.OnFragmentInteractionListener
    public void onStopClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.really_stop);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.13
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (ZazenTimerActivity.this.serviceConnection != null) {
                    ZazenTimerActivity.this.serviceConnection.stopMeditation();
                }
            }
        });
        builder.setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.14
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.create().show();
    }

    @Override // de.gaffga.android.fragments.MeditationFragment.OnFragmentInteractionListener
    public boolean isPaused() {
        if (this.serviceConnection == null || this.serviceConnection.getRunningMeditation() == null) {
            return false;
        }
        return this.serviceConnection.getRunningMeditation().isPaused();
    }

    public int getSelectedSessionId() {
        return this.mainFragment.getSelectedSessionId();
    }

    private void bindToService(Handler handler, Runnable runnable) {
        if (this.serviceConnection == null) {
            Log.d(TAG, "serviceConnection is null - making fresh connection service");
            this.serviceConnection = new ServCon(this);
            this.serviceConnection.setRunOnConnect(new RunOnConnect(handler, runnable));
            bindService(this.serviceIntent, this.serviceConnection, 72);
            return;
        }
        if (this.serviceConnection.isBound()) {
            Log.d(TAG, "service is already bound");
            handler.post(runnable);
        } else {
            Log.d(TAG, "service comm existing, but service not bound - rebinding");
            bindService(this.serviceIntent, this.serviceConnection, 72);
        }
    }

    public void unbindFromService() {
        if (this.serviceConnection != null && this.serviceConnection.isBound()) {
            try {
                unbindService(this.serviceConnection);
            } catch (Exception unused) {
            }
        }
        this.serviceConnection = null;
    }

    private boolean isMyServiceRunning(Class<?> cls) {
        Iterator<ActivityManager.RunningServiceInfo> it = ((ActivityManager) getSystemService("activity")).getRunningServices(Integer.MAX_VALUE).iterator();
        while (it.hasNext()) {
            if (cls.getName().equals(it.next().service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void convertFromOldVersions() {
        this.pref = getPreferences(this);
        if (!this.pref.getBoolean(PREF_KEY_CONVERTED_FROM_DB, false)) {
            Log.d(TAG, "converting old settings from DB to preferences...");
            Settings.init(this);
            convertSettings();
            Settings.close();
            Log.d(TAG, "done converting settings");
        }
        if (!this.pref.getBoolean(PREF_KEY_CONVERTED_BELL_INDICES, false)) {
            Log.d(TAG, "converting Bell Indices to URIs...");
            convertBellIndices();
            Log.d(TAG, "done converting Bell Indices");
        }
        if (this.pref.contains(PREF_KEY_PHONE_OFF)) {
            if (this.pref.getBoolean(PREF_KEY_PHONE_OFF, true)) {
                this.pref.edit().putBoolean(PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false).putBoolean(PREF_KEY_MUTE_MODE_NONE, true).putBoolean(PREF_KEY_MUTE_ALARM, true).putBoolean(PREF_KEY_MUTE_MUSIC, false).putBoolean(PREF_KEY_OUTPUT_CHANNEL_ALARM, true).putBoolean(PREF_KEY_OUTPUT_CHANNEL_MUSIC, false).remove(PREF_KEY_PHONE_OFF).apply();
            } else {
                this.pref.edit().putBoolean(PREF_KEY_MUTE_MODE_VIBRATE_SOUND, true).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false).putBoolean(PREF_KEY_MUTE_MODE_NONE, false).putBoolean(PREF_KEY_MUTE_ALARM, false).putBoolean(PREF_KEY_MUTE_MUSIC, false).putBoolean(PREF_KEY_OUTPUT_CHANNEL_ALARM, true).putBoolean(PREF_KEY_OUTPUT_CHANNEL_MUSIC, false).remove(PREF_KEY_PHONE_OFF).apply();
            }
        }
    }

    private void convertBellIndices() {
        for (Session session : DbOperations.readSessions()) {
            for (Section section : DbOperations.readSections(session.id)) {
                if (section.bellUri == null || section.bellUri.trim().length() == 0) {
                    Bell bell = BellCollection.getInstance().getBell(section.bell);
                    if (bell != null) {
                        section.bellUri = bell.getUri().toString();
                        section.bell = -2;
                        DbOperations.updateSection(section);
                    } else {
                        section.bellUri = BellCollection.getInstance().getDemoBell().getUri().toString();
                        section.bell = -2;
                        DbOperations.updateSection(section);
                    }
                } else if (section.bell == -1) {
                    section.bell = -2;
                    section.bellUri = BellCollection.getInstance().getDemoBell().getUri().toString();
                    DbOperations.updateSection(section);
                }
            }
        }
    }

    private void convertSettings() {
        boolean booleanValue = Settings.getBooleanValue(Settings.PARAM_B_PHONE_OFF_DURING_MEDITATION, true);
        int intValue = Settings.getIntValue(Settings.PARAM_I_LAST_SELECTED_SESSION, -1);
        int intValue2 = Settings.getIntValue(Settings.PARAM_I_BELL_VOLUME, 20);
        boolean booleanValue2 = Settings.getBooleanValue(Settings.PARAM_B_KEEP_SCREEN_ON, false);
        this.pref.edit().putBoolean(PREF_KEY_PHONE_OFF, booleanValue).putInt(PREF_KEY_LAST_SESSION, intValue).putInt(PREF_KEY_VOLUME, intValue2).putBoolean(PREF_KEY_KEEP_SCREEN_ON, booleanValue2).putBoolean(PREF_KEY_FIRST_START, Settings.getBooleanValue(Settings.PARAM_B_FIRST_START, true)).putBoolean(PREF_KEY_CONVERTED_FROM_DB, true).apply();
    }

    public void resetSettingsForTest() {
        getPreferences(this).edit().putBoolean(PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false).putBoolean(PREF_KEY_MUTE_MODE_NONE, true).putBoolean(PREF_KEY_MUTE_ALARM, true).putBoolean(PREF_KEY_MUTE_MUSIC, false).putBoolean(PREF_KEY_OUTPUT_CHANNEL_ALARM, true).putBoolean(PREF_KEY_OUTPUT_CHANNEL_MUSIC, false).apply();
    }

    public void resetDatabaseForTest() {
        Session[] readSessions = DbOperations.readSessions();
        for (int i = 0; i < readSessions.length; i++) {
            for (Section section : DbOperations.readSections(readSessions[i].id)) {
                DbOperations.deleteSection(section.id);
            }
            DbOperations.deleteSession(readSessions[i].id);
        }
        createDemoSessions();
        runOnUiThread(new Runnable() { // from class: de.gaffga.android.zazentimer.ZazenTimerActivity.15
            @Override // java.lang.Runnable
            public void run() {
                ZazenTimerActivity.this.mainFragment.updateSessionList();
            }
        });
    }
}
