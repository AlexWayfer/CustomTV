package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.ChatterHistory
import name.alexwayfer.customtv.chat.copyableText
import name.alexwayfer.customtv.chat.replyMention
import kotlin.math.abs

/** One of the user's own messages in this chat: the [message] the plaque shows, and the [text] the field takes back. */
internal data class ChatSentMessage(val message: ChatMessage, val text: String) {
    val id: String get() = message.id
}

/**
 * The user's own messages in this chat, oldest first, from the chatter history that their own card shows: a reply
 * with the nick it answered and a `/me` message as the command. A subscription or another event without the user's
 * own words is left out.
 */
internal fun chatSentHistory(history: ChatterHistory, selfLogin: String?): List<ChatSentMessage> {
    val login = selfLogin?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return emptyList()
    return history.messages[login].orEmpty().mapNotNull { message ->
        if (message.rawText.isBlank()) return@mapNotNull null
        val text = message.copyableText() ?: return@mapNotNull null
        ChatSentMessage(chatSentShownMessage(message), if (message.isAction) "/me $text" else text)
    }
}

/**
 * The message as the plaque shows it: with a reply's nick ahead of its text, as the field takes it back, and without
 * a deletion.
 */
private fun chatSentShownMessage(message: ChatMessage): ChatMessage {
    val mention = message.reply?.let(::replyMention)
    val parts = if (mention == null || message.rawText.trim().startsWith(mention, ignoreCase = true)) {
        message.parts
    } else {
        listOf(ChatPart.Text("$mention ")) + message.parts
    }
    return message.copy(parts = parts, deleted = false, deletedBy = null, timeoutSeconds = null, banned = false)
}

/** [chatSentHistory] that changes only with the user's own messages, so other chatters do not recompose the input. */
@Composable
internal fun rememberSentHistory(
    chatterHistory: StateFlow<ChatterHistory>,
    selfLogin: String?,
): List<ChatSentMessage> {
    val history = chatterHistory.collectAsStateWithLifecycle()
    val sent = remember(history, selfLogin) { derivedStateOf { chatSentHistory(history.value, selfLogin) } }
    return sent.value
}

/** The message the history plaque shows: [shownId], or the newest one when it is not set or has left the history. */
internal fun chatHistoryShown(history: List<ChatSentMessage>, shownId: String?): ChatSentMessage? =
    history.firstOrNull { it.id == shownId } ?: history.lastOrNull()

/** The message one step [older] or newer than [shownId], staying at the end it has reached. */
internal fun chatHistoryStep(history: List<ChatSentMessage>, shownId: String?, older: Boolean): ChatSentMessage? {
    val index = history.indexOfFirst { it.id == shownId }.takeIf { it >= 0 } ?: history.lastIndex
    if (index < 0) return null
    return history[(if (older) index - 1 else index + 1).coerceIn(0, history.lastIndex)]
}

/** Whether a message is left one step [older] or newer than [shownId]. */
internal fun chatHistoryCanStep(history: List<ChatSentMessage>, shownId: String?, older: Boolean): Boolean {
    val shown = chatHistoryShown(history, shownId) ?: return false
    return chatHistoryStep(history, shown.id, older) != shown
}

/** What Up and Down on a hardware keyboard recalled: the message in the field, and the draft it took the place of. */
internal data class ChatHistoryRecall(val shownId: String? = null, val draft: String = "")

/** The recall after a key, and the text that goes in the field. */
internal data class ChatHistoryKeyResult(val recall: ChatHistoryRecall, val text: String)

/**
 * Up ([older]) or Down on a hardware keyboard, as on a computer: Up puts the newest message in the field, keeping the
 * draft, and goes on to older ones; Down goes back to newer ones and past the newest brings the draft back. A recalled
 * message the user has edited, or one that has left the history, counts as the draft. Null leaves the key to the field:
 * nothing older is left, or Down is pressed on the draft.
 */
internal fun chatHistoryKey(
    history: List<ChatSentMessage>,
    recall: ChatHistoryRecall,
    fieldText: String,
    older: Boolean,
): ChatHistoryKeyResult? {
    val shown = history.firstOrNull { it.id == recall.shownId }?.takeIf { it.text == fieldText }
    if (shown == null) {
        val newest = history.lastOrNull()?.takeIf { older } ?: return null
        return ChatHistoryKeyResult(ChatHistoryRecall(newest.id, draft = fieldText), newest.text)
    }
    val index = history.indexOf(shown)
    val next = history.getOrNull(if (older) index - 1 else index + 1)
    return when {
        next != null -> ChatHistoryKeyResult(recall.copy(shownId = next.id), next.text)
        older -> null
        else -> ChatHistoryKeyResult(ChatHistoryRecall(), recall.draft)
    }
}

/** The history button shows in the field while the keyboard or the picker is up and there is a message to recall. */
internal fun chatHistoryButtonShown(
    enabled: Boolean,
    canSend: Boolean,
    composerOpen: Boolean,
    hasHistory: Boolean,
): Boolean = enabled && canSend && composerOpen && hasHistory

/** The opened plaque shows with its button; completions of the word being typed take its place while they show. */
internal fun chatHistoryPlaqueShown(open: Boolean, buttonShown: Boolean, completionShown: Boolean): Boolean =
    open && buttonShown && !completionShown

/**
 * Which way a swipe over the plaque pages: down to the older message and up to the newer one, as the chat above runs.
 * Null for a swipe more sideways than vertical.
 */
internal fun chatHistorySwipeOlder(dx: Float, dy: Float): Boolean? =
    if (dy == 0f || abs(dy) <= abs(dx)) null else dy > 0f
