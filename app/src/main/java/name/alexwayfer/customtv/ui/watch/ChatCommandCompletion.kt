package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.NickCompletionEdit

/** The chat commands the message field handles itself, without sending them. */
internal enum class ChatCommand(val word: String) {
    User("/user"),
}

/** The command word typed at the start of the field, up to [end], while the cursor is in it. */
internal data class ChatCommandQuery(val end: Int, val text: String)

internal fun chatCommandQueryAtCursor(text: String, cursor: Int): ChatCommandQuery? {
    if (cursor !in 1..text.length || text[0] != '/') return null
    if ((0 until cursor).any { text[it].isWhitespace() }) return null
    var end = cursor
    while (end < text.length && !text[end].isWhitespace()) end++
    return ChatCommandQuery(end = end, text = text.substring(0, end))
}

internal fun matchingChatCommands(query: String): List<ChatCommand> =
    ChatCommand.entries.filter { it.word.startsWith(query, ignoreCase = true) }

/** [text] with its command word replaced by [command] and a space, with the cursor after that space. */
internal fun textWithCompletedCommand(text: String, cursor: Int, command: ChatCommand, maxLength: Int): NickCompletionEdit? {
    val query = chatCommandQueryAtCursor(text, cursor) ?: return null
    val rest = text.substring(query.end)
    val next = command.word + (if (rest.startsWith(' ')) rest else " $rest")
    if (next.length > maxLength) return null
    return NickCompletionEdit(text = next, cursor = command.word.length + 1)
}
