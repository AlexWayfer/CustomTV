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

/** A switch in the 7TV, FFZ, or BTTV section of the Chat page. */
enum class ThirdPartyChatOption(internal val key: Preferences.Key<Boolean>) {
    SevenTvEmotes(SEVEN_TV_EMOTES),
    SevenTvBadges(SEVEN_TV_BADGES),
    SevenTvPaints(SEVEN_TV_PAINTS),
    FfzEmotes(FFZ_EMOTES),
    FfzBadges(FFZ_BADGES),
    BttvEmotes(BTTV_EMOTES),
    BttvBadges(BTTV_BADGES),
}

/** Writes the settings of the Chat page; [AppSettingsStore] reads them. */
class ChatPageSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    suspend fun setThirdPartyOption(option: ThirdPartyChatOption, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[option.key] = enabled }
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

    suspend fun setMessageMenuModeration(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[MESSAGE_MENU_MODERATION] = enabled }
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
