package at.priv.graf.zazentimer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class SectionEndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        val serviceIntent = Intent(context, MeditationService::class.java)
        serviceIntent.action = MeditationService.ACTION_SECTION_ENDED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "ZMT_SectionEndReceiver"
    }
}
