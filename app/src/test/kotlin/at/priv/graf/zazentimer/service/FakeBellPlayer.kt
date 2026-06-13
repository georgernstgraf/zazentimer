package at.priv.graf.zazentimer.service

import at.priv.graf.zazentimer.bo.Section

class FakeBellPlayer : BellPlayer {
    data class BellCall(
        val section: Section,
        val volume: Int,
    )

    val playedBells = mutableListOf<BellCall>()
    var released = false

    override fun playBells(
        section: Section,
        volume: Int,
        stoppingCheck: () -> Boolean,
        onDone: Runnable?,
    ) {
        playedBells.add(BellCall(section, volume))
        onDone?.run()
    }

    override suspend fun release() {
        released = true
    }
}
