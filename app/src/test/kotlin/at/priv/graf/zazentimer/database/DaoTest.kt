package at.priv.graf.zazentimer.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var sectionDao: SectionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sessionDao = db.sessionDao()
        sectionDao = db.sectionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun sessionDao_insertAndGetAll() {
        sessionDao.insert(SessionEntity(name = "Test Session", description = "Desc"))

        val sessions = sessionDao.getAllSessions()
        assertThat(sessions).hasSize(1)
        assertThat(sessions[0].name).isEqualTo("Test Session")
        assertThat(sessions[0].description).isEqualTo("Desc")
    }

    @Test
    fun sessionDao_insertAndGetById() {
        val id = sessionDao.insert(SessionEntity(name = "By ID", description = "Test"))

        val result = sessionDao.getSessionById(id.toInt())
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("By ID")
    }

    @Test
    fun sessionDao_getByIdNotFound() {
        val result = sessionDao.getSessionById(999)
        assertThat(result).isNull()
    }

    @Test
    fun sessionDao_update() {
        val id = sessionDao.insert(SessionEntity(name = "Old", description = ""))

        sessionDao.update(SessionEntity(_id = id.toInt(), name = "New", description = "Updated"))

        val result = sessionDao.getAllSessions()
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("New")
        assertThat(result[0].description).isEqualTo("Updated")
    }

    @Test
    fun sessionDao_delete() {
        val id = sessionDao.insert(SessionEntity(name = "Delete Me", description = ""))

        sessionDao.deleteById(id.toInt())

        assertThat(sessionDao.getAllSessions()).isEmpty()
    }

    @Test
    fun sessionDao_getAllOrderedByNameCaseInsensitive() {
        sessionDao.insert(SessionEntity(name = "Banana", description = ""))
        sessionDao.insert(SessionEntity(name = "apple", description = ""))
        sessionDao.insert(SessionEntity(name = "Cherry", description = ""))

        val sessions = sessionDao.getAllSessions()
        assertThat(sessions).hasSize(3)
        assertThat(sessions[0].name).isEqualTo("apple")
        assertThat(sessions[1].name).isEqualTo("Banana")
        assertThat(sessions[2].name).isEqualTo("Cherry")
    }

    @Test
    fun sectionDao_insertAndGetForSession() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "Section 1",
                duration = 300,
                bell = 0,
                rank = 1,
            ),
        )

        val sections = sectionDao.getSectionsForSession(sessionId.toInt())
        assertThat(sections).hasSize(1)
        assertThat(sections[0].name).isEqualTo("Section 1")
    }

    @Test
    fun sectionDao_getById() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        val id =
            sectionDao.insert(
                SectionEntity(
                    fk_session = sessionId.toInt(),
                    name = "By ID",
                    duration = 60,
                    bell = 0,
                    rank = 1,
                ),
            )

        val result = sectionDao.getSectionById(id.toInt())
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("By ID")
    }

    @Test
    fun sectionDao_getByIdNotFound() {
        val result = sectionDao.getSectionById(999)
        assertThat(result).isNull()
    }

    @Test
    fun sectionDao_getMaxRankEmpty() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))

        assertThat(sectionDao.getMaxRank(sessionId.toInt())).isNull()
    }

    @Test
    fun sectionDao_getMaxRankWithData() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "S1",
                duration = 60,
                bell = 0,
                rank = 1,
            ),
        )
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "S2",
                duration = 120,
                bell = 0,
                rank = 3,
            ),
        )
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "S3",
                duration = 180,
                bell = 0,
                rank = 2,
            ),
        )

        assertThat(sectionDao.getMaxRank(sessionId.toInt())).isEqualTo(3)
    }

    @Test
    fun sectionDao_update() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        val id =
            sectionDao.insert(
                SectionEntity(
                    fk_session = sessionId.toInt(),
                    name = "Old",
                    duration = 60,
                    bell = 0,
                    rank = 1,
                ),
            )

        sectionDao.update(
            SectionEntity(
                _id = id.toInt(),
                fk_session = sessionId.toInt(),
                name = "Updated",
                duration = 120,
                bell = 1,
                rank = 1,
            ),
        )

        val result = sectionDao.getSectionById(id.toInt())
        assertThat(result!!.name).isEqualTo("Updated")
        assertThat(result.duration).isEqualTo(120)
        assertThat(result.bell).isEqualTo(1)
    }

    @Test
    fun sectionDao_updateRank() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        val id =
            sectionDao.insert(
                SectionEntity(
                    fk_session = sessionId.toInt(),
                    name = "S1",
                    duration = 60,
                    bell = 0,
                    rank = 1,
                ),
            )

        sectionDao.updateRank(id.toInt(), 5)

        assertThat(sectionDao.getSectionById(id.toInt())!!.rank).isEqualTo(5)
    }

    @Test
    fun sectionDao_delete() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        val id =
            sectionDao.insert(
                SectionEntity(
                    fk_session = sessionId.toInt(),
                    name = "Del",
                    duration = 60,
                    bell = 0,
                    rank = 1,
                ),
            )

        sectionDao.deleteById(id)

        assertThat(sectionDao.getSectionsForSession(sessionId.toInt())).isEmpty()
    }

    @Test
    fun sectionDao_orderedByRank() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "Third",
                duration = 180,
                bell = 0,
                rank = 3,
            ),
        )
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "First",
                duration = 60,
                bell = 0,
                rank = 1,
            ),
        )
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "Second",
                duration = 120,
                bell = 0,
                rank = 2,
            ),
        )

        val sections = sectionDao.getSectionsForSession(sessionId.toInt())
        assertThat(sections).hasSize(3)
        assertThat(sections[0].name).isEqualTo("First")
        assertThat(sections[1].name).isEqualTo("Second")
        assertThat(sections[2].name).isEqualTo("Third")
    }

    @Test
    fun sectionDao_cascadeDeleteOnSessionDelete() {
        val sessionId = sessionDao.insert(SessionEntity(name = "S", description = ""))
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "S1",
                duration = 60,
                bell = 0,
                rank = 1,
            ),
        )
        sectionDao.insert(
            SectionEntity(
                fk_session = sessionId.toInt(),
                name = "S2",
                duration = 120,
                bell = 0,
                rank = 2,
            ),
        )

        sessionDao.deleteById(sessionId.toInt())

        assertThat(sectionDao.getSectionsForSession(sessionId.toInt())).isEmpty()
    }
}
