package name.alexwayfer.customtv.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// The keys of appSettingsDataStore. AppSettingsStore reads them all; each settings page's store writes its own.

internal val SEVEN_TV_EMOTES = booleanPreferencesKey("seven_tv_emotes")
internal val FFZ_EMOTES = booleanPreferencesKey("ffz_emotes")
internal val BTTV_EMOTES = booleanPreferencesKey("bttv_emotes")
internal val ME_MESSAGE_ITALIC = booleanPreferencesKey("me_message_italic")
internal val HIGHLIGHT_FIRST_MESSAGES = booleanPreferencesKey("highlight_first_messages")
internal val MARK_RAIDERS = booleanPreferencesKey("mark_raiders")
internal val HIGHLIGHT_MENTIONS = booleanPreferencesKey("highlight_mentions")
internal val MENTION_VIBRATION = booleanPreferencesKey("mention_vibration")
internal val MENTION_VIBRATION_MS = intPreferencesKey("mention_vibration_ms")
internal val MENTION_VIBRATION_PERCENT = intPreferencesKey("mention_vibration_percent")
internal val MENTION_SOUND = booleanPreferencesKey("mention_sound")
internal val MENTION_SOUND_URI = stringPreferencesKey("mention_sound_uri")
internal val KEYWORD_PHRASES = stringPreferencesKey("keyword_phrases")
internal val TEMPORARILY_PIN_HIGHLIGHTED_MESSAGES =
    booleanPreferencesKey("temporarily_pin_highlighted_messages")
internal val HIGHLIGHT_PIN_SECONDS = intPreferencesKey("highlight_pin_seconds")
internal val LOAD_RECENT_CHAT_ON_OPEN = booleanPreferencesKey("load_recent_chat_on_open")
internal val EMOTE_COMPLETION_WITHOUT_COLON = booleanPreferencesKey("emote_completion_without_colon")
internal val KEEP_KEYBOARD_AFTER_SEND = booleanPreferencesKey("keep_keyboard_after_send")
internal val SCROLL_TO_LATEST_AFTER_SEND = booleanPreferencesKey("scroll_to_latest_after_send")
internal val BACKGROUND_SOUND_ONLY = booleanPreferencesKey("background_sound_only")
internal val STREAM_START_NOTIFICATIONS = booleanPreferencesKey("stream_start_notifications")
internal val STREAM_CHANGE_NOTIFICATIONS = booleanPreferencesKey("stream_change_notifications")
internal val WATCH_STREAK_NOTIFICATIONS = booleanPreferencesKey("watch_streak_notifications")
internal val LINK_PREVIEW_MODE = stringPreferencesKey("link_preview_mode")
internal val AUTO_CLAIM_BONUS = booleanPreferencesKey("auto_claim_bonus")
internal val PORTRAIT_LANGUAGE = stringPreferencesKey("portrait_language")
internal val PORTRAIT_MODEL = stringPreferencesKey("portrait_model")
internal val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
