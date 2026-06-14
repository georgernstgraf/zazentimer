package at.priv.graf.zazentimer.audio

import android.content.Context
import at.priv.graf.zazentimer.R

object BuiltinBells {
    data class BellSeed(
        val rawResId: Int,
        val nameResId: Int,
    )

    fun definitions(): List<BellSeed> =
        listOf(
            BellSeed(R.raw.bell1, R.string.bell_name_1),
            BellSeed(R.raw.bell2, R.string.bell_name_2),
            BellSeed(R.raw.dharma107, R.string.bell_name_3),
            BellSeed(R.raw.dharmaschwarz88, R.string.bell_name_4),
            BellSeed(R.raw.shomyo90, R.string.bell_name_5),
            BellSeed(R.raw.tang164, R.string.bell_name_6),
            BellSeed(R.raw.tib230, R.string.bell_name_7),
            BellSeed(R.raw.zen97, R.string.bell_name_8),
        )

    fun resourceUri(
        context: Context,
        rawResId: Int,
    ): String = "android.resource://${context.packageName}/$rawResId"

    val DEMO_BELL_RAW_RES: Int = R.raw.bell2

    val DEMO_BELL_NAME_RES: Int = R.string.bell_name_2
}
