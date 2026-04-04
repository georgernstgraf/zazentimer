package de.gaffga.android.zazentimer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;

/* loaded from: classes.dex */
public class MeditationService extends Service {
    private static final int NOTIFY_MEDITATION_RUNNING = 1;
    private static final String TAG = "ZMT_MeditationService";
    public static final String ZAZENTIMER_SESSION_ENDED = "ZAZENTIMER_SESSION_ENDED";
    private IBinder binder;
    private Meditation runningMeditation;

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

    @Override // android.app.Service
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
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
            startForeground(1, createNotification());
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
        startForeground(1, createNotification());
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
        PendingIntent activity = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(getBaseContext());
        builder.setContentTitle(string);
        builder.setContentText(string2);
        builder.setSmallIcon(i);
        builder.setContentIntent(activity);
        Notification build = builder.build();
        build.flags |= 98;
        return build;
    }
}
