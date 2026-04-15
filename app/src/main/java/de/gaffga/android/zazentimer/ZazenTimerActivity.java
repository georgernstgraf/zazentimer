package de.gaffga.android.zazentimer;

import android.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import de.gaffga.android.fragments.MainFragment;
import de.gaffga.android.zazentimer.audio.BellCollection;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.BuildConfig;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.android.zazentimer.service.MeditationService;
import de.gaffga.android.zazentimer.service.MeditationViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@AndroidEntryPoint
public class ZazenTimerActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener {
    public static final String INTENT_DATA_SHOW_PREF_ON_START = "gotoPrefs";
    public static final int PREF_DEFAULT_BRIGHTNESS = 0;
    public static final boolean PREF_DEFAULT_CONVERTED_BELL_INDICES = false;
    public static final boolean PREF_DEFAULT_CONVERTED_FROM_DB = false;
    public static final boolean PREF_DEFAULT_FIRST_START = true;
    public static final boolean PREF_DEFAULT_KEEP_SCREEN_ON = false;
    public static final int PREF_DEFAULT_LAST_SESSION = -1;
    public static final boolean PREF_DEFAULT_MUTE_MODE_NONE = true;
    public static final boolean PREF_DEFAULT_MUTE_MODE_VIBRATE = false;
    public static final boolean PREF_DEFAULT_MUTE_MODE_VIBRATE_SOUND = false;
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
    public static final String PREF_KEY_MUTE_MODE_NONE = "mute_mode_none";
    public static final String PREF_KEY_MUTE_MODE_VIBRATE = "mute_mode_vibrate";
    public static final String PREF_KEY_MUTE_MODE_VIBRATE_SOUND = "mute_mode_vibrate_sound";
    public static final String PREF_KEY_PHONE_OFF = "phone_off";
    public static final String PREF_KEY_SHOW_ELAPSED_TIME = "show_elapsed_time";
    public static final String PREF_KEY_SHOW_SESSION_EDIT_HELP_V13 = "session_edit_help_13";
    public static final String PREF_KEY_SHOW_TIME_MODE = "view_time_mode";
    public static final String PREF_KEY_THEME = "theme";
    public static final String PREF_KEY_VOLUME = "volume";
    public static final String PREF_VALUE_THEME_DARK = "dark";
    public static final String PREF_VALUE_THEME_LIGHT = "light";
    private static final String TAG = "ZMT_ZazenTimerActivity";
    private MeditationEndReceiver meditationEndReceiver;
    private SharedPreferences pref;
    final Intent intentAllowMuting = new Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS");
    private boolean created = false;
    private boolean showPrefsOnStart = false;
    private MeditationViewModel viewModel;
    private boolean appRunning = false;
    private Handler handler;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private ImageView zenIndicator;

    @Inject DbOperations dbOperations;

