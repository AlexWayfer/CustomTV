package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The raid a chatter came with: the raiding channel, and its name once the app knows it. */
data class ChatRaider(
    val sourceChannelId: String,
    val sourceDisplayName: String?,
)

/** Messages of chatters who came with a raid, by Twitch message ID. Empty unless the user moderates the open channel. */
object ChatRaiders {
    private val _byMessageId = MutableStateFlow<Map<String, ChatRaider>>(emptyMap())
    val byMessageId: StateFlow<Map<String, ChatRaider>> = _byMessageId

    fun mark(messageId: String, raider: ChatRaider) {
        _byMessageId.update { current -> chatRaidersAfter(current, messageId, raider) }
    }

    fun clear() {
        _byMessageId.value = emptyMap()
    }
}

/** Adds a mark and keeps the newest [MAX_CHAT_MESSAGES]: a chat shows no more rows than that. */
internal fun chatRaidersAfter(
    current: Map<String, ChatRaider>,
    messageId: String,
    raider: ChatRaider,
): Map<String, ChatRaider> {
    if (messageId.isBlank()) return current
    val next = current - messageId + (messageId to raider)
    if (next.size <= MAX_CHAT_MESSAGES) return next
    return next.entries.drop(next.size - MAX_CHAT_MESSAGES).associate { it.key to it.value }
}
