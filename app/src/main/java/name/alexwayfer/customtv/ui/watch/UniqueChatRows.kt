package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatMessage

/**
 * The chat list keys each row by its message id, and a repeated key crashes the app. Several sources feed the chat
 * (history, live chat, EventSub, moderation, replay), so a message one of them already added keeps only its first row.
 * A list without repeats comes back as is.
 */
internal fun uniqueChatRows(messages: List<ChatMessage>): List<ChatMessage> {
    val seen = HashSet<String>(messages.size * 2)
    if (messages.all { seen.add(it.id) }) return messages
    seen.clear()
    return messages.filter { seen.add(it.id) }
}
