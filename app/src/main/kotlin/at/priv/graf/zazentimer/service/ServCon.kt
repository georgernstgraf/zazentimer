package at.priv.graf.zazentimer.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

class ServCon(
    context: Context,
) : ServiceConnection {
    private var binder: MeditationServiceBinder? = null
    private var runOnConnect: RunOnConnect? = null

    override fun onServiceConnected(
        componentName: ComponentName,
        iBinder: IBinder,
    ) {
        Log.d(TAG, "Service connected")
        binder = iBinder as MeditationServiceBinder
        runOnConnect?.let {
            it.getHandler().post(it.getRunOnConnect())
        }
    }

    fun isBound(): Boolean = binder != null && binder?.getService() != null

    override fun onServiceDisconnected(componentName: ComponentName) {
        Log.d(TAG, "Service disconnected")
        binder = null
    }

    fun startMeditation(i: Int) {
        if (binder == null) {
            Log.d(TAG, "startMeditation(): No service bound!")
        } else {
            binder!!.getService().startMeditation(i)
        }
    }

    fun pauseMeditation() {
        if (binder == null) {
            Log.d(TAG, "pauseMeditation(): No service bound!")
        } else {
            binder!!.getService().pauseMeditation()
        }
    }

    fun stopMeditation() {
        if (binder == null) {
            Log.d(TAG, "stopMeditation(): No service bound!")
        } else {
            binder!!.getService().stopMeditation()
        }
    }

    fun getRunningMeditation(): Meditation? {
        if (binder == null) {
            Log.d(TAG, "getRunningMeditation(): No service bound!")
            return null
        }
        return binder!!.getService().getRunningMeditation()
    }

    fun setRunOnConnect(runOnConnect: RunOnConnect) {
        this.runOnConnect = runOnConnect
    }

    fun getRunOnConnect(): RunOnConnect? = runOnConnect

    fun getBinder(): MeditationServiceBinder? = binder

    companion object {
        private const val TAG = "ZMT_ServiceConnection"
    }
}
