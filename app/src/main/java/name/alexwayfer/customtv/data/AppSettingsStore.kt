package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException

internal val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

/**
 * Reads every app setting, and writes the settings of the smaller pages. The Chat and Mentions pages write theirs
 * through [ChatPageSettingsStore], [HiddenMessageSettingsStore], and [MentionPageSettingsStore].
 */
class AppSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            sevenTvEmotes = prefs[SEVEN_TV_EMOTES] ?: defaults.sevenTvEmotes,
            ffzEmotes = prefs[FFZ_EMOTES] ?: defaults.ffzEmotes,
            bttvEmotes = prefs[BTTV_EMOTES] ?: defaults.bttvEmotes,
            meMessageItalic = prefs[ME_MESSAGE_ITALIC] ?: defaults.meMessageItalic,
            highlightFirstMessages = prefs[HIGHLIGHT_FIRST_MESSAGES]
                ?: defaults.highlightFirstMessages,
            markRaiders = prefs[MARK_RAIDERS] ?: defaults.markRaiders,
            topChatterBadges = prefs[TOP_CHATTER_BADGES] ?: defaults.topChatterBadges,
            highlightMentions = prefs[HIGHLIGHT_MENTIONS] ?: defaults.highlightMentions,
            mentionVibration = prefs[MENTION_VIBRATION] ?: defaults.mentionVibration,
            mentionVibrationMs = prefs[MENTION_VIBRATION_MS] ?: defaults.mentionVibrationMs,
            mentionVibrationPercent = prefs[MENTION_VIBRATION_PERCENT] ?: defaults.mentionVibrationPercent,
            mentionSound = prefs[MENTION_SOUND] ?: defaults.mentionSound,
            mentionSoundUri = prefs[MENTION_SOUND_URI] ?: defaults.mentionSoundUri,
            keywordPhrases = decodeKeywordPhrases(prefs[KEYWORD_PHRASES]),
            temporarilyPinHighlightedMessages = prefs[TEMPORARILY_PIN_HIGHLIGHTED_MESSAGES]
                ?: defaults.temporarilyPinHighlightedMessages,
            highlightPinSeconds = prefs[HIGHLIGHT_PIN_SECONDS] ?: defaults.highlightPinSeconds,
            loadRecentChatOnOpen = prefs[LOAD_RECENT_CHAT_ON_OPEN] ?: defaults.loadRecentChatOnOpen,
            emoteCompletionWithoutColon = prefs[EMOTE_COMPLETION_WITHOUT_COLON]
                ?: defaults.emoteCompletionWithoutColon,
            keepKeyboardAfterSend = prefs[KEEP_KEYBOARD_AFTER_SEND] ?: defaults.keepKeyboardAfterSend,
            scrollToLatestAfterSend = prefs[SCROLL_TO_LATEST_AFTER_SEND] ?: defaults.scrollToLatestAfterSend,
            sentMessageHistory = prefs[SENT_MESSAGE_HISTORY] ?: defaults.sentMessageHistory,
            hiddenPhrases = decodeKeywordPhrases(prefs[HIDDEN_PHRASES]),
            hidePhrasesWhenModerator = prefs[HIDE_PHRASES_WHEN_MODERATOR] ?: defaults.hidePhrasesWhenModerator,
            backgroundSoundOnly = prefs[BACKGROUND_SOUND_ONLY] ?: defaults.backgroundSoundOnly,
            streamStartNotifications = prefs[STREAM_START_NOTIFICATIONS]
                ?: defaults.streamStartNotifications,
            streamChangeNotifications = prefs[STREAM_CHANGE_NOTIFICATIONS]
                ?: defaults.streamChangeNotifications,
            watchStreakNotifications = prefs[WATCH_STREAK_NOTIFICATIONS]
                ?: defaults.watchStreakNotifications,
            whisperUnreadGlow = prefs[WHISPER_UNREAD_GLOW] ?: defaults.whisperUnreadGlow,
            linkPreviewMode = storedLinkPreviewMode(prefs[LINK_PREVIEW_MODE]),
            autoClaimBonus = prefs[AUTO_CLAIM_BONUS] ?: defaults.autoClaimBonus,
            portraitLanguage = storedPortraitLanguage(prefs[PORTRAIT_LANGUAGE]),
            portraitModel = storedPortraitModelKind(prefs[PORTRAIT_MODEL]),
            sleepTimerMinutes = prefs[SLEEP_TIMER_MINUTES] ?: defaults.sleepTimerMinutes,
        )
    }

    suspend fun setBackgroundSoundOnly(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BACKGROUND_SOUND_ONLY] = enabled }
    }

    suspend fun setStreamStartNotifications(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[STREAM_START_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setStreamChangeNotifications(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[STREAM_CHANGE_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setWatchStreakNotifications(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[WATCH_STREAK_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setWhisperUnreadGlow(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[WHISPER_UNREAD_GLOW] = enabled }
    }

    suspend fun setAutoClaimBonus(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_CLAIM_BONUS] = enabled
        }
    }

    suspend fun setPortraitLanguage(language: PortraitLanguage) {
        dataStore.edit { prefs ->
            prefs[PORTRAIT_LANGUAGE] = language.name
        }
    }

    suspend fun setPortraitModel(model: PortraitModelKind) {
        dataStore.edit { prefs ->
            prefs[PORTRAIT_MODEL] = model.name
        }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[SLEEP_TIMER_MINUTES] = minutes
        }
    }

    companion object {
        private const val TAG = "AppSettings"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val current = MutableStateFlow<AppSettings?>(null)
        val restored: StateFlow<AppSettings?> = current.asStateFlow()
        private var reading = false

        fun start(context: Context) {
            if (reading) return
            reading = true
            val store = AppSettingsStore(context.applicationContext)
            scope.launch {
                try {
                    store.settings.collect { current.value = it }
                } catch (failure: IOException) {
                    AppLog.w(TAG, "settings restore failed: ${failure.javaClass.simpleName}")
                }
            }
        }
    }
}
