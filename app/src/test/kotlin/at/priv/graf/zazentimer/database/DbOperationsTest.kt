package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.bo.Section
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
class DbOperationsTest {
    private lateinit var dbOps: DbOperations

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        dbOps = DbOperations(context)
    }

    @After
    fun tearDown() {
        dbOps.close()
    }

    @Test
    fun insertSessionAndReadSession() {
        runBlocking {
            val session = Session("Test Session", "Description")
            dbOps.insertSession(session)

            val result = dbOps.readSession(session.id)
            assertThat(result).isNotNull()
            assertThat(result!!.name).isEqualTo("Test Session")
            assertThat(result.description).isEqualTo("Description")
        }
    }

    @Test
    fun readSessions() {
        runBlocking {
            dbOps.insertSession(Session("Session A", "Desc A"))
            dbOps.insertSession(Session("Session B", "Desc B"))

            val sessions = dbOps.readSessions()
            assertThat(sessions).hasLength(2)
            assertThat(sessions.map { it.name }).containsExactly("Session A", "Session B")
        }
    }

    @Test
    fun readSessions_orderedByNameCaseInsensitive() {
        runBlocking {
            dbOps.insertSession(Session("Banana", "Desc"))
            dbOps.insertSession(Session("apple", "Desc"))
            dbOps.insertSession(Session("Cherry", "Desc"))

            val sessions = dbOps.readSessions()
            assertThat(sessions).hasLength(3)
            assertThat(sessions[0].name).isEqualTo("apple")
            assertThat(sessions[1].name).isEqualTo("Banana")
            assertThat(sessions[2].name).isEqualTo("Cherry")
        }
    }

    @Test
    fun updateSession() {
        runBlocking {
            val session = Session("Old Name", "Desc")
            dbOps.insertSession(session)

            session.name = "New Name"
            dbOps.updateSession(session)

            val result = dbOps.readSession(session.id)
            assertThat(result!!.name).isEqualTo("New Name")
        }
    }

    @Test
    fun deleteSession() {
        runBlocking {
            val session = Session("Delete Me", "Desc")
            dbOps.insertSession(session)

            dbOps.deleteSession(session.id)

            assertThat(dbOps.readSession(session.id)).isNull()
        }
    }

    @Test
    fun deleteSession_cascadesSections() {
        runBlocking {
            val session = Session("Session", "Desc")
            dbOps.insertSession(session)
            val s1 = Section("Section 1", 60)
            dbOps.insertSection(session, s1)
            val s2 = Section("Section 2", 120)
            dbOps.insertSection(session, s2)

            dbOps.deleteSession(session.id)

            assertThat(dbOps.readSections(session.id)).isEmpty()
        }
    }

    @Test
    fun insertSectionAndReadSection() {
        runBlocking {
            val session = Session("Session", "Desc")
            dbOps.insertSession(session)
            val section = Section("Zazen", 300)
            section.bell = 1
            section.volume = 80
            dbOps.insertSection(session, section)

            val result = dbOps.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.name).isEqualTo("Zazen")
            assertThat(result.duration).isEqualTo(300)
            assertThat(result.bell).isEqualTo(1)
            assertThat(result.volume).isEqualTo(80)
            assertThat(result.fkSession).isEqualTo(session.id)
        }
    }

    @Test
    fun insertSection_autoRankAssignment() {
        runBlocking {
            val session = Session("Session", "Desc")
            dbOps.insertSession(session)
            val s1 = Section("First", 60)
            dbOps.insertSection(session, s1)
            val s2 = Section("Second", 120)
            dbOps.insertSection(session, s2)

            assertThat(s1.rank).isEqualTo(1)
            assertThat(s2.rank).isEqualTo(2)
        }
    }

    @Test
    fun readSections_orderedByRank() {
        runBlocking {
            val session = Session("Session", "Desc")
            dbOps.insertSession(session)
            val s1 = Section("C Section", 180)
            s1.rank = 3
            dbOps.insertSection(session, s1)
            val s2 = Section("A Section", 60)
            s2.rank = 1
            dbOps.insertSection(session, s2)
            val s3 = Section("B Section", 120)
            s3.rank = 2
            dbOps.insertSection(session, s3)

            val sections = dbOps.readSections(session.id)
            assertThat(sections).hasLength(3)
            assertThat(sections[0].name).isEqualTo("A Section")
            assertThat(sections[1].name).isEqualTo("B Section")
            assertThat(sections[2].name).isEqualTo("C Section")
        }
    }

    @Test
    fun updateSection() {
        runBlocking {
            val session = Session("Session", "Desc")
            dbOps.insertSession(session)
            val section = Section("Old Name", 60)
            dbOps.insertSection(session, section)

            section.name = "New Name"
            section.duration = 120
            dbOps.updateSection(section)

            val result = dbOps.readSection(section.id)
            assertThat(result!!.name).isEqualTo("New Name")
            assertThat(result.duration).isEqualTo(120)
        }
    }

    @Test
    fun deleteSection() {
        runBlocking {
            val session = Session("Session", "Desc")
            dbOps.insertSession(session)
            val section = Section("Delete Me", 60)
            dbOps.insertSection(session, section)

            dbOps.deleteSection(section.id.toLong())

            assertThat(dbOps.readSection(section.id)).isNull()
        }
    }

    @Test
    fun readSessionNotFound() {
        runBlocking {
            assertThat(dbOps.readSession(999)).isNull()
        }
    }

    @Test
    fun readSectionNotFound() {
        runBlocking {
            assertThat(dbOps.readSection(999)).isNull()
        }
    }
}
