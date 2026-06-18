package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.bo.Session
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class SessionRankAssignerTest {
    private lateinit var dbOps: DbOperations

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        val owner = DatabaseOwner(context)
        dbOps =
            DbOperations(
                owner,
                SessionRepository(owner, context),
                SectionRepository(owner, context),
                BellRepository(owner, context),
                BellSanitizer(owner, context),
                context,
            )
    }

    @After
    fun tearDown() {
        dbOps.close()
    }

    @Test
    fun assignRanks_multipleSessions_setsSequentialRanks() {
        runBlocking {
            val a = Session("A", "Desc")
            val b = Session("B", "Desc")
            val c = Session("C", "Desc")
            dbOps.insertSession(a)
            dbOps.insertSession(b)
            dbOps.insertSession(c)

            dbOps.assignRanks(listOf(a, b, c))

            val sessions = dbOps.readSessions()
            assertThat(sessions).hasLength(3)
            assertThat(sessions[0].name).isEqualTo("A")
            assertThat(sessions[0].rank).isEqualTo(0)
            assertThat(sessions[1].name).isEqualTo("B")
            assertThat(sessions[1].rank).isEqualTo(1)
            assertThat(sessions[2].name).isEqualTo("C")
            assertThat(sessions[2].rank).isEqualTo(2)
        }
    }

    @Test
    fun assignRanks_emptyList_isNoOp() {
        runBlocking {
            dbOps.assignRanks(emptyList())
            val sessions = dbOps.readSessions()
            assertThat(sessions).isEmpty()
        }
    }

    @Test
    fun assignRanks_singleSession_setsRankToZero() {
        runBlocking {
            val a = Session("A", "Desc")
            dbOps.insertSession(a)

            dbOps.assignRanks(listOf(a))

            val sessions = dbOps.readSessions()
            assertThat(sessions).hasLength(1)
            assertThat(sessions[0].rank).isEqualTo(0)
        }
    }
}
