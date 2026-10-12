package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.edit

/** Writes the settings of the Mentions page; [AppSettingsStore] reads them. */
class MentionPageSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    suspend fun setHighlightMentions(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[HIGHLIGHT_MENTIONS] = enabled
        }
    }

    suspend fun setMentionVibration(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MENTION_VIBRATION] = enabled
        }
    }

    suspend fun setMentionVibrationMs(durationMs: Int) {
        dataStore.edit { prefs ->
            prefs[MENTION_VIBRATION_MS] = durationMs
        }
    }

    suspend fun setMentionVibrationPercent(percent: Int) {
        dataStore.edit { prefs ->
            prefs[MENTION_VIBRATION_PERCENT] = percent
        }
    }

    suspend fun setMentionSound(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MENTION_SOUND] = enabled
        }
    }

    suspend fun setMentionSoundUri(uri: String) {
        dataStore.edit { prefs ->
            prefs[MENTION_SOUND_URI] = uri
        }
    }

    suspend fun setKeywordPhrases(phrases: List<KeywordPhrase>) {
        dataStore.edit { prefs ->
            prefs[KEYWORD_PHRASES] = encodeKeywordPhrases(phrases)
        }
    }

    suspend fun setTemporarilyPinHighlightedMessages(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TEMPORARILY_PIN_HIGHLIGHTED_MESSAGES] = enabled
        }
    }

    suspend fun setHighlightPinSeconds(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[HIGHLIGHT_PIN_SECONDS] = seconds
        }
    }

    suspend fun setLoadRecentChatOnOpen(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[LOAD_RECENT_CHAT_ON_OPEN] = enabled }
    }
}
