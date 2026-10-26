package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * The open channel's latest shoutout from PubSub, which the chat shows as a plaque while it lasts, with a line in the
 * chat that stays.
 */
internal class ChannelShoutouts(
    scope: CoroutineScope,
    pubSub: TwitchPubSubClient,
    private val currentChannelId: () -> String?,
    private val messages: MutableStateFlow<List<ChatMessage>>,
    private val trim: ChatTrim,
    /** This chat's chatters, login to display name, for the name of who gave the shoutout. */
    private val chatters: () -> Map<String, String>,
) {
    private val _latest = MutableStateFlow<ChannelShoutout?>(null)
    val latest: StateFlow<ChannelShoutout?> = _latest

    init {
        scope.launch {
            pubSub.shoutouts.collect { shoutout ->
                val accepted = shoutoutForChannel(shoutout, currentChannelId()) ?: return@collect
                AppLog.i(TAG, "shoutout to ${accepted.targetLogin}")
                _latest.value = accepted
                val row = shoutoutChatRow(accepted, shoutoutSourceName(accepted.sourceLogin, chatters()))
                messages.update { current ->
                    if (current.any { it.id == row.id }) current else trim.keep(current + row)
                }
            }
        }
    }

    /** Forgets the shoutout of the channel the chat leaves. */
    fun clear() {
        _latest.value = null
    }

    private companion object {
        const val TAG = "Shoutout"
    }
}
