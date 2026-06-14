package at.priv.graf.zazentimer.bo

import at.priv.graf.zazentimer.Constants
import java.util.Locale

object TimeFormat {
    fun mmss(seconds: Int): String =
        String.format(
            Locale.US,
            "%02d:%02d",
            seconds / Constants.SECONDS_PER_MINUTE,
            seconds % Constants.SECONDS_PER_MINUTE,
        )

    fun mmss(
        minutes: Int,
        seconds: Int,
    ): String = String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
