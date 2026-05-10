package at.priv.graf.zazentimer.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

class ServCon : ServiceConnection {
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

    fun isBound(): Boolean = binder?.getService() != null

    override fun onServiceDisconnected(componentName: ComponentName) {
        Log.d(TAG, "Service disconnected")
        binder = null
    }

    fun startMeditation(i: Int) {
        val b = binder
        if (b == null) {
            Log.d(TAG, "startMeditation(): No service bound!")
        } else {
            b.getService().startMeditation(i)
        }
    }

    fun pauseMeditation() {
        val b = binder
        if (b == null) {
            Log.d(TAG, "pauseMeditation(): No service bound!")
        } else {
            b.getService().pauseMeditation()
        }
    }

    fun stopMeditation() {
        val b = binder
        if (b == null) {
            Log.d(TAG, "stopMeditation(): No service bound!")
        } else {
            b.getService().stopMeditation()
        }
    }

    fun getRunningMeditation(): Meditation? {
        val b = binder
        if (b == null) {
            Log.d(TAG, "getRunningMeditation(): No service bound!")
            return null
        }
        return b.getService().getRunningMeditation()
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
