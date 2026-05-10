package at.priv.graf.zazentimer.database

import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DbOperationsMappingTest {
    @Test
    fun toEntity_session_mapsId() {
        val session = Session(id = 42, name = "Test")
        val entity = EntityMapper.toEntity(session)
        assertThat(entity._id).isEqualTo(42)
    }

    @Test
    fun toEntity_session_mapsName() {
        val session = Session(id = 1, name = "Test")
        val entity = EntityMapper.toEntity(session)
        assertThat(entity.name).isEqualTo("Test")
    }

    @Test
    fun toEntity_session_mapsDescription() {
        val session = Session(id = 1, name = "Test", description = "Desc")
        val entity = EntityMapper.toEntity(session)
        assertThat(entity.description).isEqualTo("Desc")
    }

    @Test
    fun toEntity_session_nullName_becomesEmptyString() {
        val session = Session(id = 1, name = null)
        val entity = EntityMapper.toEntity(session)
        assertThat(entity.name).isEmpty()
    }

    @Test
    fun toEntity_session_nullDescription_becomesEmptyString() {
        val session = Session(id = 1, name = "Test", description = null)
        val entity = EntityMapper.toEntity(session)
        assertThat(entity.description).isEmpty()
    }

    @Test
    fun toBo_sessionEntity_mapsId() {
        val entity = SessionEntity(_id = 7, name = "Hello", description = "World")
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.id).isEqualTo(7)
    }

    @Test
    fun toBo_sessionEntity_mapsName() {
        val entity = SessionEntity(_id = 1, name = "Hello", description = "")
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.name).isEqualTo("Hello")
    }

    @Test
    fun toBo_sessionEntity_mapsDescription() {
        val entity = SessionEntity(_id = 1, name = "", description = "World")
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.description).isEqualTo("World")
    }

    @Test
    fun toEntity_section_mapsAllFields() {
        val section =
            Section(
                id = 5,
                fkSession = 10,
                name = "Zazen",
                duration = 1800,
                bell = 3,
                rank = 2,
                bellcount = 2,
                bellpause = 5,
                bellUri = "content://audio/bell.mp3",
                volume = 75,
            )
        val entity = EntityMapper.toEntity(section)
        assertThat(entity._id).isEqualTo(5)
        assertThat(entity.fk_session).isEqualTo(10)
        assertThat(entity.name).isEqualTo("Zazen")
        assertThat(entity.duration).isEqualTo(1800)
        assertThat(entity.bell).isEqualTo(3)
        assertThat(entity.rank).isEqualTo(2)
        assertThat(entity.bellcount).isEqualTo(2)
        assertThat(entity.bellpause).isEqualTo(5)
        assertThat(entity.belluri).isEqualTo("content://audio/bell.mp3")
        assertThat(entity.volume).isEqualTo(75)
    }

    @Test
    fun toEntity_section_nullName_becomesEmptyString() {
        val section = Section(id = 1, name = null)
        val entity = EntityMapper.toEntity(section)
        assertThat(entity.name).isEmpty()
    }

    @Test
    fun toBo_sectionEntity_mapsAllFields() {
        val entity =
            SectionEntity(
                _id = 5,
                fk_session = 10,
                name = "Kinhin",
                duration = 300,
                bell = 1,
                rank = 3,
                bellcount = 3,
                bellpause = 10,
                belluri = "content://audio/bell2.mp3",
                volume = 50,
            )
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.id).isEqualTo(5)
        assertThat(bo.fkSession).isEqualTo(10)
        assertThat(bo.name).isEqualTo("Kinhin")
        assertThat(bo.duration).isEqualTo(300)
        assertThat(bo.bell).isEqualTo(1)
        assertThat(bo.rank).isEqualTo(3)
        assertThat(bo.bellcount).isEqualTo(3)
        assertThat(bo.bellpause).isEqualTo(10)
        assertThat(bo.bellUri).isEqualTo("content://audio/bell2.mp3")
        assertThat(bo.volume).isEqualTo(50)
    }

    @Test
    fun toBo_sectionEntity_nullRank_defaultsToMinusOne() {
        val entity = SectionEntity(_id = 1, rank = null)
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.rank).isEqualTo(-1)
    }

    @Test
    fun toBo_sectionEntity_nullBellcount_defaultsToOne() {
        val entity = SectionEntity(_id = 1, bellcount = null)
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.bellcount).isEqualTo(1)
    }

    @Test
    fun toBo_sectionEntity_nullBellpause_defaultsToOne() {
        val entity = SectionEntity(_id = 1, bellpause = null)
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.bellpause).isEqualTo(1)
    }

    @Test
    fun toBo_sectionEntity_nullVolume_defaultsTo100() {
        val entity = SectionEntity(_id = 1, volume = null)
        val bo = EntityMapper.toBo(entity)
        assertThat(bo.volume).isEqualTo(100)
    }

    @Test
    fun roundTrip_session_toEntity_toBo_preservesFields() {
        val original = Session(id = 99, name = "Sesshin", description = "Retreat")
        val entity = EntityMapper.toEntity(original)
        val roundTripped = EntityMapper.toBo(entity)
        assertThat(roundTripped.id).isEqualTo(99)
        assertThat(roundTripped.name).isEqualTo("Sesshin")
        assertThat(roundTripped.description).isEqualTo("Retreat")
    }

    @Test
    fun roundTrip_section_toEntity_toBo_preservesFields() {
        val original =
            Section(
                id = 42,
                fkSession = 7,
                name = "Zazen",
                duration = 2400,
                bell = 2,
                rank = 1,
                bellcount = 3,
                bellpause = 15,
                bellUri = "uri",
                volume = 80,
            )
        val entity = EntityMapper.toEntity(original)
        val roundTripped = EntityMapper.toBo(entity)
        assertThat(roundTripped.id).isEqualTo(42)
        assertThat(roundTripped.fkSession).isEqualTo(7)
        assertThat(roundTripped.name).isEqualTo("Zazen")
        assertThat(roundTripped.duration).isEqualTo(2400)
        assertThat(roundTripped.bell).isEqualTo(2)
        assertThat(roundTripped.rank).isEqualTo(1)
        assertThat(roundTripped.bellcount).isEqualTo(3)
        assertThat(roundTripped.bellpause).isEqualTo(15)
        assertThat(roundTripped.bellUri).isEqualTo("uri")
        assertThat(roundTripped.volume).isEqualTo(80)
    }
}
