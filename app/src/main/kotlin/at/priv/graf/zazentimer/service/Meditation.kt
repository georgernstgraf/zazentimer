package at.priv.graf.zazentimer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.Section
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.HashSet

class Meditation(
    private val meditationService: MeditationService,
    private val sessionName: String,
    private val sections: Array<Section>,
) {
    private var alarmManager: AlarmManager = meditationService.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var currentSectionEndIntent: PendingIntent? = null
    private var currentSectionIdx: Int = 0
    private var meditationWakeLock: PowerManager.WakeLock? = null
    private var oldRingerMode: Int = 0
    private var oldRingerVolume: Int = 0
    private var mutedRingerMode: Int = -1
    private var pauseSectionSeconds: Int = 0

    @Volatile
    private var paused: Boolean = false

    private val pref: SharedPreferences = ZazenTimerActivity.getPreferences(meditationService)
    private var sectionStartTime: Long = 0

    @Volatile
    private var stopping: Boolean = false

    private var totalSessionTime: Int = 0
    private var audioObjects: HashSet<Audio> = HashSet()
    private var started: Boolean = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        this.stopping = false
        this.paused = false
        this.totalSessionTime = 0
        this.currentSectionIdx = -1
        this.currentSectionIdx = 0
        this.totalSessionTime = MeditationTimer.getTotalSessionTime(sections)
    }

    fun getSessionName(): String = sessionName

    fun start() {
        if (started) {
            Log.d(TAG, "start(): Meditation already started!")
            return
        }
        started = true
        mutePhone()
        startSectionTimer()
        if (Build.VERSION.SDK_INT < 23) {
            createMeditationWakeLock()
        }
    }

    fun stop() {
        if (!started) {
            Log.d(TAG, "stop(): Meditation not yet started!")
        } else {
            finishMeditation()
        }
    }

    fun pause() {
        if (!started || stopping) {
            Log.d(TAG, "pause(): Meditation not yet started or already stopped")
            return
        }
        if (!paused) {
            pauseSectionSeconds = getSectionElapsedSeconds()
            paused = true
            currentSectionEndIntent?.let { alarmManager.cancel(it) }
               	currentSectionEndIntent = null
            if (Build.VERSION.SDK_INT < 23) {
                releaseMeditationWakeLock()
                return
            }
            return
        }
        paused = false
        startSectionTimer()
        if (Build.VERSION.SDK_INT < 23) {
            createMeditationWakeLock()
        }
    }

    fun finishMeditation() {
        stopping = true
        stopSectionTimer()
        releaseAudioObjects()
        unmutePhone()
        if (Build.VERSION.SDK_INT < 23) {
            releaseMeditationWakeLock()
        }
        fireMeditationEnded()
    }

    fun release() {
        scope.cancel()
    }

    private fun releaseMeditationWakeLock() {
        Log.d(TAG, "Releasing meditation wake lock")
        meditationWakeLock?.let { lock ->
            try {
                if (lock.isHeld) {
                    lock.release()
                }
            } catch (e: Exception) {
                Log.d(TAG, "error releasing wake lock", e)
            }
        }
    }

    private fun createMeditationWakeLock() {
        Log.d(TAG, "Creating meditation wake lock")
        val powerManager = meditationService.getSystemService(Context.POWER_SERVICE) as PowerManager
        meditationWakeLock = powerManager.newWakeLock(1, "MeditationWakeLock")
        meditationWakeLock?.acquire(((totalSessionTime + 120) * 1000).toLong())
    }

    private fun fireMeditationEnded() {
        meditationService.onMeditationEnd()
    }

    private fun stopSectionTimer() {
        currentSectionEndIntent?.let { alarmManager.cancel(it) }
        currentSectionEndIntent = null
    }

    private fun startSectionTimer() {
        val section = sections[currentSectionIdx]
        sectionStartTime = System.currentTimeMillis()
        val triggerTime = sectionStartTime + ((section.duration - pauseSectionSeconds) * 1000L)
        val pendingIntent =
            PendingIntent.getBroadcast(
                meditationService,
                0,
                Intent(INTENT_SECTION_ENDED).setClass(meditationService, SectionEndReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        currentSectionEndIntent = pendingIntent
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        Log.d(TAG, "Started AlarmClock for next section: triggerTime=$triggerTime")
    }

    fun onSectionEnd() {
        currentSectionEndIntent = null
        if (currentSectionIdx < sections.size - 1) {
            playBells(sections[currentSectionIdx], null)
            currentSectionIdx++
            pauseSectionSeconds = 0
            startSectionTimer()
            return
        }
        playBells(sections[currentSectionIdx]) { finishMeditation() }
    }

    fun getCurrentSection(): Section = sections[currentSectionIdx]

    fun getCurrentSectionName(): String = if (currentSectionIdx >= 0) sections[currentSectionIdx].name ?: "" else ""

    fun getTotalSessionTime(): Int = totalSessionTime

    fun getCurrentSessionTime(): Int = MeditationTimer.getCurrentSessionTime(getCurrentStartSeconds(), getSectionElapsedSeconds())

    fun isPaused(): Boolean = paused

    fun isStopped(): Boolean = stopping

    fun getCurrentStartSeconds(): Int = MeditationTimer.getCurrentStartSeconds(sections, currentSectionIdx)

    fun getNextEndSeconds(): Int = MeditationTimer.getNextEndSeconds(sections, currentSectionIdx)

    fun getNextStartSeconds(): Int = MeditationTimer.getNextStartSeconds(sections, currentSectionIdx)

    fun getPrevStartSeconds(): Int = MeditationTimer.getPrevStartSeconds(sections, currentSectionIdx)

    fun getNextSectionName(): String = if (currentSectionIdx < sections.size - 1) sections[currentSectionIdx + 1].name ?: "" else ""

    fun getNextNextSectionName(): String = if (currentSectionIdx < sections.size - 2) sections[currentSectionIdx + 2].name ?: "" else ""

    fun getSectionElapsedSeconds(): Int {
        val raw: Int =
            if (paused) {
                pauseSectionSeconds
            } else {
                Math.round(((System.currentTimeMillis() / 1000) - (sectionStartTime / 1000)).toFloat()) + pauseSectionSeconds
            }
        return MeditationTimer.getSectionElapsedSeconds(raw, getCurrentSection().duration)
    }

    private fun releaseAudioObjects() {
        val it = audioObjects.iterator()
        while (it.hasNext()) {
            it.next().release()
        }
        audioObjects.clear()
    }

    fun isPlaying(): Boolean {
        val it = audioObjects.iterator()
        while (it.hasNext()) {
            if (it.next().isPlaying()) {
                return true
            }
        }
        return false
    }

    fun playBell(section: Section) {
        var bellForSection: Bell? = BellCollection.getBellForSection(section)
        if (bellForSection == null) {
            bellForSection = BellCollection.getDemoBell()
        }
        var found = false
        val it = audioObjects.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (!next.isPlaying()) {
                Log.d(TAG, "Found free Audio Object")
                found = true
                next.playAbsVolume(bellForSection, section.volume)
                break
            }
        }
        if (found) {
            return
        }
        Log.d(TAG, "Created new Audio Object for new bell")
        val audio = Audio(meditationService)
        audio.playAbsVolume(bellForSection, section.volume)
        audioObjects.add(audio)
    }

    private fun mutePhone() {
        Log.d(TAG, "muting Phone")
        val vibrateSound = pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false)
        val vibrate = pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE, false)
        val none = pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE, true)
        val audioManager = meditationService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!vibrateSound) {
            if (vibrate) {
                oldRingerMode = audioManager.ringerMode
                oldRingerVolume = audioManager.getStreamVolume(2)
                audioManager.ringerMode = 1
                mutedRingerMode = audioManager.ringerMode
            } else if (none) {
                oldRingerMode = audioManager.ringerMode
                oldRingerVolume = audioManager.getStreamVolume(2)
                audioManager.ringerMode = 0
                kotlinx.coroutines.runBlocking { delay(500) }
                audioManager.setStreamVolume(2, 0, 0)
                audioManager.ringerMode = 0
                mutedRingerMode = audioManager.ringerMode
            }
        }
    }

    private fun unmutePhone() {
        Log.d(TAG, "unmuting Phone")
        val vibrateSound = pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE_SOUND, false)
        val vibrate = pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_VIBRATE, false)
        val none = pref.getBoolean(ZazenTimerActivity.PREF_KEY_MUTE_MODE_NONE, true)
        val audioManager = meditationService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!vibrateSound && (vibrate || none)) {
            if (mutedRingerMode != -1) {
                val currentMode = audioManager.ringerMode
                if (currentMode != mutedRingerMode) {
                    Log.d(TAG, "Ringer mode changed during meditation (was $mutedRingerMode, now $currentMode) — skipping restore")
                    mutedRingerMode = -1
                    return
                }
            }
            Log.d(TAG, "unmuting: ring=$oldRingerVolume ringerMode=$oldRingerMode")
            audioManager.ringerMode = oldRingerMode
            kotlinx.coroutines.runBlocking { delay(500) }
            if (oldRingerMode == 2) {
                audioManager.setStreamVolume(2, oldRingerVolume, 0)
            }
            audioManager.ringerMode = oldRingerMode
            mutedRingerMode = -1
        }
    }

    private fun playBells(
        section: Section,
        onDone: Runnable?,
    ) {
        scope.launch {
            Log.d(TAG, "Playing bells in coroutine")
            var wakeLock: PowerManager.WakeLock? = null
            val powerManager = meditationService.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(26, "PlayBells")
            wakeLock.acquire((section.bellcount * 25 * 1000).toLong())
            Log.d(TAG, "WakeLock created for playing bells")
            for (i in 0 until section.bellcount) {
                if (isStopped()) break
                playBell(section)
                if (i < section.bellcount - 1) {
                    for (j in 0 until section.bellpause * 2) {
                        if (isStopped()) break
                        delay(500)
                    }
                }
            }
            Log.d(TAG, "waiting until the bells have finished playing")
            while (isPlaying() && !isStopped()) {
                delay(500)
            }
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                Log.d(TAG, "wakeLock release error", e)
            }
            Log.d(TAG, "WakeLock released for playing bells")
            Log.d(TAG, "Done playing bells")
            onDone?.run()
        }
    }

    companion object {
        private const val INTENT_SECTION_ENDED = "ZAZENTIMER_SECTION_ENDED"
        private const val TAG = "ZMT_Meditation"
    }
}
