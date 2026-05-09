package at.priv.graf.zazentimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.DbOperations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MeditationService : Service() {
    @Inject
    lateinit var dbOperations: DbOperations

    private var binder: IBinder? = null
    private var runningMeditation: Meditation? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind")
        binder = MeditationServiceBinder(this)
        return binder!!
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind")
        binder = null
        return super.onUnbind(intent)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action ?: "null"}")
        if (intent != null && ACTION_SECTION_ENDED == intent.action) {
            if (runningMeditation != null) {
                runningMeditation!!.onSectionEnd()
            } else {
                Log.w(TAG, "onStartCommand: section ended but no running meditation")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        isRunning = false
        stopMeditation()
        super.onDestroy()
    }

    fun stopMeditation() {
        Log.d(TAG, "stopMeditation")
        runningMeditation?.stop()
    }

    fun pauseMeditation(): Boolean {
        Log.d(TAG, "pauseMeditation")
        if (runningMeditation != null) {
            runningMeditation!!.pause()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, createNotification())
            }
            return runningMeditation!!.isPaused()
        }
        Log.d(TAG, "pauseMeditation(): No meditation seems to be running!")
        return true
    }

    fun startMeditation(i: Int) {
        Log.d(TAG, "startMeditation")
        if (runningMeditation != null) {
            Log.d(TAG, "startMeditation(): Meditation seems to be already running!")
            return
        }
        val session: Session = dbOperations.readSession(i) ?: return
        val sections = dbOperations.readSections(i) ?: return
        runningMeditation = Meditation(this, session.name ?: "", sections)
        runningMeditation!!.start()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, createNotification())
        }
    }

    fun onMeditationEnd() {
        Log.d(TAG, "onMeditationEnd")
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        runningMeditation = null
        val intent = Intent()
        intent.action = ZAZENTIMER_SESSION_ENDED
        sendBroadcast(intent)
        stopSelf()
    }

    fun getRunningMeditation(): Meditation? = runningMeditation

    private fun createNotification(): Notification {
        val icon: Int
        val title: String
        val text: String
        if (!runningMeditation!!.isPaused()) {
            icon = R.drawable.notify
            title = getString(R.string.notification_title)
            text = getString(R.string.notification_text)
        } else {
            icon = R.drawable.notify_paused
            title = getString(R.string.notification_title_paused)
            text = getString(R.string.notification_text_paused)
        }
        val intent = Intent(this, ZazenTimerActivity::class.java)
        intent.addFlags(536870912)
        intent.setClass(this, ZazenTimerActivity::class.java)
        val activity = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val channel = NotificationChannel("zazen_timer_channel", "Meditation Timer", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        return NotificationCompat
            .Builder(baseContext, "zazen_timer_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(activity)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFY_MEDITATION_RUNNING = 1
        private const val TAG = "ZMT_MeditationService"
        const val ZAZENTIMER_SESSION_ENDED = "ZAZENTIMER_SESSION_ENDED"
        const val ACTION_SECTION_ENDED = "ZAZENTIMER_SECTION_ENDED"

        @Volatile
        private var isRunning = false

        @JvmStatic
        fun isServiceRunning(): Boolean = isRunning
    }
}
