package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.priv.graf.zazentimer.audio.BuiltinBells
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
class DbOperationsBellFallbackTest {
    private lateinit var owner: DatabaseOwner
    private lateinit var sessionRepo: SessionRepository
    private lateinit var sectionRepo: SectionRepository
    private lateinit var bellRepo: BellRepository
    private lateinit var context: Context
    private var demoBellId: Int = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        owner = DatabaseOwner(context)
        sessionRepo = SessionRepository(owner, context)
        sectionRepo = SectionRepository(owner, context)
        bellRepo = BellRepository(owner, context)
        runBlocking {
            val demoUri = BuiltinBells.resourceUri(context, BuiltinBells.DEMO_BELL_RAW_RES)
            val demoName = context.getString(BuiltinBells.DEMO_BELL_NAME_RES)
            demoBellId =
                bellRepo
                    .insertBell(
                        BellEntity(
                            name = demoName,
                            uri = demoUri,
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
    fun insertSection_bellIdZero_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Test", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = 0
            sectionRepo.insertSection(session, section)

            val result = sectionRepo.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun insertSection_bellIdNegative_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Test", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = -1
            sectionRepo.insertSection(session, section)

            val result = sectionRepo.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun insertSection_bellIdDeleted_resolvesToDemoBell() {
        runBlocking {
            val customId =
                bellRepo
                    .insertBell(
                        BellEntity(name = "Custom", uri = "file://custom.mp3", isBuiltin = false),
                    ).toInt()
            bellRepo.deleteBellById(customId)

            val session = Session("Test", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = customId
            sectionRepo.insertSection(session, section)

            val result = sectionRepo.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun duplicateSession_sectionWithInvalidBellId_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Source", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = 0
            sectionRepo.insertSection(session, section)

            val newId = sessionRepo.duplicateSession(session.id, "Copy of Source")
            assertThat(newId).isGreaterThan(0)

            val copiedSections = sectionRepo.readSections(newId)
            assertThat(copiedSections).hasLength(1)
            assertThat(copiedSections[0].bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun duplicateSession_bellVolumesWithInvalidBellId_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Source", "Desc")
            sessionRepo.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = demoBellId
            sectionRepo.insertSection(session, section)
            sessionRepo.saveBellVolumes(
                session.id,
                listOf(
                    at.priv.graf.zazentimer.bo
                        .SessionBellVolume(bellId = demoBellId, volume = 80),
                ),
            )

            val newId = sessionRepo.duplicateSession(session.id, "Copy of Source")
            assertThat(newId).isGreaterThan(0)

            val copiedVolumes = sessionRepo.readBellVolumes(newId)
            assertThat(copiedVolumes).hasSize(1)
            assertThat(copiedVolumes[0].bellId).isEqualTo(demoBellId)
        }
    }
}
