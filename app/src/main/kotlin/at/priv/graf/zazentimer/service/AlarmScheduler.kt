package at.priv.graf.zazentimer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import at.priv.graf.zazentimer.bo.Section

class AlarmScheduler(
    private val meditationService: MeditationService,
    private val clock: ZazenClock,
) {
    private val alarmManager: AlarmManager =
        meditationService.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    var sectionStartTime: Long = 0L
        private set
    private var pendingIntent: PendingIntent? = null

    fun setAlarmForSectionEnd(
        section: Section,
        pauseSectionSeconds: Int,
    ) {
        sectionStartTime = clock.now()
        val triggerTime =
            sectionStartTime + ((section.duration - pauseSectionSeconds) * MS_PER_SECOND)
        val pi =
            PendingIntent.getBroadcast(
                meditationService,
                0,
                Intent(Meditation.INTENT_SECTION_ENDED)
                    .setClass(meditationService, SectionEndReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        pendingIntent = pi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms -- permission denied")
            }
        }
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pi)
        alarmManager.setAlarmClock(alarmClockInfo, pi)
        Log.d(TAG, "Started AlarmClock for next section: triggerTime=$triggerTime")
    }

    fun cancelAlarm() {
        pendingIntent?.let { alarmManager.cancel(it) }
        pendingIntent = null
    }

    companion object {
        private const val TAG = "ZMT_AlarmScheduler"
        private const val MS_PER_SECOND = 1000L
    }
}
