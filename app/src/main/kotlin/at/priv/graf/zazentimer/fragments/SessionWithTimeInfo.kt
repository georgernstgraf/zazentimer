package at.priv.graf.zazentimer.fragments

import at.priv.graf.zazentimer.bo.Session

data class SessionWithTimeInfo(
    val session: Session,
    val totalTimeSeconds: Int,
)
