package at.priv.graf.zazentimer.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.Nullable
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.Section
import java.io.File
import java.io.FilenameFilter

object BellCollection {
    const val BELL_IDX_HIGH_TONE: Int = 0
    const val BELL_IDX_JAP_RHINBOWL_107: Int = 2
    const val BELL_IDX_JAP_RHINBOWL_164: Int = 5
    const val BELL_IDX_JAP_RHINBOWL_88: Int = 3
    const val BELL_IDX_JAP_RHINBOWL_90: Int = 4
    const val BELL_IDX_JAP_RHINBOWL_97: Int = 7
    const val BELL_IDX_LOW_TONE: Int = 1
    const val BELL_IDX_TIB_RHINBOWL_230: Int = 6

    private const val TAG = "ZMT_BellCollection"

    private var bells: ArrayList<Bell> = ArrayList()
    private var demoBellName: String? = null

    @JvmStatic
    fun initialize(context: Context) {
        this.demoBellName = context.resources.getString(R.string.bell_name_2)
        this.bells.clear()
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.bell1), context.resources.getString(R.string.bell_name_1)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.bell2), context.resources.getString(R.string.bell_name_2)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.dharma107), context.resources.getString(R.string.bell_name_3)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.dharmaschwarz88), context.resources.getString(R.string.bell_name_4)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.shomyo90), context.resources.getString(R.string.bell_name_5)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.tang164), context.resources.getString(R.string.bell_name_6)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.tib230), context.resources.getString(R.string.bell_name_7)))
        this.bells.add(Bell(getPredefinedBellUri(context, R.raw.zen97), context.resources.getString(R.string.bell_name_8)))
        val listFiles = context.filesDir.listFiles(FilenameFilter { _: File, str: String -> str.startsWith("bell_") })
        if (listFiles != null) {
            for (file in listFiles) {
                this.bells.add(Bell(getCustomBellUri(context, file.name), file.name))
            }
        }
    }

    private fun getPredefinedBellUri(
        context: Context,
        i: Int,
    ): Uri = Uri.parse("android.resource://${context.packageName}/$i")

    private fun getCustomBellUri(
        context: Context,
        str: String,
    ): Uri = Uri.parse("file://${context.filesDir}/$str")

    @Nullable
    @JvmStatic
    fun getBell(str: String): Bell? {
        for (bell in this.bells) {
            if (bell.getName() == str) {
                return bell
            }
        }
        return null
    }

    @JvmStatic
    fun getBellList(): ArrayList<Bell> = this.bells

    @JvmStatic
    fun getDemoBell(): Bell? = demoBellName?.let { getBell(it) }

    @JvmStatic
    fun release() {
        this.bells.clear()
        this.demoBellName = null
    }

    @JvmStatic
    fun getBell(i: Int): Bell? {
        if (this.bells.size <= i || i < 0) {
            return null
        }
        return this.bells[i]
    }

    @JvmStatic
    fun getBellForSection(section: Section): Bell? {
        val str = section.bellUri
        for (bell in this.bells) {
            if (bell.uri.toString() == str) {
                return bell
            }
        }
        return null
    }

    @JvmStatic
    fun getUriForName(str: String): Uri? {
        for (bell in this.bells) {
            if (bell.uri.toString().endsWith(str)) {
                return bell.uri
            }
        }
        return null
    }
}
