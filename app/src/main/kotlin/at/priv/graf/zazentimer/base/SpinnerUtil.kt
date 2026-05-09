package at.priv.graf.zazentimer.base

import at.priv.graf.zazentimer.bo.Session

object SpinnerUtil {
    @JvmStatic
    fun getPositionById(
        list: ArrayList<Session>,
        id: Int,
    ): Int {
        for (i in list.indices) {
            if (list[i].id == id) {
                return i
            }
        }
        return -1
    }
}
