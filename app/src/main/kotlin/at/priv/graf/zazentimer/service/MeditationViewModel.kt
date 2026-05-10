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
import androidx.lifecycle.viewModelScope
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.DbOperations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class MeditationViewModel
    @Inject
    constructor(
        @NonNull application: Application,
        private val dbOperations: DbOperations,
        private val meditationRepository: MeditationRepository,
    ) : AndroidViewModel(application) {
        private val meditationEnded = MutableLiveData<Boolean>()

        private var serviceConnection: ServCon? = null
        private var handler: Handler? = null
        private var wakeLock: PowerManager.WakeLock? = null
        private var serviceIntent: Intent? = null
        private var selectedSessionId = -1
        private var timerViewInitialized = false

        private val meditationState = MutableLiveData<MeditationUiState>()

        init {
            meditationEnded.setValue(false)
            viewModelScope.launch {
                meditationRepository.meditationState.collect { state ->
                    meditationState.setValue(state)
                }
            }
            emitIdleState()
        }

        public fun getMeditationState(): LiveData<MeditationUiState> = meditationState

        public fun getMeditationEnded(): LiveData<Boolean> = meditationEnded

        public fun notifyMeditationEnded() {
            meditationEnded.setValue(true)
        }

        public fun consumeMeditationEnded() {
            meditationEnded.setValue(false)
        }

        public fun bindToService(
            app: Application,
            h: Handler,
            callback: Runnable,
        ) {
            this.handler = h
            if (this.serviceIntent == null) {
                this.serviceIntent = Intent(app, MeditationService::class.java)
            }
            val intent = this.serviceIntent ?: return
            val conn = this.serviceConnection
            if (conn == null) {
                Log.d(TAG, "serviceConnection is null - making fresh connection service")
                val newConn = ServCon()
                this.serviceConnection = newConn
                newConn.setRunOnConnect(RunOnConnect(h, callback))
                app.bindService(intent, newConn, Context.BIND_AUTO_CREATE)
                return
            }
            if (conn.isBound()) {
                Log.d(TAG, "service is already bound")
                h.post(callback)
            } else {
                Log.d(TAG, "service comm existing, but service not bound - rebinding")
                app.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            }
        }

        public fun unbindFromService(app: Application) {
            serviceConnection?.let { conn ->
                if (conn.isBound()) {
                    try {
                        app.unbindService(conn)
                    } catch (_: Exception) {
                    }
                }
            }
            this.serviceConnection = null
        }

        public fun startMeditation(
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
            bindToService(
                app,
                this.handler ?: Handler(Looper.getMainLooper()),
                Runnable {
                    serviceConnection?.startMeditation(sessionId)
                },
            )
        }

        public fun startUpdateThread() {
            this.timerViewInitialized = false
        }

        public fun stopUpdateThread(emitIdle: Boolean = true) {
            if (emitIdle) {
                emitIdleState()
            }
        }

        public fun emitIdleState() {
            viewModelScope.launch {
                if (meditationRepository.meditationState.value !is MeditationUiState.Idle) {
                    return@launch
                }
                var sessionId = this@MeditationViewModel.selectedSessionId
                if (sessionId == -1) {
                    val prefs = ZazenTimerActivity.getPreferences(getApplication())
                    sessionId = prefs.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1)
                }
                if (sessionId == -1) {
                    meditationState.setValue(SectionArcCalculator.emptyState())
                    return@launch
                }
                val session: Session? = dbOperations.readSession(sessionId)
                if (session == null) {
                    meditationState.setValue(SectionArcCalculator.emptyState())
                    return@launch
                }
                val sections: Array<Section> = dbOperations.readSections(sessionId)
                if (sections.isEmpty()) {
                    meditationState.setValue(SectionArcCalculator.emptyState(session.name ?: ""))
                    return@launch
                }
                meditationState.setValue(SectionArcCalculator.computeIdleState(session, sections))
            }
        }

        public fun pauseMeditation() {
            serviceConnection?.pauseMeditation()
        }

        public fun stopMeditation() {
            serviceConnection?.stopMeditation()
        }

        public fun isPaused(): Boolean {
            val meditation = serviceConnection?.getRunningMeditation() ?: return false
            return meditation.isPaused()
        }

        public fun acquireScreenWakeLock(
            app: Application,
            pref: SharedPreferences,
        ) {
            val keepScreenOn = pref.getBoolean(ZazenTimerActivity.PREF_KEY_KEEP_SCREEN_ON, false)
            if (!keepScreenOn) {
                return
            }
            val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            viewModelScope.launch {
                val totalSeconds = dbOperations.readSections(selectedSessionId).sumOf { it.duration }
                wakeLock = null
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenOnWakeLock")
                val timeoutSeconds = totalSeconds + WAKELOCK_TIMEOUT_BUFFER_SECONDS
                wakeLock?.acquire(timeoutSeconds * MILLIS_PER_SECOND)
                Log.i(TAG, "Acquired WakeLock to keep screen on for $timeoutSeconds seconds")
            }
        }

        public fun releaseScreenWakeLock() {
            wakeLock?.let { lock ->
                try {
                    if (lock.isHeld) {
                        lock.release()
                    }
                } catch (e: IllegalArgumentException) {
                    Log.d(TAG, "wakeLock release error", e)
                }
                wakeLock = null
                Log.i(TAG, "ScreenOn-WakeLock released")
            }
        }

        public fun getSelectedSessionId(): Int = selectedSessionId

        public fun setSelectedSessionId(sessionId: Int) {
            this.selectedSessionId = sessionId
        }

        public fun getServiceIntent(app: Application): Intent {
            if (this.serviceIntent == null) {
                this.serviceIntent = Intent(app, MeditationService::class.java)
            }
            return this.serviceIntent ?: Intent(app, MeditationService::class.java)
        }

        public fun isServiceConnected(): Boolean = serviceConnection?.isBound() == true

        public fun setHandler(handler: Handler) {
            this.handler = handler
        }

        override fun onCleared() {
            super.onCleared()
            releaseScreenWakeLock()
        }

        companion object {
            private const val TAG = "ZMT_MeditationViewModel"
            private const val WAKELOCK_TIMEOUT_BUFFER_SECONDS = 60
            private const val MILLIS_PER_SECOND = 1000L
        }
    }
