package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.KeywordPhrase
import name.alexwayfer.customtv.data.LinkPreviewMode
import name.alexwayfer.customtv.data.PortraitLanguage
import name.alexwayfer.customtv.data.PortraitModelKind

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppSettingsStore(application)

    val settings: StateFlow<AppSettings?> = AppSettingsStore.restored

    fun setSevenTvEmotes(enabled: Boolean) {
        viewModelScope.launch {
            store.setSevenTvEmotes(enabled)
        }
    }

    fun setFfzEmotes(enabled: Boolean) {
        viewModelScope.launch {
            store.setFfzEmotes(enabled)
        }
    }

    fun setBttvEmotes(enabled: Boolean) {
        viewModelScope.launch {
            store.setBttvEmotes(enabled)
        }
    }

    fun setMeMessageItalic(italic: Boolean) {
        viewModelScope.launch {
            store.setMeMessageItalic(italic)
        }
    }

    fun setHighlightFirstMessages(enabled: Boolean) {
        viewModelScope.launch {
            store.setHighlightFirstMessages(enabled)
        }
    }

    fun setHighlightMentions(enabled: Boolean) {
        viewModelScope.launch {
            store.setHighlightMentions(enabled)
        }
    }

    fun setMentionVibration(enabled: Boolean) {
        viewModelScope.launch {
            store.setMentionVibration(enabled)
        }
    }

    fun setMentionVibrationMs(durationMs: Int) {
        viewModelScope.launch {
            store.setMentionVibrationMs(durationMs)
        }
    }

    fun setMentionVibrationPercent(percent: Int) {
        viewModelScope.launch {
            store.setMentionVibrationPercent(percent)
        }
    }

    fun setMentionSound(enabled: Boolean) {
        viewModelScope.launch {
            store.setMentionSound(enabled)
        }
    }

    fun setMentionSoundUri(uri: String) {
        viewModelScope.launch {
            store.setMentionSoundUri(uri)
        }
    }

    fun setKeywordPhrases(phrases: List<KeywordPhrase>) {
        viewModelScope.launch {
            store.setKeywordPhrases(phrases)
        }
    }

    fun setTemporarilyPinHighlightedMessages(enabled: Boolean) {
        viewModelScope.launch {
            store.setTemporarilyPinHighlightedMessages(enabled)
        }
    }

    fun setHighlightPinSeconds(seconds: Int) {
        viewModelScope.launch {
            store.setHighlightPinSeconds(seconds)
        }
    }

    fun setLoadRecentChatOnOpen(enabled: Boolean) {
        viewModelScope.launch { store.setLoadRecentChatOnOpen(enabled) }
    }

    fun setEmoteCompletionWithoutColon(enabled: Boolean) {
        viewModelScope.launch {
            store.setEmoteCompletionWithoutColon(enabled)
        }
    }

    fun setKeepKeyboardAfterSend(enabled: Boolean) {
        viewModelScope.launch { store.setKeepKeyboardAfterSend(enabled) }
    }

    fun setBackgroundSoundOnly(enabled: Boolean) {
        viewModelScope.launch { store.setBackgroundSoundOnly(enabled) }
    }

    fun setMarkRaiders(enabled: Boolean) {
        viewModelScope.launch {
            store.setMarkRaiders(enabled)
        }
    }

    fun setAutoClaimBonus(enabled: Boolean) {
        viewModelScope.launch {
            store.setAutoClaimBonus(enabled)
        }
    }

    fun setStreamStartNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.setStreamStartNotifications(enabled)
        }
    }

    fun setStreamChangeNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.setStreamChangeNotifications(enabled)
        }
    }

    fun setWatchStreakNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.setWatchStreakNotifications(enabled)
        }
    }

    fun setLinkPreviewMode(mode: LinkPreviewMode) {
        viewModelScope.launch {
            store.setLinkPreviewMode(mode)
        }
    }

    fun setPortraitLanguage(language: PortraitLanguage) {
        viewModelScope.launch {
            store.setPortraitLanguage(language)
        }
    }

    fun setPortraitModel(model: PortraitModelKind) {
        viewModelScope.launch {
            store.setPortraitModel(model)
        }
    }
}
