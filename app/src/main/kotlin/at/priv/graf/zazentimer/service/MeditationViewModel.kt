package at.priv.graf.zazentimer.service

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.DbOperations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MeditationViewModel
    @Inject
    constructor(
        @NonNull application: Application,
        private val dbOperations: DbOperations,
    ) : AndroidViewModel(application) {
        private val meditationState = MutableLiveData<MeditationUiState>()
        private val meditationEnded = MutableLiveData<Boolean>()

        private var serviceConnection: ServCon? = null
        private var handler: Handler? = null
        private var wakeLock: PowerManager.WakeLock? = null
        private var serviceIntent: Intent? = null
        private var updateRunning = false
        private var selectedSessionId = -1
        private var timerViewInitialized = false

        private val updateRunnable: Runnable =
            object : Runnable {
                override fun run() {
                    if (!updateRunning) {
                        return
                    }
                    pollMeditationState()
                    handler!!.postDelayed(this, 300L)
                }
            }

        init {
            meditationEnded.setValue(false)
            emitIdleState()
        }

        fun getMeditationState(): LiveData<MeditationUiState> = meditationState

        fun getMeditationEnded(): LiveData<Boolean> = meditationEnded

        fun notifyMeditationEnded() {
            meditationEnded.setValue(true)
        }

        fun consumeMeditationEnded() {
            meditationEnded.setValue(false)
        }

        fun bindToService(
            app: Application,
            h: Handler,
            callback: Runnable,
        ) {
            this.handler = h
            if (this.serviceIntent == null) {
                this.serviceIntent = Intent(app, MeditationService::class.java)
            }
            if (this.serviceConnection == null) {
                Log.d(TAG, "serviceConnection is null - making fresh connection service")
                this.serviceConnection = ServCon(app)
                this.serviceConnection!!.setRunOnConnect(RunOnConnect(h, callback))
                app.bindService(this.serviceIntent!!, this.serviceConnection!!, Context.BIND_AUTO_CREATE)
                return
            }
            if (this.serviceConnection!!.isBound()) {
                Log.d(TAG, "service is already bound")
                h.post(callback)
            } else {
                Log.d(TAG, "service comm existing, but service not bound - rebinding")
                app.bindService(this.serviceIntent!!, this.serviceConnection!!, Context.BIND_AUTO_CREATE)
            }
        }

        fun unbindFromService(app: Application) {
            if (this.serviceConnection != null && this.serviceConnection!!.isBound()) {
                try {
                    app.unbindService(this.serviceConnection!!)
                } catch (_: Exception) {
                }
            }
            this.serviceConnection = null
        }

        fun startMeditation(
            app: Application,
            sessionId: Int,
        ) {
            this.selectedSessionId = sessionId
            this.timerViewInitialized = false
            if (this.serviceIntent == null) {
                this.serviceIntent = Intent(app, MeditationService::class.java)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(this.serviceIntent)
            } else {
                app.startService(this.serviceIntent)
            }
            val conn = this.serviceConnection
            bindToService(
                app,
                this.handler ?: Handler(Looper.getMainLooper()),
                Runnable {
                    conn?.startMeditation(sessionId)
                },
            )
        }

        fun startUpdateThread() {
            if (this.handler == null) {
                this.handler = Handler(Looper.getMainLooper())
            }
            stopUpdateThread()
            this.updateRunning = true
            this.timerViewInitialized = false
            this.handler!!.postDelayed(this.updateRunnable, 300L)
        }

        fun stopUpdateThread() {
            this.updateRunning = false
            this.handler?.removeCallbacks(this.updateRunnable)
            emitIdleState()
        }

        fun emitIdleState() {
            var sessionId = this.selectedSessionId
            if (sessionId == -1) {
                val prefs = ZazenTimerActivity.getPreferences(getApplication())
                sessionId = prefs.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1)
            }
            if (sessionId == -1 || dbOperations == null) {
                meditationState.setValue(SectionArcCalculator.emptyState())
                return
            }
            val session: Session? = dbOperations.readSession(sessionId)
            if (session == null) {
                meditationState.setValue(SectionArcCalculator.emptyState())
                return
            }
            val sections: Array<Section>? = dbOperations.readSections(sessionId)
            if (sections == null || sections.isEmpty()) {
                meditationState.setValue(SectionArcCalculator.emptyState(session.name ?: ""))
                return
            }
            meditationState.setValue(SectionArcCalculator.computeIdleState(session, sections))
        }

        private fun pollMeditationState() {
            if (this.serviceConnection == null) {
                return
            }
            val meditation = this.serviceConnection!!.getRunningMeditation() ?: return
            if (meditation.getCurrentSection() == null) {
                return
            }
            if (!this.timerViewInitialized) {
                this.timerViewInitialized = true
                val initState =
                    MeditationUiState(
                        0,
                        meditation.getTotalSessionTime(),
                        meditation.getNextEndSeconds(),
                        meditation.getNextStartSeconds(),
                        meditation.getPrevStartSeconds(),
                        0,
                        0,
                        meditation.getCurrentSectionName(),
                        meditation.getNextSectionName(),
                        meditation.getNextNextSectionName(),
                        meditation.getSessionName(),
                        meditation.isPaused(),
                        true,
                    )
                meditationState.setValue(initState)
                return
            }
            val state =
                MeditationUiState(
                    meditation.getCurrentStartSeconds(),
                    meditation.getTotalSessionTime(),
                    meditation.getNextEndSeconds(),
                    meditation.getNextStartSeconds(),
                    meditation.getPrevStartSeconds(),
                    meditation.getSectionElapsedSeconds(),
                    meditation.getCurrentSessionTime(),
                    meditation.getCurrentSectionName(),
                    meditation.getNextSectionName(),
                    meditation.getNextNextSectionName(),
                    meditation.getSessionName(),
                    meditation.isPaused(),
                    true,
                )
            meditationState.setValue(state)
        }

        fun pauseMeditation() {
            serviceConnection?.pauseMeditation()
        }

        fun stopMeditation() {
            serviceConnection?.stopMeditation()
        }

        fun isPaused(): Boolean {
            val meditation = serviceConnection?.getRunningMeditation() ?: return false
            return meditation.isPaused()
        }

        fun acquireScreenWakeLock(
            app: Application,
            pref: SharedPreferences,
        ) {
            val keepScreenOn = pref.getBoolean(ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON, false)
            if (!keepScreenOn) {
                return
            }
            val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            val totalSeconds = dbOperations.readSections(selectedSessionId).sumOf { it.duration }
            wakeLock = null
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenOnWakeLock")
            val timeoutSeconds = totalSeconds + 60
            wakeLock!!.acquire(timeoutSeconds * 1000L)
            Log.i(TAG, "Acquired WakeLock to keep screen on for $timeoutSeconds seconds")
        }

        fun releaseScreenWakeLock() {
            if (wakeLock != null) {
                try {
                    if (wakeLock!!.isHeld) {
                        wakeLock!!.release()
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "wakeLock release error", e)
                }
                wakeLock = null
                Log.i(TAG, "ScreenOn-WakeLock released")
            }
        }

        fun getSelectedSessionId(): Int = selectedSessionId

        fun setSelectedSessionId(sessionId: Int) {
            this.selectedSessionId = sessionId
        }

        fun getServiceIntent(app: Application): Intent {
            if (this.serviceIntent == null) {
                this.serviceIntent = Intent(app, MeditationService::class.java)
            }
            return this.serviceIntent!!
        }

        fun isServiceConnected(): Boolean = serviceConnection != null && serviceConnection!!.isBound()

        fun setHandler(handler: Handler) {
            this.handler = handler
        }

        override fun onCleared() {
            super.onCleared()
            stopUpdateThread()
            releaseScreenWakeLock()
        }

        companion object {
            private const val TAG = "ZMT_MeditationViewModel"
        }
    }
