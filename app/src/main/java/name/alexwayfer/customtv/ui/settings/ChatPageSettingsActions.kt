package name.alexwayfer.customtv.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChatPageSettingsStore
import name.alexwayfer.customtv.data.LinkPreviewMode

/** Saves what the user changes on the Chat page of the settings. */
class ChatPageSettingsActions(private val scope: CoroutineScope, private val store: ChatPageSettingsStore) {
    fun setSevenTvEmotes(enabled: Boolean) {
        scope.launch {
            store.setSevenTvEmotes(enabled)
        }
    }

    fun setFfzEmotes(enabled: Boolean) {
        scope.launch {
            store.setFfzEmotes(enabled)
        }
    }

    fun setBttvEmotes(enabled: Boolean) {
        scope.launch {
            store.setBttvEmotes(enabled)
        }
    }

    fun setMeMessageItalic(italic: Boolean) {
        scope.launch {
            store.setMeMessageItalic(italic)
        }
    }

    fun setHighlightFirstMessages(enabled: Boolean) {
        scope.launch {
            store.setHighlightFirstMessages(enabled)
        }
    }

    fun setEmoteCompletionWithoutColon(enabled: Boolean) {
        scope.launch {
            store.setEmoteCompletionWithoutColon(enabled)
        }
    }

    fun setKeepKeyboardAfterSend(enabled: Boolean) {
        scope.launch { store.setKeepKeyboardAfterSend(enabled) }
    }

    fun setScrollToLatestAfterSend(enabled: Boolean) {
        scope.launch { store.setScrollToLatestAfterSend(enabled) }
    }

    fun setMarkRaiders(enabled: Boolean) {
        scope.launch {
            store.setMarkRaiders(enabled)
        }
    }

    fun setTopChatterBadges(enabled: Boolean) {
        scope.launch { store.setTopChatterBadges(enabled) }
    }

    fun setLinkPreviewMode(mode: LinkPreviewMode) {
        scope.launch {
            store.setLinkPreviewMode(mode)
        }
    }
}
