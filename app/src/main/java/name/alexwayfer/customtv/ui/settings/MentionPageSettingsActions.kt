package name.alexwayfer.customtv.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.KeywordPhrase
import name.alexwayfer.customtv.data.MentionPageSettingsStore

/** Saves what the user changes on the Mentions page of the settings. */
class MentionPageSettingsActions(private val scope: CoroutineScope, private val store: MentionPageSettingsStore) {
    fun setHighlightMentions(enabled: Boolean) {
        scope.launch {
            store.setHighlightMentions(enabled)
        }
    }

    fun setMentionVibration(enabled: Boolean) {
        scope.launch {
            store.setMentionVibration(enabled)
        }
    }

    fun setMentionVibrationMs(durationMs: Int) {
        scope.launch {
            store.setMentionVibrationMs(durationMs)
        }
    }

    fun setMentionVibrationPercent(percent: Int) {
        scope.launch {
            store.setMentionVibrationPercent(percent)
        }
    }

    fun setMentionSound(enabled: Boolean) {
        scope.launch {
            store.setMentionSound(enabled)
        }
    }

    fun setMentionSoundUri(uri: String) {
        scope.launch {
            store.setMentionSoundUri(uri)
        }
    }

    fun setKeywordPhrases(phrases: List<KeywordPhrase>) {
        scope.launch {
            store.setKeywordPhrases(phrases)
        }
    }

    fun setTemporarilyPinHighlightedMessages(enabled: Boolean) {
        scope.launch {
            store.setTemporarilyPinHighlightedMessages(enabled)
        }
    }

    fun setHighlightPinSeconds(seconds: Int) {
        scope.launch {
            store.setHighlightPinSeconds(seconds)
        }
    }

    fun setLoadRecentChatOnOpen(enabled: Boolean) {
        scope.launch { store.setLoadRecentChatOnOpen(enabled) }
    }
}
