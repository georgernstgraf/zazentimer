package at.priv.graf.zazentimer.database

import android.content.res.Resources
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DemoSessionCreator(
    private val dbOperations: DbOperations,
    private val resources: Resources,
) {
    suspend fun createDemoSessions() {
        withContext(Dispatchers.IO) {
            createDemoSession1()
            createDemoSession2()
        }
    }

    private suspend fun createDemoSession1() {
        val session = Session()
        session.description = resources.getString(R.string.demo_sess1_description)
        session.name = resources.getString(R.string.demo_sess1_name)
        dbOperations.insertSession(session)
        createSection(
            session,
            R.string.demo_sess1_sec1_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_88,
            BELL_COUNT_1,
            RANK_1,
            DURATION_30,
        )
        createSection(
            session,
            R.string.demo_sess1_sec2_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_107,
            BELL_COUNT_2,
            RANK_2,
            DURATION_900,
        )
        createSection(
            session,
            R.string.demo_sess1_sec3_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_88,
            BELL_COUNT_2,
            RANK_3,
            DURATION_300,
        )
        createSection(
            session,
            R.string.demo_sess1_sec4_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_107,
            BELL_COUNT_2,
            RANK_4,
            DURATION_900,
        )
        createSection(
            session,
            R.string.demo_sess1_sec5_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_88,
            BELL_COUNT_2,
            RANK_5,
            DURATION_300,
        )
        createSection(
            session,
            R.string.demo_sess1_sec6_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_107,
            BELL_COUNT_2,
            RANK_6,
            DURATION_900,
        )
    }

    private suspend fun createDemoSession2() {
        val session = Session()
        session.description = resources.getString(R.string.demo_sess2_description)
        session.name = resources.getString(R.string.demo_sess2_name)
        dbOperations.insertSession(session)
        createSection(
            session,
            R.string.demo_sess1_sec1_name,
            BellCollection.BELL_IDX_TIB_RHINBOWL_230,
            BELL_COUNT_1,
            RANK_1,
            DURATION_5,
        )
        createSection(
            session,
            R.string.demo_sess1_sec2_name,
            BellCollection.BELL_IDX_JAP_RHINBOWL_107,
            BELL_COUNT_2,
            RANK_2,
            DURATION_600,
        )
    }

    @Suppress("LongParameterList")
    private suspend fun createSection(
        session: Session,
        nameResId: Int,
        bellIdx: Int,
        bellCount: Int,
        rank: Int,
        duration: Int,
    ) {
        val section = Section()
        section.bell = BELL_INDEX_NONE
        section.bellUri = BellCollection.getBell(bellIdx)?.uri?.toString() ?: return
        section.bellcount = bellCount
        section.bellpause = if (bellCount == 1) BELL_PAUSE_1 else BELL_PAUSE_3
        section.duration = duration
        section.name = resources.getString(nameResId)
        section.rank = rank
        dbOperations.insertSection(session, section)
    }

    companion object {
        const val BELL_COUNT_1 = 1
        const val BELL_COUNT_2 = 2
        const val BELL_PAUSE_1 = 1
        const val BELL_PAUSE_3 = 3
        const val BELL_INDEX_NONE = -2
        const val DURATION_5 = 5
        const val DURATION_30 = 30
        const val DURATION_300 = 300
        const val DURATION_600 = 600
        const val DURATION_900 = 900
        const val RANK_1 = 1
        const val RANK_2 = 2
        const val RANK_3 = 3
        const val RANK_4 = 4
        const val RANK_5 = 5
        const val RANK_6 = 6
    }
}
