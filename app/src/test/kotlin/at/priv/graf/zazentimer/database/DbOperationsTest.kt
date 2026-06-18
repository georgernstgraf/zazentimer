package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
import at.priv.graf.zazentimer.database.BellEntity
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
    private lateinit var owner: DatabaseOwner
    private lateinit var sessionRepo: SessionRepository
    private lateinit var sectionRepo: SectionRepository
    private lateinit var bellRepo: BellRepository
    private var bellId: Int = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        owner = DatabaseOwner(context)
        sessionRepo = SessionRepository(owner, context)
        sectionRepo = SectionRepository(owner, context)
        bellRepo = BellRepository(owner, context)
        runBlocking {
            bellId =
                bellRepo
                    .insertBell(
                        BellEntity(
                            name = TestBellHelper.TEST_BELL_NAME,
                            uri = TestBellHelper.TEST_BELL_URI,
                            isBuiltin = true,
                        ),
                    ).toInt()
        }
    }

    @After
    fun tearDown() {
        owner.close()
    }

    @Test
    fun insertSessionAndReadSession() {
        runBlocking {
            val session = Session("Test Session", "Description")
            sessionRepo.insertSession(session)

            val result = sessionRepo.readSession(session.id)
            assertThat(result).isNotNull()
            assertThat(result!!.name).isEqualTo("Test Session")
            assertThat(result.description).isEqualTo("Description")
        }
    }

    @Test
    fun readSessions() {
        runBlocking {
            sessionRepo.insertSession(Session("Session A", "Desc A"))
            sessionRepo.insertSession(Session("Session B", "Desc B"))

            val sessions = sessionRepo.readSessions()
            assertThat(sessions).hasLength(2)
            assertThat(sessions.map { it.name }).containsExactly("Session A", "Session B")
        }
    }

    @Test
    fun readSessions_orderedByRank() {
        runBlocking {
            sessionRepo.insertSession(Session("Banana", "Desc"))
            sessionRepo.insertSession(Session("apple", "Desc"))
            sessionRepo.insertSession(Session("Cherry", "Desc"))

            val sessions = sessionRepo.readSessions()
            assertThat(sessions).hasLength(3)
            assertThat(sessions[0].name).isEqualTo("Banana")
            assertThat(sessions[0].rank).isEqualTo(1)
            assertThat(sessions[1].name).isEqualTo("apple")
            assertThat(sessions[1].rank).isEqualTo(2)
            assertThat(sessions[2].name).isEqualTo("Cherry")
            assertThat(sessions[2].rank).isEqualTo(3)
        }
    }

    @Test
    fun updateSession() {
        runBlocking {
            val session = Session("Old Name", "Desc")
            sessionRepo.insertSession(session)

            session.name = "New Name"
            sessionRepo.updateSession(session)

            val result = sessionRepo.readSession(session.id)
            assertThat(result!!.name).isEqualTo("New Name")
        }
    }

    @Test
    fun deleteSession() {
        runBlocking {
            val session = Session("Delete Me", "Desc")
            sessionRepo.insertSession(session)

            sessionRepo.deleteSession(session.id)

            assertThat(sessionRepo.readSession(session.id)).isNull()
        }
    }

    @Test
    fun deleteSession_cascadesSections() {
        runBlocking {
            val session = Session("Session", "Desc")
            sessionRepo.insertSession(session)
            val s1 = Section("Section 1", 60)
            s1.bellId = bellId
            sectionRepo.insertSection(session, s1)
            val s2 = Section("Section 2", 120)
            s2.bellId = bellId
            sectionRepo.insertSection(session, s2)

            sessionRepo.deleteSession(session.id)

            assertThat(sectionRepo.readSections(session.id)).isEmpty()
        }
    }

    @Test
    fun insertSectionAndReadSection() {
        runBlocking {
            val session = Session("Session", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = bellId
            sectionRepo.insertSection(session, section)

            val result = sectionRepo.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.name).isEqualTo("Zazen")
            assertThat(result.duration).isEqualTo(300)
            assertThat(result.bellId).isEqualTo(bellId)
            assertThat(result.fkSession).isEqualTo(session.id)
        }
    }

    @Test
    fun insertSection_autoRankAssignment() {
        runBlocking {
            val session = Session("Session", "Desc")
            sessionRepo.insertSession(session)
            val s1 = Section("First", 60)
            s1.bellId = bellId
            sectionRepo.insertSection(session, s1)
            val s2 = Section("Second", 120)
            s2.bellId = bellId
            sectionRepo.insertSection(session, s2)

            assertThat(s1.rank).isEqualTo(1)
            assertThat(s2.rank).isEqualTo(2)
        }
    }

    @Test
    fun readSections_orderedByRank() {
        runBlocking {
            val session = Session("Session", "Desc")
            sessionRepo.insertSession(session)
            val s1 = Section("C Section", 180)
            s1.rank = 3
            s1.bellId = bellId
            sectionRepo.insertSection(session, s1)
            val s2 = Section("A Section", 60)
            s2.rank = 1
            s2.bellId = bellId
            sectionRepo.insertSection(session, s2)
            val s3 = Section("B Section", 120)
            s3.rank = 2
            s3.bellId = bellId
            sectionRepo.insertSection(session, s3)

            val sections = sectionRepo.readSections(session.id)
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
            sessionRepo.insertSession(session)
            val section = Section("Old Name", 60)
            section.bellId = bellId
            sectionRepo.insertSection(session, section)

            section.name = "New Name"
            section.duration = 120
            sectionRepo.updateSection(section)

            val result = sectionRepo.readSection(section.id)
            assertThat(result!!.name).isEqualTo("New Name")
            assertThat(result.duration).isEqualTo(120)
        }
    }

    @Test
    fun deleteSection() {
        runBlocking {
            val session = Session("Session", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Delete Me", 60)
            section.bellId = bellId
            sectionRepo.insertSection(session, section)

            sectionRepo.deleteSection(section.id.toLong())

            assertThat(sectionRepo.readSection(section.id)).isNull()
        }
    }

    @Test
    fun switchSessionPositions() {
        runBlocking {
            val sessionA = Session("Session A", "Desc A")
            sessionRepo.insertSession(sessionA)
            val sessionB = Session("Session B", "Desc B")
            sessionRepo.insertSession(sessionB)

            sessionRepo.switchSessionPositions(sessionA.id.toLong(), sessionB.id.toLong())

            val sessions = sessionRepo.readSessions()
            assertThat(sessions[0].name).isEqualTo("Session B")
            assertThat(sessions[1].name).isEqualTo("Session A")
        }
    }

    @Test
    fun readSessionNotFound() {
        runBlocking {
            assertThat(sessionRepo.readSession(999)).isNull()
        }
    }

    @Test
    fun readSectionNotFound() {
        runBlocking {
            assertThat(sectionRepo.readSection(999)).isNull()
        }
    }

    // --- Bell CRUD tests ---

    @Test
    fun insertBell_returnsPositiveId() =
        runBlocking {
            val id = bellRepo.insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
            assertThat(id).isGreaterThan(0)
        }

    @Test
    fun getNonBuiltinBells_returnsOnlyNonBuiltin() =
        runBlocking {
            bellRepo.insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
            val bells = bellRepo.getNonBuiltinBells()
            assertThat(bells).hasSize(1)
            assertThat(bells[0].isBuiltin).isFalse()
            assertThat(bells[0].name).isEqualTo("Custom")
        }

    @Test
    fun getBuiltinBells_returnsOnlyBuiltin() =
        runBlocking {
            val bells = bellRepo.getBuiltinBells()
            assertThat(bells).hasSize(1)
            assertThat(bells[0].isBuiltin).isTrue()
            assertThat(bells[0].name).isEqualTo(TestBellHelper.TEST_BELL_NAME)
        }

    @Test
    fun getAllBells_returnsBothBuiltinAndCustom() =
        runBlocking {
            bellRepo.insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
            assertThat(bellRepo.getAllBells()).hasSize(2)
        }

    @Test
    fun getBellById_returnsCorrectBell() =
        runBlocking {
            val id =
                bellRepo
                    .insertBell(BellEntity(name = "FindMe", uri = "file://find.mp3", isBuiltin = false))
                    .toInt()
            val bell = bellRepo.getBellById(id)
            assertThat(bell).isNotNull()
            assertThat(bell!!.name).isEqualTo("FindMe")
        }

    @Test
    fun getBellById_notFound_returnsNull() =
        runBlocking {
            assertThat(bellRepo.getBellById(999)).isNull()
        }

    @Test
    fun getBellByUri_returnsCorrectBell() =
        runBlocking {
            bellRepo.insertBell(BellEntity(name = "ByUri", uri = "file://byuri.mp3", isBuiltin = false))
            assertThat(bellRepo.getBellByUri("file://byuri.mp3")).isNotNull()
        }

    @Test
    fun getBellByUri_notFound_returnsNull() =
        runBlocking {
            assertThat(bellRepo.getBellByUri("file://nonexistent.mp3")).isNull()
        }

    @Test
    fun deleteBellById_removesBell() =
        runBlocking {
            val id =
                bellRepo
                    .insertBell(BellEntity(name = "DeleteMe", uri = "file://delete.mp3", isBuiltin = false))
                    .toInt()
            bellRepo.deleteBellById(id)
            assertThat(bellRepo.getBellById(id)).isNull()
        }

    @Test
    fun updateBell_updatesFields() =
        runBlocking {
            val id =
                bellRepo
                    .insertBell(BellEntity(name = "Old", uri = "file://old.mp3", isBuiltin = false))
                    .toInt()
            bellRepo.updateBell(BellEntity(id = id, name = "New", uri = "file://new.mp3", isBuiltin = false))
            val bell = bellRepo.getBellById(id)
            assertThat(bell!!.name).isEqualTo("New")
            assertThat(bell.uri).isEqualTo("file://new.mp3")
            assertThat(bell.isBuiltin).isFalse()
        }

    // --- deleteCustomBell tests ---

    @Test
    fun deleteCustomBell_unused_deletesRow() {
        runBlocking {
            val id =
                bellRepo
                    .insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
                    .toInt()
            bellRepo.deleteCustomBell(id)
            assertThat(bellRepo.getBellById(id)).isNull()
        }
    }

    @Test
    fun deleteCustomBell_reassignsSections() =
        runBlocking {
            val customId =
                bellRepo
                    .insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
                    .toInt()
            val session = Session("Test", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = customId
            sectionRepo.insertSection(session, section)

            bellRepo.deleteCustomBell(customId)

            val updated = sectionRepo.readSection(section.id)
            assertThat(updated).isNotNull()
            assertThat(updated!!.bellId).isNotEqualTo(customId)
            assertThat(updated.bellId).isEqualTo(bellId)
        }

    @Test
    fun deleteCustomBell_removesBellVolumes() =
        runBlocking {
            val customId =
                bellRepo
                    .insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
                    .toInt()
            val session = Session("Volumes", "Desc")
            sessionRepo.insertSession(session)
            sessionRepo.saveBellVolumes(session.id, listOf(SessionBellVolume(bellId = customId, volume = 80)))

            bellRepo.deleteCustomBell(customId)

            assertThat(sessionRepo.readBellVolumes(session.id)).isEmpty()
        }

    @Test
    fun deleteCustomBell_noBuiltinBells_throws() =
        runBlocking {
            bellRepo.getAllBells().forEach { bellRepo.deleteBellById(it.id) }

            val customId =
                bellRepo
                    .insertBell(BellEntity(name = "Custom", uri = "file://test.mp3", isBuiltin = false))
                    .toInt()
            var threw = false
            try {
                bellRepo.deleteCustomBell(customId)
            } catch (_: IllegalStateException) {
                threw = true
            }
            assert(threw)
        }
}
