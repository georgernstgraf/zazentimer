package at.priv.graf.zazentimer.audio

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.media.MediaPlayer
import android.net.Uri
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.BellRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [29])
@HiltAndroidTest
class BellImporterTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val bellRepository: BellRepository = mockk(relaxed = true)

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = RuntimeEnvironment.getApplication()
        clearCustomBellsFromFilesDir()
        mockMediaPlayerToSucceed()
        coEvery { bellRepository.insertBell(any()) } returns 1L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun clearCustomBellsFromFilesDir() {
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
    fun import_rawResourceUri_writesFileAndInsertsEntity() {
        val uri = Uri.parse(BuiltinBells.resourceUri(context, R.raw.bell1))
        val importer = BellImporter(context, bellRepository)
        val capturedEntities = mutableListOf<BellEntity>()
        coEvery { bellRepository.insertBell(capture(capturedEntities)) } returns 1L

        val result = runBlocking { importer.import(uri) }

        val expectedFileName = "bell_${uri.lastPathSegment}"
        val expectedFile = File(context.filesDir, expectedFileName)
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.length()).isGreaterThan(0L)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo(uri.lastPathSegment)
        assertThat(result.uri).isEqualTo("file://${context.filesDir}/$expectedFileName")
        assertThat(capturedEntities).hasSize(1)
        assertThat(capturedEntities[0].name).isEqualTo(uri.lastPathSegment)
        assertThat(capturedEntities[0].uri).isEqualTo("file://${context.filesDir}/$expectedFileName")
    }

    @Test
    fun import_contentUriWithDisplayName_usesDisplayNameAsFileName() {
        val displayName = "temple_bell.mp3"
        val uri = Uri.parse("content://test.authority/bells/123")
        val resolver = mockResolverWithDisplayName(uri, displayName)
        val importer = BellImporter(context.withMockedContentResolver(resolver), bellRepository)

        val result = runBlocking { importer.import(uri) }

        val expectedFile = File(context.filesDir, "bell_$displayName")
        assertThat(expectedFile.exists()).isTrue()
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo(displayName)
        assertThat(result.uri).isEqualTo("file://${context.filesDir}/bell_$displayName")
    }

    @Test
    fun import_contentUriWithoutDisplayName_fallsBackToLastPathSegment() {
        val uri = Uri.parse("content://test.authority/bells/456")
        val resolver = mockResolverWithoutDisplayName(uri)
        val importer = BellImporter(context.withMockedContentResolver(resolver), bellRepository)

        val result = runBlocking { importer.import(uri) }

        val expectedFile = File(context.filesDir, "bell_456")
        assertThat(expectedFile.exists()).isTrue()
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("456")
    }

    @Test
    fun import_nullInputStream_throwsBellImportException() {
        val uri = Uri.parse("content://test.authority/bells/null")
        val resolver = mockk<ContentResolver>()
        every { resolver.query(uri, null, null, null, null) } returns null
        every { resolver.openInputStream(uri) } returns null
        val importer = BellImporter(context.withMockedContentResolver(resolver), bellRepository)

        val exception =
            assertThrows(BellImportException::class.java) {
                runBlocking { importer.import(uri) }
            }

        assertThat(exception).hasMessageThat().contains("Could not open input stream")
    }

    @Test
    fun import_ioExceptionDuringCopy_throwsBellImportExceptionAndCleansUp() {
        val uri = Uri.parse("content://test.authority/bells/broken")
        val resolver = mockk<ContentResolver>()
        every { resolver.query(uri, null, null, null, null) } returns null
        every { resolver.openInputStream(uri) } returns ThrowingInputStream()
        val importer = BellImporter(context.withMockedContentResolver(resolver), bellRepository)

        val exception =
            assertThrows(BellImportException::class.java) {
                runBlocking { importer.import(uri) }
            }

        assertThat(exception).hasMessageThat().contains("read failed")
        assertThat(exception.cause).isInstanceOf(IOException::class.java)
        assertThat(File(context.filesDir, "bell_unnamed").exists()).isFalse()
    }

    @Test
    fun import_unplayableFile_throwsBellImportExceptionDuringValidation() {
        val source = File(context.filesDir, "not_an_audio_file.txt")
        source.writeText("This is not audio.")
        val uri = Uri.fromFile(source)

        every { anyConstructed<MediaPlayer>().prepare() } throws IOException("unplayable")

        val importer = BellImporter(context, bellRepository)
        val exception =
            assertThrows(BellImportException::class.java) {
                runBlocking { importer.import(uri) }
            }

        assertThat(exception).hasMessageThat().contains("unplayable")
        assertThat(File(context.filesDir, "bell_not_an_audio_file.txt").exists()).isFalse()
    }

    @Test
    fun import_duplicateFileName_overwritesExistingFileAndSucceeds() {
        val displayName = "duplicate.mp3"
        val uri = Uri.parse("content://test.authority/bells/duplicate")
        val existing = File(context.filesDir, "bell_$displayName")
        existing.writeText("old content")

        val resolver = mockResolverWithDisplayName(uri, displayName)
        val content = "new content"
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(content.toByteArray())
        val importer = BellImporter(context.withMockedContentResolver(resolver), bellRepository)

        val result = runBlocking { importer.import(uri) }

        assertThat(result).isNotNull()
        assertThat(existing.exists()).isTrue()
        assertThat(existing.readText()).isEqualTo(content)
    }

    private fun Context.withMockedContentResolver(resolver: ContentResolver): Context {
        val spy = spyk(this)
        every { spy.contentResolver } returns resolver
        return spy
    }

    private fun mockResolverWithDisplayName(
        uri: Uri,
        displayName: String,
    ): ContentResolver {
        val resolver = mockk<ContentResolver>()
        val cursor = MatrixCursor(arrayOf("_display_name"))
        cursor.addRow(arrayOf(displayName))
        every { resolver.query(uri, null, null, null, null) } returns cursor
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream("content".toByteArray())
        return resolver
    }

    private fun mockResolverWithoutDisplayName(uri: Uri): ContentResolver {
        val resolver = mockk<ContentResolver>()
        val cursor = MatrixCursor(arrayOf("_id"))
        cursor.addRow(arrayOf(1))
        every { resolver.query(uri, null, null, null, null) } returns cursor
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream("content".toByteArray())
        return resolver
    }

    private class ThrowingInputStream : InputStream() {
        override fun read(): Int = throw IOException("read failed")
    }
}
