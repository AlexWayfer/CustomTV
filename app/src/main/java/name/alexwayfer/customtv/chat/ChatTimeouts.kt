package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import name.alexwayfer.customtv.data.ChatSendResult
import name.alexwayfer.customtv.secondsRoundedUp
import kotlin.time.Duration.Companion.seconds

/** Known timeouts for the signed-in viewer, retained while switching channels or reconnecting IRC. */
internal class ChatTimeouts(val nowMillis: () -> Long = System::currentTimeMillis) {
    private val current = MutableStateFlow(ChatTimeoutState())
    val state: StateFlow<ChatTimeoutState> = current

    fun setUser(userId: String?) {
        current.update { if (it.userId == userId) it else ChatTimeoutState(userId) }
    }

    fun onMessage(channel: String, message: ChatMessage) {
        if (message.userId != current.value.userId) return
        current.update { state ->
            val update = chatTimeoutUpdate(message, state.userId)
            if (update == null) state else state.withUpdate(channel, update)
        }
    }

    fun onHistory(channel: String, history: RecentChatLoadResult) {
        current.update { state ->
            val update = (history.moderation.asSequence() + history.messages)
                .mapNotNull { chatTimeoutUpdate(it, state.userId) }
                .maxByOrNull { it.atMillis }
            if (update == null) state else state.withUpdate(channel, update)
        }
    }

    /** A status response must not overwrite a newer live event that arrived during the request. */
    fun applyCheck(
        channel: String,
        userId: String,
        beforeCheck: ChatTimeoutState,
        check: ChatTimeoutCheck,
        startedAtMillis: Long,
    ): Boolean {
        val previous = current.getAndUpdate { state ->
            if (state.canApplyCheck(channel, userId, beforeCheck, startedAtMillis)) {
                state.withUpdate(channel, ChatTimeoutUpdate(startedAtMillis, check.deadlineMillis))
            } else {
                state
            }
        }
        return previous.canApplyCheck(channel, userId, beforeCheck, startedAtMillis)
    }

    /** Helix confirms the timeout without a deadline. Look for the IRC event if it was missed live. */
    suspend fun recover(channel: String, userId: String?) {
        if (userId == null || userId != current.value.userId ||
            chatTimeoutSecondsLeft(current.value.deadline(channel, userId), nowMillis()) > 0
        ) {
            return
        }
        val history = loadRecentChatMessages(channel, MAX_CHAT_MESSAGES)
        if (userId == current.value.userId) onHistory(channel, history)
    }
}

internal data class ChatTimeoutState(
    val userId: String? = null,
    private val channels: Map<String, ChatTimeoutUpdate> = emptyMap(),
) {
    fun deadline(channel: String, userId: String?): Long? =
        channels[channel]?.endsAtMillis.takeIf { this.userId == userId }

    fun canApplyCheck(channel: String, userId: String, beforeCheck: ChatTimeoutState, startedAtMillis: Long): Boolean {
        if (this.userId != userId || beforeCheck.userId != userId) return false
        val latest = channels[channel]
        return latest == beforeCheck.channels[channel] ||
            (latest != null && latest.atMillis < startedAtMillis)
    }

    fun withUpdate(channel: String, update: ChatTimeoutUpdate): ChatTimeoutState {
        val previous = channels[channel]
        return if (previous == null || update.atMillis > previous.atMillis) {
            copy(channels = channels + (channel to update))
        } else {
            this
        }
    }
}

internal sealed interface ChatTimeoutCheck {
    val deadlineMillis: Long?

    data object Clear : ChatTimeoutCheck {
        override val deadlineMillis: Long? = null
    }

    data class Until(override val deadlineMillis: Long) : ChatTimeoutCheck
}

/** A later message accepted from the viewer proves that their timeout ended. */
internal data class ChatTimeoutUpdate(val atMillis: Long, val endsAtMillis: Long?)

private fun chatTimeoutUpdate(message: ChatMessage, userId: String?): ChatTimeoutUpdate? {
    if (userId == null || message.userId != userId || message.sourceRoomId != null) return null
    val at = message.timestampMillis
    return when {
        message.eventKind == ChatEventKind.UserMessagesDeleted ->
            ChatTimeoutUpdate(at, message.timeoutSeconds?.let { at + it.seconds.inWholeMilliseconds })
        isOwnLiveChatLine(message, userId) -> ChatTimeoutUpdate(at, null)
        else -> null
    }
}

internal fun chatTimeoutSecondsLeft(deadlineMillis: Long?, nowMillis: Long): Long =
    deadlineMillis?.let { secondsRoundedUp((it - nowMillis).coerceAtLeast(0)) } ?: 0L

internal fun chatSendTimedOut(result: ChatSendResult): Boolean =
    result is ChatSendResult.Dropped && result.code == "user_timed_out"
