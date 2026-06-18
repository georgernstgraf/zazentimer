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
    private lateinit var dbOps: DbOperations
    private lateinit var context: Context
    private var demoBellId: Int = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
        runBlocking {
            val demoUri = BuiltinBells.resourceUri(context, BuiltinBells.DEMO_BELL_RAW_RES)
            val demoName = context.getString(BuiltinBells.DEMO_BELL_NAME_RES)
            demoBellId =
                dbOps
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
        dbOps.close()
    }

    @Test
    fun insertSection_bellIdZero_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Test", "Desc")
            dbOps.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = 0
            dbOps.insertSection(session, section)

            val result = dbOps.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun insertSection_bellIdNegative_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Test", "Desc")
            dbOps.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = -1
            dbOps.insertSection(session, section)

            val result = dbOps.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun insertSection_bellIdDeleted_resolvesToDemoBell() {
        runBlocking {
            val customId =
                dbOps
                    .insertBell(
                        BellEntity(name = "Custom", uri = "file://custom.mp3", isBuiltin = false),
                    ).toInt()
            dbOps.deleteBellById(customId)

            val session = Session("Test", "Desc")
            dbOps.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = customId
            dbOps.insertSection(session, section)

            val result = dbOps.readSection(section.id)
            assertThat(result).isNotNull()
            assertThat(result!!.bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun duplicateSession_sectionWithInvalidBellId_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Source", "Desc")
            dbOps.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = 0
            dbOps.insertSection(session, section)

            val newId = dbOps.duplicateSession(session.id, "Copy of Source")
            assertThat(newId).isGreaterThan(0)

            val copiedSections = dbOps.readSections(newId)
            assertThat(copiedSections).hasLength(1)
            assertThat(copiedSections[0].bellId).isEqualTo(demoBellId)
        }
    }

    @Test
    fun duplicateSession_bellVolumesWithInvalidBellId_resolvesToDemoBell() {
        runBlocking {
            val session = Session("Source", "Desc")
            dbOps.insertSession(session)
            val section = Section("Zazen", 300)
            section.bellId = demoBellId
            dbOps.insertSection(session, section)
            dbOps.saveBellVolumes(
                session.id,
                listOf(
                    at.priv.graf.zazentimer.bo
                        .SessionBellVolume(bellId = demoBellId, volume = 80),
                ),
            )

            val newId = dbOps.duplicateSession(session.id, "Copy of Source")
            assertThat(newId).isGreaterThan(0)

            val copiedVolumes = dbOps.readBellVolumes(newId)
            assertThat(copiedVolumes).hasSize(1)
            assertThat(copiedVolumes[0].bellId).isEqualTo(demoBellId)
        }
    }
}
