package at.priv.graf.zazentimer

import android.content.SharedPreferences
import android.util.Log
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.database.DbOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MigrationHelper(
    private val dbOperations: DbOperations,
    private val preferences: SharedPreferences,
    private val scope: CoroutineScope,
) {
    fun convertFromOldVersions() {
        if (!preferences.getBoolean(ZazenTimerActivity.PREF_KEY_CONVERTED_FROM_DB, false)) {
            Log.d(TAG, "marking settings as converted from DB to preferences...")
            preferences
                .edit()
                .putBoolean(ZazenTimerActivity.PREF_KEY_CONVERTED_FROM_DB, true)
                .apply()
            Log.d(TAG, "done converting settings")
        }
        if (!preferences.getBoolean(ZazenTimerActivity.PREF_KEY_CONVERTED_BELL_INDICES, false)) {
            Log.d(TAG, "converting Bell Indices to URIs...")
            scope.launch { convertBellIndices() }
            Log.d(TAG, "done converting Bell Indices")
        }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun convertBellIndices() {
        for (session in dbOperations.readSessions()) {
            for (section in dbOperations.readSections(session.id)) {
                convertSectionBellIndex(section)
            }
        }
    }

    private suspend fun convertSectionBellIndex(section: Section) {
        val bellUri = section.bellUri
        if (bellUri == null || bellUri.trim().isEmpty()) {
            val bell = BellCollection.getBell(section.bell)
            if (bell != null) {
                section.bellUri = bell.uri.toString()
            } else {
                section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: return
            }
            section.bell = BELL_INDEX_NONE
            dbOperations.updateSection(section)
        } else if (section.bell == BELL_INDEX_LEGACY_DEFAULT) {
            section.bell = BELL_INDEX_NONE
            section.bellUri = BellCollection.getDemoBell()?.uri?.toString() ?: return
            dbOperations.updateSection(section)
        }
    }

    companion object {
        private const val TAG = "ZMT_MigrationHelper"
        private const val BELL_INDEX_NONE = -2
        private const val BELL_INDEX_LEGACY_DEFAULT = -1
    }
}
