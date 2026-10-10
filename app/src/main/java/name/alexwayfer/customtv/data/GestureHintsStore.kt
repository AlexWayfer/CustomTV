package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.gestureHintsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gesture_hints",
)

/** Which gestures the user already knows and how close each tip is to showing, kept across restarts. */
internal class GestureHintsStore(context: Context) {
    private val dataStore = context.applicationContext.gestureHintsDataStore

    suspend fun markGestureUsed(hint: GestureHint) {
        dataStore.edit { prefs ->
            if (prefs[hint.gestureUsedKey] != true) prefs[hint.gestureUsedKey] = true
        }
    }

    /** Counts a slow use of what [hint] is about; its result says whether the tip is due now. */
    suspend fun recordSlowUse(hint: GestureHint): GestureHintSlowUse {
        var result: GestureHintSlowUse? = null
        dataStore.edit { prefs ->
            val step = gestureHintAfterSlowUse(prefs.progress(hint), hint.slowUsesBeforeHint)
            prefs[hint.slowUsesKey] = step.progress.slowUses
            prefs[hint.shownKey] = step.progress.shown
            result = step
        }
        return checkNotNull(result)
    }

    private fun MutablePreferences.progress(hint: GestureHint) = GestureHintProgress(
        gestureUsed = this[hint.gestureUsedKey] ?: false,
        slowUses = this[hint.slowUsesKey] ?: 0,
        shown = this[hint.shownKey] ?: false,
    )

    private val GestureHint.gestureUsedKey get() = booleanPreferencesKey("${key}_gesture_used")
    private val GestureHint.slowUsesKey get() = intPreferencesKey("${key}_slow_uses")
    private val GestureHint.shownKey get() = booleanPreferencesKey("${key}_shown")
}
