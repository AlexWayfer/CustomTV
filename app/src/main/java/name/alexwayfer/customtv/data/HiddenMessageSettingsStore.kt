package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.edit

/** Writes the hidden messages settings of the Chat page; [AppSettingsStore] reads them. */
class HiddenMessageSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    suspend fun setHiddenPhrases(phrases: List<KeywordPhrase>) {
        dataStore.edit { prefs -> prefs[HIDDEN_PHRASES] = encodeKeywordPhrases(phrases) }
    }

    suspend fun setHidePhrasesWhenModerator(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[HIDE_PHRASES_WHEN_MODERATOR] = enabled }
    }
}
