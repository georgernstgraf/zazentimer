package at.priv.graf.zazentimer.service

object MeditationServiceState {
    @Volatile
    private var running = false

    @JvmStatic
    fun isServiceRunning(): Boolean = running

    fun setRunning(value: Boolean) {
        running = value
    }
}
