package at.priv.graf.zazentimer.database

object TestBellHelper {
    const val TEST_BELL_NAME = "Test Bell"
    const val TEST_BELL_URI = "android.resource://at.priv.graf.zazentimer/raw/bell2"

    suspend fun seedBell(bellRepo: BellRepository): Int =
        bellRepo
            .insertBell(
                BellEntity(
                    name = TEST_BELL_NAME,
                    uri = TEST_BELL_URI,
                    isBuiltin = true,
                ),
            ).toInt()

    suspend fun seedBell(db: AppDatabase): Int =
        db
            .bellDao()
            .insert(
                BellEntity(
                    name = TEST_BELL_NAME,
                    uri = TEST_BELL_URI,
                    isBuiltin = true,
                ),
            ).toInt()
}
