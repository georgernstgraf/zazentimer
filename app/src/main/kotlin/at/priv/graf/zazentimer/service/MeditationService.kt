package at.priv.graf.zazentimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class MeditationService : LifecycleService() {
    @Inject
    lateinit var meditationRepository: MeditationRepository

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var clock: ZazenClock

    private var binder: IBinder? = null
    private var runningMeditation: Meditation? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        val notification =
            NotificationCompat
                .Builder(baseContext, "zazen_timer_channel")
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.notify)
                .setOngoing(true)
                .build()
        startForegroundCompat(1, notification)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        val b = MeditationServiceBinder(this)
        binder = b
        return b
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
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand: action=${intent?.action ?: "null"}")
        if (intent != null && ACTION_SECTION_ENDED == intent.action) {
            runningMeditation?.onSectionEnd() ?: run {
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
        val meditation = runningMeditation
        var result = true
        if (meditation != null) {
            meditation.pause()
            val notification = createNotification()
            if (notification != null) {
                startForegroundCompat(1, notification)
                result = meditation.isPaused()
            }
        } else {
            Log.d(TAG, "pauseMeditation(): No meditation seems to be running!")
        }
        return result
    }

    fun startMeditation(i: Int) {
        Log.d(TAG, "startMeditation")
        if (runningMeditation != null) {
            Log.d(TAG, "startMeditation(): Meditation seems to be already running!")
            return
        }
        lifecycleScope.launch {
            val session = meditationRepository.readSession(i) ?: return@launch
            val sections = meditationRepository.readSections(i)
            val bellVolumes = meditationRepository.readBellVolumes(i)
            if (sections.isEmpty()) return@launch
            runningMeditation =
                Meditation(
                    this@MeditationService,
                    meditationRepository,
                    session.name ?: "",
                    sections,
                    bellVolumes,
                    coroutineDispatchers,
                    AudioStateManager(
                        this@MeditationService,
                        ZazenTimerActivity.getPreferences(this@MeditationService),
                    ),
                    AlarmScheduler(this@MeditationService, clock),
                    BellPlayer(this@MeditationService, coroutineDispatchers),
                )
            val meditation = runningMeditation ?: return@launch
            meditation.start()
            val notification = createNotification() ?: return@launch
            startForegroundCompat(1, notification)
        }
    }

    fun onMeditationEnd() {
        Log.d(TAG, "onMeditationEnd")
        isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        runningMeditation?.release()
        runningMeditation = null
        val intent = Intent()
        intent.action = ZAZENTIMER_SESSION_ENDED
        intent.setPackage(packageName)
        sendBroadcast(intent)
        stopSelf()
    }

    fun getRunningMeditation(): Meditation? = runningMeditation

    private fun createNotification(): Notification? {
        val meditation = runningMeditation ?: return null
        val icon: Int
        val title: String
        val text: String
        if (!meditation.isPaused()) {
            icon = R.drawable.notify
            title = getString(R.string.notification_title)
            text = getString(R.string.notification_text)
        } else {
            icon = R.drawable.notify_paused
            title = getString(R.string.notification_title_paused)
            text = getString(R.string.notification_text_paused)
        }
        val intent = Intent(this, ZazenTimerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.setClass(this, ZazenTimerActivity::class.java)
        val activity = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        createNotificationChannel()
        return NotificationCompat
            .Builder(baseContext, "zazen_timer_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(activity)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    "zazen_timer_channel",
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundCompat(
        id: Int,
        notification: Notification,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    companion object {
        private const val TAG = "ZMT_MeditationService"
        const val ZAZENTIMER_SESSION_ENDED: String = "ZAZENTIMER_SESSION_ENDED"
        const val ACTION_SECTION_ENDED: String = "ZAZENTIMER_SECTION_ENDED"

        @Volatile
        private var isRunning = false

        @JvmStatic
        fun isServiceRunning(): Boolean = isRunning

        @JvmStatic
        fun setRunning(running: Boolean) {
            isRunning = running
        }
    }
}
