package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

/** A switch in the Sending section of the Chat page. */
enum class ChatSendOption(internal val key: Preferences.Key<Boolean>) {
    KeepKeyboard(KEEP_KEYBOARD_AFTER_SEND),
    ScrollToLatest(SCROLL_TO_LATEST_AFTER_SEND),
    SentMessageHistory(SENT_MESSAGE_HISTORY),
}

/** Writes the settings of the Chat page; [AppSettingsStore] reads them. */
class ChatPageSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    suspend fun setSevenTvEmotes(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SEVEN_TV_EMOTES] = enabled
        }
    }

    suspend fun setFfzEmotes(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[FFZ_EMOTES] = enabled
        }
    }

    suspend fun setBttvEmotes(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BTTV_EMOTES] = enabled
        }
    }

    suspend fun setMeMessageItalic(italic: Boolean) {
        dataStore.edit { prefs ->
            prefs[ME_MESSAGE_ITALIC] = italic
        }
    }

    suspend fun setHighlightFirstMessages(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[HIGHLIGHT_FIRST_MESSAGES] = enabled
        }
    }

    suspend fun setMarkRaiders(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MARK_RAIDERS] = enabled
        }
    }

    suspend fun setTopChatterBadges(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[TOP_CHATTER_BADGES] = enabled }
    }

    suspend fun setEmoteCompletionWithoutColon(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[EMOTE_COMPLETION_WITHOUT_COLON] = enabled
        }
    }

    suspend fun setSendOption(option: ChatSendOption, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[option.key] = enabled }
    }

    suspend fun setLinkPreviewMode(mode: LinkPreviewMode) {
        dataStore.edit { prefs ->
            prefs[LINK_PREVIEW_MODE] = mode.name
        }
    }
}
