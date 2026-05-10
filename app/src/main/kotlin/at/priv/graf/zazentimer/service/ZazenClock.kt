package at.priv.graf.zazentimer.service

interface ZazenClock {
    fun now(): Long

    fun elapsedRealtime(): Long
}

class SystemClock : ZazenClock {
    override fun now(): Long = System.currentTimeMillis()

    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
}
