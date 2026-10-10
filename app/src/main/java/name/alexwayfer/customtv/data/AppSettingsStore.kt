package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
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

internal val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

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
            backgroundSoundOnly = prefs[BACKGROUND_SOUND_ONLY] ?: defaults.backgroundSoundOnly,
            streamStartNotifications = prefs[STREAM_START_NOTIFICATIONS]
                ?: defaults.streamStartNotifications,
            streamChangeNotifications = prefs[STREAM_CHANGE_NOTIFICATIONS]
                ?: defaults.streamChangeNotifications,
            watchStreakNotifications = prefs[WATCH_STREAK_NOTIFICATIONS]
                ?: defaults.watchStreakNotifications,
            linkPreviewMode = storedLinkPreviewMode(prefs[LINK_PREVIEW_MODE]),
            autoClaimBonus = prefs[AUTO_CLAIM_BONUS] ?: defaults.autoClaimBonus,
            portraitLanguage = storedPortraitLanguage(prefs[PORTRAIT_LANGUAGE]),
            portraitModel = storedPortraitModelKind(prefs[PORTRAIT_MODEL]),
            sleepTimerMinutes = prefs[SLEEP_TIMER_MINUTES] ?: defaults.sleepTimerMinutes,
        )
    }

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

    suspend fun setEmoteCompletionWithoutColon(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[EMOTE_COMPLETION_WITHOUT_COLON] = enabled
        }
    }

    suspend fun setKeepKeyboardAfterSend(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEEP_KEYBOARD_AFTER_SEND] = enabled }
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

    suspend fun setLinkPreviewMode(mode: LinkPreviewMode) {
        dataStore.edit { prefs ->
            prefs[LINK_PREVIEW_MODE] = mode.name
        }
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
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    AppLog.w(TAG, "settings restore failed: ${failure.javaClass.simpleName}")
                }
            }
        }

        private val SEVEN_TV_EMOTES = booleanPreferencesKey("seven_tv_emotes")
        private val FFZ_EMOTES = booleanPreferencesKey("ffz_emotes")
        private val BTTV_EMOTES = booleanPreferencesKey("bttv_emotes")
        private val ME_MESSAGE_ITALIC = booleanPreferencesKey("me_message_italic")
        private val HIGHLIGHT_FIRST_MESSAGES = booleanPreferencesKey("highlight_first_messages")
        private val MARK_RAIDERS = booleanPreferencesKey("mark_raiders")
        private val HIGHLIGHT_MENTIONS = booleanPreferencesKey("highlight_mentions")
        private val MENTION_VIBRATION = booleanPreferencesKey("mention_vibration")
        private val MENTION_VIBRATION_MS = intPreferencesKey("mention_vibration_ms")
        private val MENTION_VIBRATION_PERCENT = intPreferencesKey("mention_vibration_percent")
        private val MENTION_SOUND = booleanPreferencesKey("mention_sound")
        private val MENTION_SOUND_URI = stringPreferencesKey("mention_sound_uri")
        private val KEYWORD_PHRASES = stringPreferencesKey("keyword_phrases")
        private val TEMPORARILY_PIN_HIGHLIGHTED_MESSAGES =
            booleanPreferencesKey("temporarily_pin_highlighted_messages")
        private val HIGHLIGHT_PIN_SECONDS = intPreferencesKey("highlight_pin_seconds")
        private val LOAD_RECENT_CHAT_ON_OPEN = booleanPreferencesKey("load_recent_chat_on_open")
        private val EMOTE_COMPLETION_WITHOUT_COLON = booleanPreferencesKey("emote_completion_without_colon")
        private val KEEP_KEYBOARD_AFTER_SEND = booleanPreferencesKey("keep_keyboard_after_send")
        private val BACKGROUND_SOUND_ONLY = booleanPreferencesKey("background_sound_only")
        private val STREAM_START_NOTIFICATIONS = booleanPreferencesKey("stream_start_notifications")
        private val STREAM_CHANGE_NOTIFICATIONS = booleanPreferencesKey("stream_change_notifications")
        private val WATCH_STREAK_NOTIFICATIONS = booleanPreferencesKey("watch_streak_notifications")
        private val LINK_PREVIEW_MODE = stringPreferencesKey("link_preview_mode")
        private val AUTO_CLAIM_BONUS = booleanPreferencesKey("auto_claim_bonus")
        private val PORTRAIT_LANGUAGE = stringPreferencesKey("portrait_language")
        private val PORTRAIT_MODEL = stringPreferencesKey("portrait_model")
        private val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
    }
}
