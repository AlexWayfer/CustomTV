package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.latestOwnChatMessage

/** What `/user <nick>` typed in the message field asks for; it is never sent to the chat. */
internal sealed interface ChatUserCommand {
    /** Opens the chatter card of [login], in lower case. */
    data class Open(val login: String) : ChatUserCommand

    /** `/user` without a nick that can be a login: the draft stays for the user to fix. */
    data object Invalid : ChatUserCommand
}

private val WHITESPACE = Regex("\\s+")
private val TWITCH_LOGIN = Regex("[a-z0-9_]{1,25}")

/** What follows a command at the start of a message, as [chatCommandNick] reads it: the login it names. */
internal sealed interface ChatCommandNick {
    data class Login(val login: String) : ChatCommandNick

    /** The command without a nick that can be a login. */
    data object Invalid : ChatCommandNick
}

/**
 * The nick after [command] in [text], or null when [text] does not start with [command]. The nick may start with
 * `@`, may be in any case, and may be the display name of a session chatter in [chatters] (login to display name),
 * as nick completion inserts it. Words after the nick are ignored.
 */
internal fun chatCommandNick(text: String, command: ChatCommand, chatters: Map<String, String>): ChatCommandNick? {
    val words = text.trim().split(WHITESPACE, limit = 3)
    if (!words[0].equals(command.word, ignoreCase = true)) return null
    val nick = words.getOrNull(1)?.removePrefix("@").orEmpty()
    val login = chatters.entries.firstOrNull { it.value.equals(nick, ignoreCase = true) }?.key ?: nick.lowercase()
    return if (TWITCH_LOGIN.matches(login)) ChatCommandNick.Login(login) else ChatCommandNick.Invalid
}

/** The `/user` command in [text], or null when [text] is a message to send; [chatCommandNick] reads the nick. */
internal fun chatUserCommand(text: String, chatters: Map<String, String>): ChatUserCommand? =
    when (val nick = chatCommandNick(text, ChatCommand.User, chatters)) {
        is ChatCommandNick.Login -> ChatUserCommand.Open(nick.login)
        ChatCommandNick.Invalid -> ChatUserCommand.Invalid
        null -> null
    }

/**
 * The card for [login], with the name, ID, and badges of their latest message in this chat when there is one;
 * without one, the card loads the badges.
 */
internal fun chatterCardRequestForLogin(
    login: String,
    messages: List<ChatMessage>,
    badgeUrls: Map<String, String>,
): ChatterCardRequest {
    val latest = latestOwnChatMessage(messages, login)
    return ChatterCardRequest(
        login = login,
        displayName = latest?.displayName?.ifBlank { null } ?: login,
        userId = latest?.userId,
        badges = latest?.badges.orEmpty(),
        badgeUrls = badgeUrls,
        badgesKnown = latest != null,
    )
}
