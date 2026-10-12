package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The notices a change of the chat connection adds: connecting, the welcome once connected, and the lost and
 * restored connection after it. [onWelcomed] runs once the first connection of a channel is up.
 */
internal class ChatConnectionNotices(
    private val messages: MutableStateFlow<List<ChatMessage>>,
    private val onWelcomed: () -> Unit,
) {
    private var hasWelcomed = false

    /** A channel opens: the next connection is its first. */
    fun reset() {
        hasWelcomed = false
    }

    fun onState(previous: ChatConnectionState?, state: ChatConnectionState) {
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
            (current + chatNoticeMessage(ChatNotice.Disconnected, newStatusNoticeId()))
                .takeLast(MAX_CHAT_MESSAGES)
        }
    }

    private fun showReconnectNotice(notice: ChatNotice) {
        messages.update { current ->
            replaceOrAppendReconnectNotice(
                messages = current,
                notice = notice,
                newId = newStatusNoticeId(),
                maxMessages = MAX_CHAT_MESSAGES,
            )
        }
    }

    private fun showWelcomeNotice() {
        messages.update { current ->
            replaceOrAppendWelcomeNotice(current, CHAT_WELCOME_NOTICE_ID, MAX_CHAT_MESSAGES)
        }
    }
}

private fun newStatusNoticeId(): String = "system-${System.nanoTime()}"
