package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class DbOperationsDuplicateTest {
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
    fun deepCopy_newSessionWithProvidedName() {
        val session = Session("Original", "Original Desc")
        dbOps.insertSession(session)

        val newId = dbOps.duplicateSession(session.id, "Copy of Original")

        assertThat(newId).isNotEqualTo(session.id)
        val copy = dbOps.readSession(newId)
        assertThat(copy).isNotNull()
        assertThat(copy!!.name).isEqualTo("Copy of Original")
        assertThat(copy.description).isEqualTo("Original Desc")
    }

    @Test
    fun allSectionsDuplicatedWithNewIds() {
        val session = Session("Session", "Desc")
        dbOps.insertSession(session)
        val s1 = Section("Section 1", 60)
        s1.bell = 1
        s1.volume = 80
        dbOps.insertSection(session, s1)
        val s2 = Section("Section 2", 120)
        s2.bell = 2
        s2.volume = 90
        dbOps.insertSection(session, s2)

        val newId = dbOps.duplicateSession(session.id, "Copy")

        val originalSections = dbOps.readSections(session.id)
        val copiedSections = dbOps.readSections(newId)
        assertThat(copiedSections.size).isEqualTo(2)
        assertThat(copiedSections.size).isEqualTo(originalSections.size)
        assertThat(copiedSections[0].id).isNotEqualTo(originalSections[0].id)
        assertThat(copiedSections[0].name).isEqualTo("Section 1")
        assertThat(copiedSections[0].duration).isEqualTo(60)
        assertThat(copiedSections[0].bell).isEqualTo(1)
        assertThat(copiedSections[0].volume).isEqualTo(80)
        assertThat(copiedSections[0].fkSession).isEqualTo(newId)
        assertThat(copiedSections[1].name).isEqualTo("Section 2")
        assertThat(copiedSections[1].duration).isEqualTo(120)
        assertThat(copiedSections[1].bell).isEqualTo(2)
        assertThat(copiedSections[1].volume).isEqualTo(90)
        assertThat(copiedSections[1].fkSession).isEqualTo(newId)
    }

    @Test
    fun originalSessionUnchanged() {
        val session = Session("Original", "Original Desc")
        dbOps.insertSession(session)
        val s1 = Section("Section 1", 60)
        dbOps.insertSection(session, s1)
        val originalSessionId = session.id
        val originalSectionId = s1.id

        dbOps.duplicateSession(session.id, "Copy")

        val originalSession = dbOps.readSession(originalSessionId)
        assertThat(originalSession!!.name).isEqualTo("Original")
        assertThat(originalSession.description).isEqualTo("Original Desc")
        val sections = dbOps.readSections(originalSessionId)
        assertThat(sections).hasLength(1)
        assertThat(sections[0].id).isEqualTo(originalSectionId)
        assertThat(sections[0].name).isEqualTo("Section 1")
    }

    @Test
    fun duplicateOfDuplicate() {
        val session = Session("First", "Desc")
        dbOps.insertSession(session)
        val s1 = Section("Section A", 60)
        dbOps.insertSection(session, s1)

        val copy1Id = dbOps.duplicateSession(session.id, "Copy 1")
        val copy2Id = dbOps.duplicateSession(copy1Id, "Copy 2")

        assertThat(copy2Id).isNotEqualTo(copy1Id)
        assertThat(copy2Id).isNotEqualTo(session.id)
        val copy2 = dbOps.readSession(copy2Id)
        assertThat(copy2!!.name).isEqualTo("Copy 2")
        val copy2Sections = dbOps.readSections(copy2Id)
        assertThat(copy2Sections).hasLength(1)
        assertThat(copy2Sections[0].name).isEqualTo("Section A")
        assertThat(copy2Sections[0].duration).isEqualTo(60)
        assertThat(copy2Sections[0].fkSession).isEqualTo(copy2Id)
    }

    @Test
    fun duplicateSessionWithNoSections() {
        val session = Session("Empty Session", "No sections")
        dbOps.insertSession(session)

        val newId = dbOps.duplicateSession(session.id, "Copy of Empty")

        val copy = dbOps.readSession(newId)
        assertThat(copy).isNotNull()
        assertThat(copy!!.name).isEqualTo("Copy of Empty")
        assertThat(dbOps.readSections(newId)).isEmpty()
    }
}
