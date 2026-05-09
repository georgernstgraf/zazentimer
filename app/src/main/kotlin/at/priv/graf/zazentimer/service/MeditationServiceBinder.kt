package at.priv.graf.zazentimer.service

import android.os.Binder

class MeditationServiceBinder(
    private val service: MeditationService,
) : Binder() {
    fun getService(): MeditationService = service
}
