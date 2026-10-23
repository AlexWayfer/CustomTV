package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** After the welcome, a connection on its way, or waiting to retry, is a reconnect; before it, the first connect. */
internal fun chatReconnecting(welcomed: Boolean, state: ChatConnectionState): Boolean =
    welcomed && (state == ChatConnectionState.Reconnecting || state == ChatConnectionState.Connecting)

/**
 * The notices a change of the chat connection adds: connecting, the welcome once connected, and the lost and
 * restored connection after it. [onWelcomed] runs once the first connection of a channel is up, and [onReconnected]
 * once a later one is, after its notice.
 */
internal class ChatConnectionNotices(
    private val messages: MutableStateFlow<List<ChatMessage>>,
    private val trim: ChatTrim,
    private val onWelcomed: () -> Unit,
    private val onReconnected: () -> Unit,
) {
    private var hasWelcomed = false
    private val _reconnecting = MutableStateFlow(false)

    /** The chat lost its connection after the welcome and is getting it back. */
    val reconnecting: StateFlow<Boolean> = _reconnecting

    /** A channel opens: the next connection is its first. */
    fun reset() {
        hasWelcomed = false
        _reconnecting.value = false
    }

    fun onState(previous: ChatConnectionState?, state: ChatConnectionState) {
        showNotices(previous, state)
        // After the reconnect callback, which may start a load, so a bar for both does not blink between them.
        _reconnecting.value = chatReconnecting(hasWelcomed, state)
    }

    private fun showNotices(previous: ChatConnectionState?, state: ChatConnectionState) {
        when (state) {
            ChatConnectionState.Connecting -> {
                if (!hasWelcomed) {
                    messages.value = listOf(
                        chatNoticeMessage(ChatNotice.Connecting, CHAT_WELCOME_NOTICE_ID),
                    )
                } else if (previous == ChatConnectionState.Disconnected) {
                    showReconnectNotice(ChatNotice.Reconnecting)
                }
            }
            ChatConnectionState.Connected -> {
                if (!hasWelcomed) {
                    hasWelcomed = true
                    showWelcomeNotice()
                    onWelcomed()
                } else if (previous != ChatConnectionState.Connected) {
                    showReconnectNotice(ChatNotice.Connected)
                    onReconnected()
                }
            }
            ChatConnectionState.Reconnecting -> {
                if (hasWelcomed) {
                    if (previous == ChatConnectionState.Connected) {
                        appendDisconnectedNotice()
                    }
                    showReconnectNotice(ChatNotice.Reconnecting)
                }
            }
            ChatConnectionState.Disconnected -> {
                if (hasWelcomed && previous == ChatConnectionState.Connected) {
                    appendDisconnectedNotice()
                }
            }
        }
    }

    private fun appendDisconnectedNotice() {
        messages.update { current ->
            trim.keep(current + chatNoticeMessage(ChatNotice.Disconnected, newStatusNoticeId()))
        }
    }

    private fun showReconnectNotice(notice: ChatNotice) {
        messages.update { current ->
            val next = replaceOrAppendReconnectNotice(
                messages = current,
                notice = notice,
                newId = newStatusNoticeId(),
                maxMessages = MAX_KEPT_CHAT_MESSAGES,
            )
            trim.keep(next)
        }
    }

    private fun showWelcomeNotice() {
        messages.update { current ->
            trim.keep(replaceOrAppendWelcomeNotice(current, CHAT_WELCOME_NOTICE_ID, MAX_KEPT_CHAT_MESSAGES))
        }
    }
}

private fun newStatusNoticeId(): String = "system-${System.nanoTime()}"
