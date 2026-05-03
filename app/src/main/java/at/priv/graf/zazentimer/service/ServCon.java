package at.priv.graf.zazentimer.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.content.Context;
import at.priv.graf.zazentimer.RunOnConnect;

public class ServCon implements ServiceConnection {
    private static final String TAG = "ZMT_ServiceConnection";
    private MeditationServiceBinder binder = null;
    private RunOnConnect runOnConnect;

    public ServCon(Context context) {
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "Service connected");
        this.binder = (MeditationServiceBinder) iBinder;
        if (this.runOnConnect != null) {
            this.runOnConnect.getHandler().post(this.runOnConnect.getRunOnConnect());
        }
    }

    public boolean isBound() {
        return (this.binder == null || this.binder.getService() == null) ? false : true;
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "Service disconnected");
        this.binder = null;
    }

    public void startMeditation(int i) {
        if (this.binder == null) {
            Log.d(TAG, "startMeditation(): No service bound!");
        } else {
            this.binder.getService().startMeditation(i);
        }
    }

    public void pauseMeditation() {
        if (this.binder == null) {
            Log.d(TAG, "pauseMeditation(): No service bound!");
        } else {
            this.binder.getService().pauseMeditation();
        }
    }

    public void stopMeditation() {
        if (this.binder == null) {
            Log.d(TAG, "stopMeditation(): No service bound!");
        } else {
            this.binder.getService().stopMeditation();
        }
    }

    public Meditation getRunningMeditation() {
        if (this.binder == null) {
            Log.d(TAG, "getRunningMeditation(): No service bound!");
            return null;
        }
        return this.binder.getService().getRunningMeditation();
    }

    public void setRunOnConnect(RunOnConnect runOnConnect) {
        this.runOnConnect = runOnConnect;
    }

    public RunOnConnect getRunOnConnect() {
        return this.runOnConnect;
    }

    public MeditationServiceBinder getBinder() {
        return this.binder;
    }
}
