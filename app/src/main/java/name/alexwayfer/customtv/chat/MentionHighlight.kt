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
    var from = 0
    while (from <= text.length - nick.length) {
        val index = text.indexOf(nick, from, ignoreCase = true)
        if (index < 0) return false
        val beforeOk = index == 0 || !text[index - 1].isNickChar()
        val end = index + nick.length
        val afterOk = end == text.length || !text[end].isNickChar()
        if (beforeOk && afterOk) return true
        from = index + 1
    }
    return false
}

internal fun Char.isNickChar(): Boolean = this == '_' || isLetterOrDigit()
