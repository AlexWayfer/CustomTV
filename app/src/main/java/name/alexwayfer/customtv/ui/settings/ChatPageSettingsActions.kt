package name.alexwayfer.customtv.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChatPageSettingsStore
import name.alexwayfer.customtv.data.ChatSendOption
import name.alexwayfer.customtv.data.LinkPreviewMode
import name.alexwayfer.customtv.data.ThirdPartyChatOption

/** Saves what the user changes on the Chat page of the settings. */
class ChatPageSettingsActions(private val scope: CoroutineScope, private val store: ChatPageSettingsStore) {
    fun setThirdPartyOption(option: ThirdPartyChatOption, enabled: Boolean) {
        scope.launch {
            store.setThirdPartyOption(option, enabled)
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

    fun setSendOption(option: ChatSendOption, enabled: Boolean) {
        scope.launch { store.setSendOption(option, enabled) }
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
