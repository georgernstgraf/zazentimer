package at.priv.graf.zazentimer.utils

import androidx.test.espresso.IdlingResource

class MeditationServiceIdlingResource : IdlingResource {

    @Volatile
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    @Volatile
    private var isIdle = true

    override fun getName(): String = "MeditationServiceIdlingResource"

    override fun isIdleNow(): Boolean = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        resourceCallback = callback
        callback.onTransitionToIdle()
    }

    fun setBusy() {
        isIdle = false
    }

    fun setIdle() {
        isIdle = true
        notifyIdle()
    }

    private fun notifyIdle() {
        resourceCallback?.onTransitionToIdle()
    }
}
