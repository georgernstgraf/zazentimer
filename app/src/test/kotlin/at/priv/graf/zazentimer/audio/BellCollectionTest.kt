package at.priv.graf.zazentimer.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.content.Context
import at.priv.graf.zazentimer.bo.Section

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BellCollectionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        BellCollection.release()
    }

    @After
    fun tearDown() {
        BellCollection.release()
    }

    @Test
    fun initialize_loadsEightBells() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getBellList()).hasSize(8)
    }

    @Test
    fun initialize_succeedsMultipleTimes() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getBellList()).hasSize(8)
        BellCollection.initialize(context)
        assertThat(BellCollection.getBellList()).hasSize(8)
    }

    @Test
    fun getBell_byIndex_validIndices() {
        BellCollection.initialize(context)
        for (i in 0..7) {
            val bell = BellCollection.getBell(i)
            assertThat(bell).isNotNull()
        }
    }

    @Test
    fun getBell_byIndex_outOfBoundsReturnsNull() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getBell(-1)).isNull()
        assertThat(BellCollection.getBell(8)).isNull()
        assertThat(BellCollection.getBell(100)).isNull()
    }

    @Test
    fun getBell_byIndex_returnsBellWithUri() {
        BellCollection.initialize(context)
        val bell = BellCollection.getBell(0)
        assertThat(bell).isNotNull()
        assertThat(bell!!.uri).isNotNull()
        assertThat(bell.uri.toString()).contains("android.resource://")
    }

    @Test
    fun getBell_byIndex_bellsHaveDistinctUris() {
        BellCollection.initialize(context)
        val uris = (0..7).map { BellCollection.getBell(it)!!.uri.toString() }.toSet()
        assertThat(uris).hasSize(8)
    }

    @Test
    fun getBell_byName_validName() {
        BellCollection.initialize(context)
        val bell0 = BellCollection.getBell(0)!!
        val found = BellCollection.getBell(bell0.getName())
        assertThat(found).isNotNull()
        assertThat(found!!.uri).isEqualTo(bell0.uri)
    }

    @Test
    fun getBell_byName_invalidName_returnsNull() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getBell("nonexistent_bell")).isNull()
    }

    @Test
    fun getBellList_returnsAllBells() {
        BellCollection.initialize(context)
        val list = BellCollection.getBellList()
        assertThat(list).hasSize(8)
        for (bell in list) {
            assertThat(bell.uri).isNotNull()
            assertThat(bell.getName()).isNotEmpty()
        }
    }

    @Test
    fun getBellForSection_matchingUri() {
        BellCollection.initialize(context)
        val bell = BellCollection.getBell(3)!!
        val section = Section(volume = 80)
        section.bellUri = bell.uri.toString()

        val result = BellCollection.getBellForSection(section)
        assertThat(result).isNotNull()
        assertThat(result!!.uri).isEqualTo(bell.uri)
    }

    @Test
    fun getBellForSection_nonMatchingUri_returnsNull() {
        BellCollection.initialize(context)
        val section = Section(volume = 80)
        section.bellUri = "content://nonexistent/audio.mp3"

        assertThat(BellCollection.getBellForSection(section)).isNull()
    }

    @Test
    fun getBellForSection_nullUri_returnsNull() {
        BellCollection.initialize(context)
        val section = Section(volume = 80)
        section.bellUri = null

        assertThat(BellCollection.getBellForSection(section)).isNull()
    }

    @Test
    fun getUriForName_matchingSuffix() {
        BellCollection.initialize(context)
        val bell = BellCollection.getBell(0)!!
        val uriSuffix = bell.uri.toString().substringAfterLast("/")
        val result = BellCollection.getUriForName(uriSuffix)
        assertThat(result).isEqualTo(bell.uri)
    }

    @Test
    fun getUriForName_nonMatching_returnsNull() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getUriForName("nonexistent_file")).isNull()
    }

    @Test
    fun getDemoBell_returnsBellNamedAfterBellName2() {
        BellCollection.initialize(context)
        val demoBell = BellCollection.getDemoBell()
        assertThat(demoBell).isNotNull()
        val bell1 = BellCollection.getBell(1)!!
        assertThat(demoBell!!.uri).isEqualTo(bell1.uri)
    }

    @Test
    fun release_clearsBells() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getBellList()).hasSize(8)
        BellCollection.release()
        assertThat(BellCollection.getBellList()).isEmpty()
    }

    @Test
    fun release_clearsGetBell() {
        BellCollection.initialize(context)
        assertThat(BellCollection.getBell(0)).isNotNull()
        BellCollection.release()
        assertThat(BellCollection.getBell(0)).isNull()
    }

    @Test
    fun bellUris_containPackageName() {
        BellCollection.initialize(context)
        for (i in 0..7) {
            val bell = BellCollection.getBell(i)!!
            assertThat(bell.uri.toString()).contains(context.packageName)
        }
    }

    @Test
    fun bellConstants_matchCorrectIndices() {
        BellCollection.initialize(context)
        assertThat(BellCollection.BELL_IDX_HIGH_TONE).isEqualTo(0)
        assertThat(BellCollection.BELL_IDX_LOW_TONE).isEqualTo(1)
        assertThat(BellCollection.BELL_IDX_JAP_RHINBOWL_107).isEqualTo(2)
        assertThat(BellCollection.BELL_IDX_JAP_RHINBOWL_88).isEqualTo(3)
        assertThat(BellCollection.BELL_IDX_JAP_RHINBOWL_90).isEqualTo(4)
        assertThat(BellCollection.BELL_IDX_JAP_RHINBOWL_164).isEqualTo(5)
        assertThat(BellCollection.BELL_IDX_TIB_RHINBOWL_230).isEqualTo(6)
        assertThat(BellCollection.BELL_IDX_JAP_RHINBOWL_97).isEqualTo(7)
    }
}
