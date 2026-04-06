package de.gaffga.android.zazentimer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class SectionEndReceiver extends BroadcastReceiver {
    private static final String TAG = "ZMT_SectionEndReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: action=" + intent.getAction());
        Intent serviceIntent = new Intent(context, MeditationService.class);
        serviceIntent.setAction(MeditationService.ACTION_SECTION_ENDED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
