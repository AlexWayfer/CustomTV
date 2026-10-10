package name.alexwayfer.customtv.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import name.alexwayfer.customtv.chat.KeyboardHeights

private val Context.keyboardHeightsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "keyboard_heights",
)

/** The keyboard heights last seen in each orientation, so the emote picker matches the keyboard after a restart. */
internal class KeyboardHeightStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.keyboardHeightsDataStore

    /** The keyboard the user has chosen, by its IME ID; null when the system does not say. */
    fun currentKeyboardId(): String? =
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

    suspend fun load(): KeyboardHeights {
        val prefs = dataStore.data.first()
        return KeyboardHeights(
            keyboardId = prefs[KEYBOARD_ID],
            portraitPx = prefs[PORTRAIT_PX] ?: 0,
            landscapePx = prefs[LANDSCAPE_PX] ?: 0,
        )
    }

    suspend fun save(heights: KeyboardHeights) {
        dataStore.edit { prefs ->
            val id = heights.keyboardId
            if (id == null) prefs.remove(KEYBOARD_ID) else prefs[KEYBOARD_ID] = id
            prefs[PORTRAIT_PX] = heights.portraitPx
            prefs[LANDSCAPE_PX] = heights.landscapePx
        }
    }

    private companion object {
        val KEYBOARD_ID = stringPreferencesKey("keyboard_id")
        val PORTRAIT_PX = intPreferencesKey("portrait_px")
        val LANDSCAPE_PX = intPreferencesKey("landscape_px")
    }
}
