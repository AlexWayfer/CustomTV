package name.alexwayfer.customtv.chat

internal fun messageMentionsUser(
    message: ChatMessage,
    selfLogin: String,
    selfDisplayName: String,
): Boolean {
    if (selfLogin.isBlank()) return false
    if (message.userLogin.equals(selfLogin, ignoreCase = true)) return false
    if (!message.isMentionableChat()) return false
    val reply = message.reply
    return (reply != null && reply.parentUserLogin.equals(selfLogin, ignoreCase = true)) ||
        textContainsNick(message.rawText, selfLogin) || (
        selfDisplayName.isNotBlank() &&
            !selfDisplayName.equals(selfLogin, ignoreCase = true) &&
            textContainsNick(message.rawText, selfDisplayName)
    )
}

internal fun ChatMessage.isMentionableChat(): Boolean {
    return notice == null && emoteChange == null && (
        eventKind == ChatEventKind.Normal ||
            eventKind == ChatEventKind.Announcement ||
            eventKind == ChatEventKind.Highlight ||
            eventKind == ChatEventKind.Reward ||
            eventKind == ChatEventKind.ModerationHold
    )
}

internal fun textContainsNick(text: String, nick: String): Boolean {
    if (nick.isBlank() || text.isBlank()) return false
    var index = text.indexOf(nick, 0, ignoreCase = true)
    while (index >= 0 && !wordAt(text, index, nick.length)) index = text.indexOf(nick, index + 1, ignoreCase = true)
    return index >= 0
}

/** Whether the [length] characters at [index] of [text] are a whole word, not a part of a longer nick. */
private fun wordAt(text: String, index: Int, length: Int): Boolean {
    val end = index + length
    val beforeOk = index == 0 || !text[index - 1].isNickChar()
    val afterOk = end == text.length || !text[end].isNickChar()
    return beforeOk && afterOk
}

internal fun Char.isNickChar(): Boolean = this == '_' || isLetterOrDigit()
