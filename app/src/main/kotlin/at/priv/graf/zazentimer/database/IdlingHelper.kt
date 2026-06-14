package at.priv.graf.zazentimer.database

import at.priv.graf.zazentimer.service.IdlingResourceManager

internal suspend fun <T> withIdling(block: suspend () -> T): T {
    IdlingResourceManager.increment()
    return try {
        block()
    } finally {
        IdlingResourceManager.decrement()
    }
}
