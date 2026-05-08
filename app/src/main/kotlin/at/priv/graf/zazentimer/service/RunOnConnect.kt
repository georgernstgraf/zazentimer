package at.priv.graf.zazentimer.service

import android.os.Handler

class RunOnConnect(
    private val handler: Handler,
    private val runOnConnect: Runnable
) {

    fun getHandler(): Handler = handler

    fun getRunOnConnect(): Runnable = runOnConnect
}
