package de.gaffga.android.zazentimer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;

public class MeditationService extends Service {
    private static final int NOTIFY_MEDITATION_RUNNING = 1;
    private static final String TAG = "ZMT_MeditationService";
    public static final String ZAZENTIMER_SESSION_ENDED = "ZAZENTIMER_SESSION_ENDED";
    public static final String ACTION_SECTION_ENDED = "ZAZENTIMER_SECTION_ENDED";
    private static volatile boolean isRunning = false;
    private IBinder binder;
    private Meditation runningMeditation;

    public static boolean isServiceRunning() { return isRunning; }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        this.binder = new MeditationServiceBinder(this);
        return this.binder;
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        this.binder = null;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: action=" + (intent != null ? intent.getAction() : "null"));
        if (intent != null && ACTION_SECTION_ENDED.equals(intent.getAction())) {
            if (this.runningMeditation != null) {
                this.runningMeditation.onSectionEnd();
            } else {
                Log.w(TAG, "onStartCommand: section ended but no running meditation");
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override // android.app.Service
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        isRunning = false;
        stopMeditation();
        super.onDestroy();
    }

    public void stopMeditation() {
        Log.d(TAG, "stopMeditation");
        if (this.runningMeditation != null) {
            this.runningMeditation.stop();
        }
    }

    public boolean pauseMeditation() {
        Log.d(TAG, "pauseMeditation");
        if (this.runningMeditation != null) {
            this.runningMeditation.pause();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, createNotification());
        }
            return this.runningMeditation.isPaused();
        }
        Log.d(TAG, "pauseMeditation(): No meditation seems to be running!");
        return true;
    }

    public void startMeditation(int i) {
        Log.d(TAG, "startMeditation");
        if (this.runningMeditation != null) {
            Log.d(TAG, "startMeditation(): Meditation seems to be already running!");
            return;
        }
        this.runningMeditation = new Meditation(this, DbOperations.readSections(i));
        this.runningMeditation.start();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, createNotification());
        }
    }

    public void onMeditationEnd() {
        Log.d(TAG, "onMeditationEnd");
        stopForeground(true);
        this.runningMeditation = null;
        Intent intent = new Intent();
        intent.setAction(ZAZENTIMER_SESSION_ENDED);
        sendBroadcast(intent);
        stopSelf();
    }

    public Meditation getRunningMeditation() {
        return this.runningMeditation;
    }

    private Notification createNotification() {
        int i;
        String string;
        String string2;
        if (!this.runningMeditation.isPaused()) {
            i = R.drawable.notify;
            string = getString(R.string.notification_title);
            string2 = getString(R.string.notification_text);
        } else {
            i = R.drawable.notify_paused;
            string = getString(R.string.notification_title_paused);
            string2 = getString(R.string.notification_text_paused);
        }
        Intent intent = new Intent(this, (Class<?>) ZazenTimerActivity.class);
        intent.addFlags(536870912);
        intent.setClass(this, ZazenTimerActivity.class);
        Notification.Builder builder;
        PendingIntent activity = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("zazen_timer_channel", "Meditation Timer", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(getBaseContext(), "zazen_timer_channel");
        } else {
            builder = new Notification.Builder(getBaseContext());
        }
        builder.setContentTitle(string);
        builder.setContentText(string2);
        builder.setSmallIcon(i);
        builder.setContentIntent(activity);
        Notification build = builder.build();
        build.flags |= 98;
        return build;
    }
}
