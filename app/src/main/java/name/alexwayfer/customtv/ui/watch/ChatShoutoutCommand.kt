package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatSendError

/** What `/shoutout <nick>` typed in the message field asks for; it is never sent to the chat. */
internal sealed interface ChatShoutoutCommand {
    /** Gives a shoutout to the channel [login], in lower case. */
    data class Send(val login: String) : ChatShoutoutCommand

    /** `/shoutout` without a nick that can be a channel: the draft stays for the user to fix. */
    data object Invalid : ChatShoutoutCommand
}

/** The `/shoutout` command in [text], or null when [text] is a message to send; [chatCommandNick] reads the nick. */
internal fun chatShoutoutCommand(text: String, chatters: Map<String, String>): ChatShoutoutCommand? =
    when (val nick = chatCommandNick(text, ChatCommand.Shoutout, chatters)) {
        is ChatCommandNick.Login -> ChatShoutoutCommand.Send(nick.login)
        ChatCommandNick.Invalid -> ChatShoutoutCommand.Invalid
        null -> null
    }

/** What Send does with a `/shoutout`: shows why it cannot go, or gives the shoutout. */
internal sealed interface ChatShoutoutCommandRun {
    data class Show(val error: ChatSendError) : ChatShoutoutCommandRun

    data class Send(val login: String) : ChatShoutoutCommandRun
}

/** The free build ([locked]) points to Premium; Premium explains a missing nick, or gives the shoutout. */
internal fun chatShoutoutCommandRun(command: ChatShoutoutCommand, locked: Boolean): ChatShoutoutCommandRun = when {
    locked -> ChatShoutoutCommandRun.Show(ChatSendError.ShoutoutPremium)
    command is ChatShoutoutCommand.Send -> ChatShoutoutCommandRun.Send(command.login)
    else -> ChatShoutoutCommandRun.Show(ChatSendError.ShoutoutUsage)
}
