package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Marks what moderators delete, in the chat and in each chatter's recent messages. */
internal class ChatDeletions(
    private val messages: MutableStateFlow<List<ChatMessage>>,
    private val recentAuthorMessages: MutableStateFlow<Map<String, List<ChatMessage>>>,
) {
    fun markMessageDeleted(event: ChatMessage) {
        messages.update { current ->
            markMessageDeleted(current, event.id, event.deletedBy)
        }
        recentAuthorMessages.update { history ->
            markRecentAuthorMessageDeleted(history, event.id, event.deletedBy)
        }
    }

    fun markUserMessagesDeleted(event: ChatMessage) {
        messages.update { current ->
            val marked = markUserMessagesDeleted(
                current,
                event.userLogin,
                event.deletedBy,
                event.timeoutSeconds,
                event.banned,
            )
            withModerationNotice(marked, clearChatNoticeRow(event)).takeLast(MAX_CHAT_MESSAGES)
        }
        recentAuthorMessages.update { history ->
            markRecentAuthorMessagesDeleted(
                history,
                event.userLogin,
                event.deletedBy,
                event.timeoutSeconds,
                event.banned,
            )
        }
    }
}
