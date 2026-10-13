package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Marks what moderators delete, in the chat and in each chatter's recent messages and mentions. */
internal class ChatDeletions(
    private val messages: MutableStateFlow<List<ChatMessage>>,
    private val chatterHistory: MutableStateFlow<ChatterHistory>,
) {
    fun markMessageDeleted(event: ChatMessage) {
        messages.update { current ->
            markMessageDeleted(current, event.id, event.deletedBy)
        }
        chatterHistory.update { history ->
            markChatterMessageDeleted(history, event.id, event.deletedBy)
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
        chatterHistory.update { history ->
            markChatterMessagesDeleted(
                history,
                event.userLogin,
                event.deletedBy,
                event.timeoutSeconds,
                event.banned,
            )
        }
    }
}
