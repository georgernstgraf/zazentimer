package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
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
class DbOperationsDuplicateTest {
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
    fun deepCopy_newSessionWithProvidedName() {
        runBlocking {
            val session = Session("Original", "Original Desc")
            sessionRepo.insertSession(session)

            val newId = sessionRepo.duplicateSession(session.id, "Copy of Original")

            assertThat(newId).isNotEqualTo(session.id)
            val copy = sessionRepo.readSession(newId)
            assertThat(copy).isNotNull()
            assertThat(copy!!.name).isEqualTo("Copy of Original")
            assertThat(copy.description).isEqualTo("Original Desc")
        }
    }

    @Test
    fun allSectionsDuplicatedWithNewIds() {
        runBlocking {
            val session = Session("Session", "Desc")
            sessionRepo.insertSession(session)
            val s1 = Section("Section 1", 60)
            s1.bellId = bellId
            sectionRepo.insertSection(session, s1)
            val s2 = Section("Section 2", 120)
            s2.bellId = bellId
            sectionRepo.insertSection(session, s2)

            val newId = sessionRepo.duplicateSession(session.id, "Copy")

            val originalSections = sectionRepo.readSections(session.id)
            val copiedSections = sectionRepo.readSections(newId)
            assertThat(copiedSections.size).isEqualTo(2)
            assertThat(copiedSections.size).isEqualTo(originalSections.size)
            assertThat(copiedSections[0].id).isNotEqualTo(originalSections[0].id)
            assertThat(copiedSections[0].name).isEqualTo("Section 1")
            assertThat(copiedSections[0].duration).isEqualTo(60)
            assertThat(copiedSections[0].bellId).isEqualTo(bellId)
            assertThat(copiedSections[0].fkSession).isEqualTo(newId)
            assertThat(copiedSections[1].name).isEqualTo("Section 2")
            assertThat(copiedSections[1].duration).isEqualTo(120)
            assertThat(copiedSections[1].bellId).isEqualTo(bellId)
            assertThat(copiedSections[1].fkSession).isEqualTo(newId)
        }
    }

    @Test
    fun originalSessionUnchanged() {
        runBlocking {
            val session = Session("Original", "Original Desc")
            sessionRepo.insertSession(session)
            val s1 = Section("Section 1", 60)
            s1.bellId = bellId
            sectionRepo.insertSection(session, s1)
            val originalSessionId = session.id
            val originalSectionId = s1.id

            sessionRepo.duplicateSession(session.id, "Copy")

            val originalSession = sessionRepo.readSession(originalSessionId)
            assertThat(originalSession!!.name).isEqualTo("Original")
            assertThat(originalSession.description).isEqualTo("Original Desc")
            val sections = sectionRepo.readSections(originalSessionId)
            assertThat(sections).hasLength(1)
            assertThat(sections[0].id).isEqualTo(originalSectionId)
            assertThat(sections[0].name).isEqualTo("Section 1")
        }
    }

    @Test
    fun duplicateOfDuplicate() {
        runBlocking {
            val session = Session("First", "Desc")
            sessionRepo.insertSession(session)
            val s1 = Section("Section A", 60)
            s1.bellId = bellId
            sectionRepo.insertSection(session, s1)

            val copy1Id = sessionRepo.duplicateSession(session.id, "Copy 1")
            val copy2Id = sessionRepo.duplicateSession(copy1Id, "Copy 2")

            assertThat(copy2Id).isNotEqualTo(copy1Id)
            assertThat(copy2Id).isNotEqualTo(session.id)
            val copy2 = sessionRepo.readSession(copy2Id)
            assertThat(copy2!!.name).isEqualTo("Copy 2")
            val copy2Sections = sectionRepo.readSections(copy2Id)
            assertThat(copy2Sections).hasLength(1)
            assertThat(copy2Sections[0].name).isEqualTo("Section A")
            assertThat(copy2Sections[0].duration).isEqualTo(60)
            assertThat(copy2Sections[0].fkSession).isEqualTo(copy2Id)
        }
    }

    @Test
    fun duplicateSessionWithNoSections() {
        runBlocking {
            val session = Session("Empty Session", "No sections")
            sessionRepo.insertSession(session)

            val newId = sessionRepo.duplicateSession(session.id, "Copy of Empty")

            val copy = sessionRepo.readSession(newId)
            assertThat(copy).isNotNull()
            assertThat(copy!!.name).isEqualTo("Copy of Empty")
            assertThat(sectionRepo.readSections(newId)).isEmpty()
        }
    }
}
