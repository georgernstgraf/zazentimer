package at.priv.graf.zazentimer.database

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.audio.BuiltinBells
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.bo.SessionBellVolume
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [29])
@HiltAndroidTest
class BellSanitizerTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sanitizer: BellSanitizer

    @Inject
    lateinit var bellRepository: BellRepository

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sectionRepository: SectionRepository

    @Inject
    lateinit var databaseOwner: DatabaseOwner

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        resetState()
        mockMediaPlayerToSucceed()
    }

    @After
    fun tearDown() {
        unmockkAll()
        databaseOwner.close()
    }

    private fun resetState() {
        databaseOwner.close()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        databaseOwner.reopen()
        context.filesDir.listFiles()?.forEach { it.delete() }
    }

    private fun mockMediaPlayerToSucceed() {
        mockkConstructor(MediaPlayer::class)
        every { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any<Uri>()) } just Runs
        every { anyConstructed<MediaPlayer>().setVolume(any(), any()) } just Runs
        every { anyConstructed<MediaPlayer>().prepare() } just Runs
        every { anyConstructed<MediaPlayer>().start() } just Runs
        every { anyConstructed<MediaPlayer>().stop() } just Runs
        every { anyConstructed<MediaPlayer>().release() } just Runs
    }

    @Test
    fun syncBuiltinBellUris_staleUriIsUpdatedToDefinition() =
        runBlocking {
            val definitions = BuiltinBells.definitions()
            val targetSeed = definitions.first()
            val targetName = context.getString(targetSeed.nameResId)
            val correctUri = BuiltinBells.resourceUri(context, targetSeed.rawResId)
            val staleUri = "${correctUri}_stale"
            bellRepository.insertBell(
                BellEntity(name = targetName, uri = staleUri, isBuiltin = true),
            )

            sanitizer.sanitizeBellUris()

            val updated = bellRepository.getBellByUri(correctUri)
            assertThat(updated).isNotNull()
            assertThat(updated!!.name).isEqualTo(targetName)
            assertThat(updated.isBuiltin).isTrue()
            for (seed in definitions) {
                assertThat(
                    bellRepository.getBellByUri(BuiltinBells.resourceUri(context, seed.rawResId)),
                ).isNotNull()
            }
        }

    @Test
    fun removeOrphanedBuiltinBells_reassignsSectionToDemoAndDeletesRow() =
        runBlocking {
            val orphanId =
                bellRepository
                    .insertBell(
                        BellEntity(
                            name = "Orphaned Builtin Bell",
                            uri = BuiltinBells.resourceUri(context, R.raw.bell1),
                            isBuiltin = true,
                        ),
                    ).toInt()
            val session = Session("Test Session", "")
            sessionRepository.insertSession(session)
            val section = Section("Section", 60)
            section.bellId = orphanId
            sectionRepository.insertSection(session, section)

            sanitizer.sanitizeBellUris()

            assertThat(bellRepository.getBellById(orphanId)).isNull()
            val demoId = bellRepository.getDemoBell()!!.id
            assertThat(sectionRepository.readSection(section.id)!!.bellId).isEqualTo(demoId)
        }

    @Test
    fun removeOrphanedCustomBells_missingFile_reassignsSectionAndDeletesVolumes() =
        runBlocking {
            val missingFileName = "bell_missing.mp3"
            val missingUri = "file://${context.filesDir}/$missingFileName"
            val customId =
                bellRepository
                    .insertBell(
                        BellEntity(
                            name = "Missing Custom Bell",
                            uri = missingUri,
                            isBuiltin = false,
                        ),
                    ).toInt()
            val session = Session("Test Session", "")
            sessionRepository.insertSession(session)
            val section = Section("Section", 60)
            section.bellId = customId
            sectionRepository.insertSection(session, section)
            sessionRepository.saveBellVolumes(
                session.id,
                listOf(SessionBellVolume(bellId = customId, volume = 80)),
            )

            sanitizer.sanitizeBellUris()

            assertThat(bellRepository.getBellById(customId)).isNull()
            val demoId = bellRepository.getDemoBell()!!.id
            assertThat(sectionRepository.readSection(section.id)!!.bellId).isEqualTo(demoId)
            assertThat(sessionRepository.readBellVolumes(session.id)).isEmpty()
        }

    @Test
    fun importOrphanedBellFiles_addsCustomBellRowForFile() =
        runBlocking {
            copyRawResourceToFilesDir(R.raw.bell1, "bell_test.mp3")

            sanitizer.sanitizeBellUris()

            val expectedUri = "file://${context.filesDir}/bell_test.mp3"
            val imported = bellRepository.getBellByUri(expectedUri)
            assertThat(imported).isNotNull()
            assertThat(imported!!.name).isEqualTo("test.mp3")
            assertThat(imported.isBuiltin).isFalse()
        }

    @Test
    fun sanitizeBellUris_endToEnd_cleansAllMismatchTypes() =
        runBlocking {
            val definitions = BuiltinBells.definitions()
            val seed = seedEndToEndState(definitions)

            sanitizer.sanitizeBellUris()

            assertBuiltinDefinitionsMatch(definitions)
            assertThat(bellRepository.getBellById(seed.orphanBuiltinId)).isNull()
            assertThat(bellRepository.getBellById(seed.orphanCustomId)).isNull()

            val demoId = bellRepository.getDemoBell()!!.id
            assertThat(sectionRepository.readSection(seed.orphanBuiltinSection.id)!!.bellId)
                .isEqualTo(demoId)
            assertThat(sectionRepository.readSection(seed.orphanCustomSection.id)!!.bellId)
                .isEqualTo(demoId)
            assertThat(sessionRepository.readBellVolumes(seed.session.id)).isEmpty()

            val importedUri = "file://${context.filesDir}/bell_imported.mp3"
            val imported = bellRepository.getBellByUri(importedUri)
            assertThat(imported).isNotNull()
            assertThat(imported!!.isBuiltin).isFalse()
            assertThat(imported.name).isEqualTo("imported.mp3")
        }

    private data class EndToEndSeed(
        val session: Session,
        val orphanBuiltinSection: Section,
        val orphanCustomSection: Section,
        val orphanBuiltinId: Int,
        val orphanCustomId: Int,
    )

    private suspend fun seedEndToEndState(definitions: List<BuiltinBells.BellSeed>): EndToEndSeed {
        val staleSeed = definitions.first()
        val staleName = context.getString(staleSeed.nameResId)
        val correctUri = BuiltinBells.resourceUri(context, staleSeed.rawResId)
        bellRepository.insertBell(
            BellEntity(name = staleName, uri = "${correctUri}_stale", isBuiltin = true),
        )

        val orphanBuiltinId =
            bellRepository
                .insertBell(
                    BellEntity(
                        name = "Zombie Builtin",
                        uri = BuiltinBells.resourceUri(context, R.raw.bell1),
                        isBuiltin = true,
                    ),
                ).toInt()

        val session = Session("End-to-end Session", "")
        sessionRepository.insertSession(session)

        val orphanBuiltinSection = Section("Orphan Builtin Section", 60)
        orphanBuiltinSection.bellId = orphanBuiltinId
        sectionRepository.insertSection(session, orphanBuiltinSection)

        val missingUri = "file://${context.filesDir}/bell_gone.mp3"
        val orphanCustomId =
            bellRepository
                .insertBell(
                    BellEntity(
                        name = "Gone Custom",
                        uri = missingUri,
                        isBuiltin = false,
                    ),
                ).toInt()

        val orphanCustomSection = Section("Orphan Custom Section", 90)
        orphanCustomSection.bellId = orphanCustomId
        sectionRepository.insertSection(session, orphanCustomSection)
        sessionRepository.saveBellVolumes(
            session.id,
            listOf(SessionBellVolume(bellId = orphanCustomId, volume = 70)),
        )

        copyRawResourceToFilesDir(R.raw.bell1, "bell_imported.mp3")

        return EndToEndSeed(
            session,
            orphanBuiltinSection,
            orphanCustomSection,
            orphanBuiltinId,
            orphanCustomId,
        )
    }

    private suspend fun assertBuiltinDefinitionsMatch(definitions: List<BuiltinBells.BellSeed>) {
        for (seed in definitions) {
            val name = context.getString(seed.nameResId)
            val uri = BuiltinBells.resourceUri(context, seed.rawResId)
            val bell = bellRepository.getBellByUri(uri)
            assertThat(bell).isNotNull()
            assertThat(bell!!.name).isEqualTo(name)
            assertThat(bell.isBuiltin).isTrue()
        }
    }

    private fun copyRawResourceToFilesDir(
        rawResId: Int,
        fileName: String,
    ) {
        val outFile = File(context.filesDir, fileName)
        context.resources.openRawResource(rawResId).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
