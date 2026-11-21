package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.ChatSendResult
import name.alexwayfer.customtv.data.chatMessageToSend
import kotlin.time.Duration.Companion.seconds

private val twitchIrcLogin = Regex("[a-z0-9_]+")
private val twitchIrcReplyId = Regex("[a-zA-Z0-9_-]+")
private val remainingTimeout = Regex("""\b([0-9]+) more seconds?\.?$""", RegexOption.IGNORE_CASE)

internal fun ircLogin(raw: String): String? = raw.lowercase().removePrefix("#").takeIf(twitchIrcLogin::matches)

internal fun validIrcAccessToken(token: String): Boolean =
    token.isNotEmpty() && token.none { it.isWhitespace() || it == '\u0000' }

internal fun ircChatMessage(room: String, message: String, replyParentId: String?): String? {
    val text = chatMessageToSend(message) ?: return null
    if (text.any { it == '\r' || it == '\n' || it == '\u0000' }) return null
    if (replyParentId != null && !twitchIrcReplyId.matches(replyParentId)) return null
    val tags = replyParentId?.let { "@reply-parent-msg-id=$it " }.orEmpty()
    return "${tags}PRIVMSG $room :$text"
}

internal fun ircLoginRejected(line: IrcLine): Boolean =
    line.command == "NOTICE" && line.params.firstOrNull() == "*" &&
        line.trailing in setOf("Login authentication failed", "Improperly formatted auth")

internal fun ircChatSendResult(line: IrcLine, receivedAtMillis: Long): ChatSendResult? = when (line.command) {
    "USERSTATE" -> ChatSendResult.Sent
    "NOTICE" -> ircChatRefusal(line, receivedAtMillis)
    else -> null
}

private fun ircChatRefusal(line: IrcLine, receivedAtMillis: Long): ChatSendResult.Dropped {
    val code = line.tags["msg-id"].orEmpty()
    val text = line.trailing.orEmpty().trim()
    return when (code) {
        "msg_timedout" -> {
            val seconds = remainingTimeout.find(text)?.groupValues?.get(1)?.toIntOrNull()
            val deadline = seconds?.let { receivedAtMillis + it.seconds.inWholeMilliseconds }
            ChatSendResult.Dropped("user_timed_out", text, timeoutEndsAtMillis = deadline)
        }
        "msg_warned" -> ChatSendResult.Dropped("user_warned", text)
        else -> ChatSendResult.Dropped(code, text)
    }
}