    private NavController getNavController() {
        if (navController == null) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
            }
        }
        return navController;
    }

    private static class MeditationEndReceiver extends BroadcastReceiver {
        private final ZazenTimerActivity activity;

        MeditationEndReceiver(ZazenTimerActivity zazenTimerActivity) {
            this.activity = zazenTimerActivity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.activity.serviceMeditationEndNotify();
        }
    }

    @Override
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
        this.viewModel = new ViewModelProvider(this).get(MeditationViewModel.class);
        this.viewModel.setHandler(this.handler);
        BellCollection.getInstance().init(this);
        convertFromOldVersions();
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        zenIndicator = findViewById(R.id.zenIndicator);
        NavController nc = getNavController();
        if (nc != null) {
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.mainFragment, R.id.meditationFragment, R.id.settingsFragment)
                    .build();
            NavigationUI.setupActionBarWithNavController(this, nc, appBarConfiguration);
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                NavigationUI.setupWithNavController(bottomNav, nc);
            }
            nc.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                @Override
                public void onDestinationChanged(NavController controller,
                        NavDestination destination, Bundle arguments) {
                    if (bottomNav != null) {
                        int destId = destination.getId();
                        if (destId == R.id.sessionEditFragment || destId == R.id.sectionEditFragment) {
                            bottomNav.setVisibility(View.GONE);
                        } else {
                            bottomNav.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
        observeViewModel();
        if (preferences.getBoolean(PREF_KEY_FIRST_START, true)) {
            Log.d(TAG, "This is the first run - create demo sessions");
            createDemoSessions();
            preferences.edit().putBoolean(PREF_KEY_FIRST_START, false).apply();
        }
        BellCollection.getInstance().init(this);
    }

    private void observeViewModel() {
        viewModel.getMeditationEnded().observe(this, ended -> {
            if (ended != null && ended) {
                viewModel.consumeMeditationEnded();
                viewModel.stopUpdateThread();
                viewModel.unbindFromService((Application) getApplicationContext());
                stopService(viewModel.getServiceIntent((Application) getApplicationContext()));
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                attributes.screenBrightness = -1.0f;
                getWindow().setAttributes(attributes);
                viewModel.releaseScreenWakeLock();
                if (appRunning) {
                    showMainScreen();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        viewModel.stopUpdateThread();
        unregisterReceiver(this.meditationEndReceiver);
        this.appRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (zenIndicator != null) {
            zenIndicator.setVisibility(MeditationService.isServiceRunning() ? View.VISIBLE : View.GONE);
        }
        Log.d(TAG, "onResume");
        this.appRunning = true;
        ContextCompat.registerReceiver(this, this.meditationEndReceiver, new IntentFilter(MeditationService.ZAZENTIMER_SESSION_ENDED), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (MeditationService.isServiceRunning()) {
            Log.d(TAG, "MeditationService currently running");
            viewModel.bindToService((Application) getApplicationContext(), this.handler, new Runnable() {
                @Override
                public void run() {
                    if (viewModel.isServiceConnected() && viewModel.isPaused() || viewModel.isServiceConnected()) {
                        Log.d(TAG, "A Meditation is currently running");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (created) {
                                    showMeditationScreen();
                                }
                                viewModel.startUpdateThread();
                            }
                        });
                    } else {
                        Log.d(TAG, "No Meditation is currently running");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMainScreen();
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

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController nc = getNavController();
        if (nc != null && appBarConfiguration != null) {
            return NavigationUI.navigateUp(nc, appBarConfiguration);
        }
        return super.onSupportNavigateUp();
    }

    public boolean isMeditationScreenShown() {
        NavController nc = getNavController();
        if (nc == null || nc.getCurrentDestination() == null) {
            return false;
        }
        return nc.getCurrentDestination().getId() == R.id.meditationFragment;
    }

    public void showSettingsScreen() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.settingsFragment);
    }

    public void showMeditationScreen() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.meditationFragment);
    }

    public void showMainScreen() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.mainFragment);
    }

    public void showSessionEditFragment(int sessionId) {
        NavController nc = getNavController();
        if (nc == null) return;
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        nc.navigate(R.id.action_mainFragment_to_sessionEditFragment, args);
    }

    public void showPrivacyScreen() {
        new AlertDialog.Builder(this).setTitle(R.string.privacy_title).setMessage(R.string.privacy_message).setPositiveButton(R.string.privacy_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).create().show();
    }

    public void showAboutScreen() {
        String message = "Commit: " + BuildConfig.GIT_HASH + "\n\n"
                + getString(R.string.about1) + "\n\n"
                + getString(R.string.about2) + "\n\n"
                + getString(R.string.about3);
        new AlertDialog.Builder(this)
                .setTitle(R.string.caption_zazen_meditation)
                .setMessage(message)
                .setPositiveButton(R.string.privacy_ok, (dialog, which) -> dialog.dismiss())
                .create().show();
    }

    public void startMeditation() {
        doStartMediation();
    }

    public void doStartMediation() {
        boolean z = this.pref.getBoolean(PREF_KEY_KEEP_SCREEN_ON, false);
        if (z) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.screenBrightness = this.pref.getInt(PREF_KEY_BRIGHTNESS, 0) / 100.0f;
            getWindow().setAttributes(attributes);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        viewModel.setSelectedSessionId(getSelectedSessionId());
        viewModel.acquireScreenWakeLock((Application) getApplicationContext(), this.pref);
        viewModel.startMeditation((Application) getApplicationContext(), getSelectedSessionId());
        showMeditationScreen();
        viewModel.startUpdateThread();
        if (zenIndicator != null) {
            zenIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_privacy:
                Log.d(TAG, "privacy");
                showPrivacyScreen();
                return true;
            case R.id.menu_about:
                showAboutScreen();
                return true;
            case R.id.menu_session_edit_help:
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        NavController nc = getNavController();
        if (nc != null && nc.getCurrentDestination() != null
                && nc.getCurrentDestination().getId() == R.id.mainFragment) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private MainFragment findMainFragment() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            Fragment f = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
            if (f instanceof MainFragment) {
                return (MainFragment) f;
            }
        }
        return null;
    }

    private void createDemoSessions() {
        Session session = new Session();
        session.description = getResources().getString(R.string.demo_sess1_description);
        session.name = getResources().getString(R.string.demo_sess1_name);
        dbOperations.insertSession(session);
        Section section = new Section();
        section.bell = -2;
        section.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88).getUri().toString();
        section.bellcount = 1;
        section.bellpause = 1;
        section.duration = 30;
        section.name = getResources().getString(R.string.demo_sess1_sec1_name);
        section.rank = 1;
        dbOperations.insertSection(session, section);
        Section section2 = new Section();
        section2.bell = -2;
        section2.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section2.bellcount = 2;
        section2.bellpause = 3;
        section2.duration = 900;
        section2.name = getResources().getString(R.string.demo_sess1_sec2_name);
        section2.rank = 2;
        dbOperations.insertSection(session, section2);
        Section section3 = new Section();
        section3.bell = -2;
        section3.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88).getUri().toString();
        section3.bellcount = 2;
        section3.bellpause = 3;
        section3.duration = 300;
        section3.name = getResources().getString(R.string.demo_sess1_sec3_name);
        section3.rank = 3;
        dbOperations.insertSection(session, section3);
        Section section4 = new Section();
        section4.bell = -2;
        section4.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section4.bellcount = 2;
        section4.bellpause = 3;
        section4.duration = 900;
        section4.name = getResources().getString(R.string.demo_sess1_sec4_name);
        section4.rank = 4;
        dbOperations.insertSection(session, section4);
        Section section5 = new Section();
        section5.bell = -2;
        section5.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_88).getUri().toString();
        section5.bellcount = 2;
        section5.bellpause = 3;
        section5.duration = 300;
        section5.name = getResources().getString(R.string.demo_sess1_sec5_name);
        section5.rank = 5;
        dbOperations.insertSection(session, section5);
        Section section6 = new Section();
        section6.bell = -2;
        section6.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section6.bellcount = 2;
        section6.bellpause = 3;
        section6.duration = 900;
        section6.name = getResources().getString(R.string.demo_sess1_sec6_name);
        section6.rank = 6;
        dbOperations.insertSection(session, section6);
        Session session2 = new Session();
        session2.description = getResources().getString(R.string.demo_sess2_description);
        session2.name = getResources().getString(R.string.demo_sess2_name);
        dbOperations.insertSession(session2);
        Section section7 = new Section();
        section7.bell = -2;
        section7.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_TIB_RHINBOWL_230).getUri().toString();
        section7.bellcount = 1;
        section7.bellpause = 1;
        section7.duration = 5;
        section7.name = getResources().getString(R.string.demo_sess1_sec1_name);
        section7.rank = 1;
        dbOperations.insertSection(session2, section7);
        Section section8 = new Section();
        section8.bell = -2;
        section8.bellUri = BellCollection.getInstance().getBell(BellCollection.BELL_IDX_JAP_RHINBOWL_107).getUri().toString();
        section8.bellcount = 2;
        section8.bellpause = 3;
        section8.duration = 600;
        section8.name = getResources().getString(R.string.demo_sess1_sec2_name);
        section8.rank = 2;
        dbOperations.insertSection(session2, section8);
    }

    @Override
    public void onStartPressed() {
        if (dbOperations.readSections(getSelectedSessionId()).length == 0) {
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
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
        new AlertDialog.Builder(this).setTitle(R.string.title_mute_perm_request).setMessage(R.string.test_mute_perm_request).setIcon(R.drawable.icon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    ZazenTimerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ZazenTimerActivity.this.startActivity(intent);
                            } catch (Exception unused) {
                                ZazenTimerActivity.this.showMessageNoMuteSettings();
                            }
                        }
                    });
                } catch (Exception unused) {
                    ZazenTimerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ZazenTimerActivity.this.showMessageNoMuteSettings();
                        }
                    });
                }
            }
        }).setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    public void showMessageNoMuteSettings() {
        new AlertDialog.Builder(this).setTitle(R.string.title_mute_perm_request).setMessage(R.string.text_no_notify_access_settings).setIcon(R.drawable.icon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).show();
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> queryIntentActivities = getPackageManager().queryIntentActivities(intent, 65536);
        return queryIntentActivities != null && queryIntentActivities.size() > 0;
    }

    public void serviceMeditationEndNotify() {
        if (zenIndicator != null) {
            zenIndicator.setVisibility(View.GONE);
        }
        viewModel.notifyMeditationEnded();
    }

    public int getSelectedSessionId() {
        MainFragment f = findMainFragment();
        if (f != null) {
            return f.getSelectedSessionId();
        }
        return viewModel.getSelectedSessionId();
    }

    private void convertFromOldVersions() {
        this.pref = getPreferences(this);
        if (!this.pref.getBoolean(PREF_KEY_CONVERTED_FROM_DB, false)) {
            Log.d(TAG, "marking settings as converted from DB to preferences...");
            this.pref.edit().putBoolean(PREF_KEY_CONVERTED_FROM_DB, true).apply();
            Log.d(TAG, "done converting settings");
        }
        if (!this.pref.getBoolean(PREF_KEY_CONVERTED_BELL_INDICES, false)) {
            Log.d(TAG, "converting Bell Indices to URIs...");
            convertBellIndices();
            Log.d(TAG, "done converting Bell Indices");
        }
        if (this.pref.contains(PREF_KEY_PHONE_OFF)) {
            if (this.pref.getBoolean(PREF_KEY_PHONE_OFF, true)) {
                this.pref.edit().putBoolean(PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false).putBoolean(PREF_KEY_MUTE_MODE_NONE, true).remove(PREF_KEY_PHONE_OFF).apply();
            } else {
                this.pref.edit().putBoolean(PREF_KEY_MUTE_MODE_VIBRATE_SOUND, true).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false).putBoolean(PREF_KEY_MUTE_MODE_NONE, false).remove(PREF_KEY_PHONE_OFF).apply();
            }
        }
    }

    private void convertBellIndices() {
        for (Session session : dbOperations.readSessions()) {
            for (Section section : dbOperations.readSections(session.id)) {
                if (section.bellUri == null || section.bellUri.trim().length() == 0) {
                    Bell bell = BellCollection.getInstance().getBell(section.bell);
                    if (bell != null) {
                        section.bellUri = bell.getUri().toString();
                        section.bell = -2;
                        dbOperations.updateSection(section);
                    } else {
                        section.bellUri = BellCollection.getInstance().getDemoBell().getUri().toString();
                        section.bell = -2;
                        dbOperations.updateSection(section);
                    }
                } else if (section.bell == -1) {
                    section.bell = -2;
                    section.bellUri = BellCollection.getInstance().getDemoBell().getUri().toString();
                    dbOperations.updateSection(section);
                }
            }
        }
    }

    public void resetSettingsForTest() {
        getPreferences(this).edit().putBoolean(PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false).putBoolean(PREF_KEY_MUTE_MODE_VIBRATE, false).putBoolean(PREF_KEY_MUTE_MODE_NONE, true).apply();
    }

    public void resetDatabaseForTest() {
        Session[] readSessions = dbOperations.readSessions();
        for (int i = 0; i < readSessions.length; i++) {
            for (Section section : dbOperations.readSections(readSessions[i].id)) {
                dbOperations.deleteSection(section.id);
            }
            dbOperations.deleteSession(readSessions[i].id);
        }
        createDemoSessions();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainFragment f = ZazenTimerActivity.this.findMainFragment();
                if (f != null) {
                    f.updateSessionList();
                }
            }
        });
    }
}
