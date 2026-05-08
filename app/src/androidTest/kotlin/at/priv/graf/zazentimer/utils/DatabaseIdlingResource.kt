package at.priv.graf.zazentimer.utils

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class DatabaseIdlingResource(private val dbOperations: at.priv.graf.zazentimer.database.DbOperations) : IdlingResource {

    @Volatile
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private val isIdle = AtomicBoolean(true)

    override fun getName(): String = "DatabaseIdlingResource"

    override fun isIdleNow(): Boolean = isIdle.get()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        resourceCallback = callback
        notifyIdle()
    }

    fun setBusy() {
        isIdle.set(false)
    }

    fun setIdle() {
        isIdle.set(true)
        notifyIdle()
    }

    private fun notifyIdle() {
        resourceCallback?.onTransitionToIdle()
    }

    fun <T> wrap(query: RunnableQuery<T>): T {
        setBusy()
        try {
            return query.run()
        } finally {
            setIdle()
        }
    }

    fun wrap(query: RunnableVoidQuery) {
        setBusy()
        try {
            query.run()
        } finally {
            setIdle()
        }
    }

    interface RunnableQuery<T> {
        fun run(): T
    }

    interface RunnableVoidQuery {
        fun run()
    }
}
