package at.priv.graf.zazentimer.service

import androidx.test.espresso.idling.CountingIdlingResource

object IdlingResourceManager {
    private const val RESOURCE = "GLOBAL_DB_IDLING_RESOURCE"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}
