package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatModeChange
import name.alexwayfer.customtv.data.ChatSendError

/** A chat mode command typed in the field: the change it asks for, or a value it cannot take. */
internal sealed interface ChatModeCommandInput {
    data class Change(val change: ChatModeChange) : ChatModeCommandInput

    /** The [command] came with a value it does not understand, such as `/slow fast`. */
    data class Invalid(val command: ChatCommand) : ChatModeCommandInput
}

/** The chat mode command that starts [text], as Twitch's site takes it; null for any other text. */
internal fun chatModeCommand(text: String): ChatModeCommandInput? {
    val trimmed = text.trim()
    val word = trimmed.takeWhile { !it.isWhitespace() }
    val command = CHAT_MODE_COMMANDS.firstOrNull { it.word.equals(word, ignoreCase = true) } ?: return null
    val argument = trimmed.substring(word.length).trim()
    val change = when (command) {
        ChatCommand.Slow -> slowSeconds(argument)?.let(ChatModeChange::Slow)
        ChatCommand.SlowOff -> ChatModeChange.Slow(null)
        ChatCommand.Followers -> followMinutes(argument)?.let(ChatModeChange::Followers)
        ChatCommand.FollowersOff -> ChatModeChange.Followers(null)
        ChatCommand.Subscribers -> ChatModeChange.Subscribers(true)
        ChatCommand.SubscribersOff -> ChatModeChange.Subscribers(false)
        ChatCommand.EmoteOnly -> ChatModeChange.EmoteOnly(true)
        ChatCommand.EmoteOnlyOff -> ChatModeChange.EmoteOnly(false)
        ChatCommand.UniqueChat -> ChatModeChange.UniqueChat(true)
        ChatCommand.UniqueChatOff -> ChatModeChange.UniqueChat(false)
        ChatCommand.User, ChatCommand.Prediction, ChatCommand.Shoutout -> return null
    }
    return change?.let(ChatModeCommandInput::Change) ?: ChatModeCommandInput.Invalid(command)
}

/** `/slow` alone takes Twitch's default wait; otherwise a whole number of seconds, which Twitch checks itself. */
private fun slowSeconds(argument: String): Int? = when {
    argument.isEmpty() -> DEFAULT_SLOW_SECONDS
    DIGITS.matches(argument) -> argument.toIntOrNull()
    else -> null
}

/** `/followers` alone lets every follower chat; otherwise a time such as `30m`, `2 hours`, or `3mo`. */
private fun followMinutes(argument: String): Int? {
    if (argument.isEmpty()) return 0
    val match = FOLLOW_DURATION.matchEntire(argument) ?: return null
    val count = match.groupValues[1].toLongOrNull() ?: return null
    val unit = FOLLOW_UNIT_MINUTES[match.groupValues[2].lowercase()] ?: return null
    return (count * unit).takeIf { it <= Int.MAX_VALUE }?.toInt()
}

/** The wait Twitch sets when slow mode is turned on without one. */
private const val DEFAULT_SLOW_SECONDS = 30

private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_DAY = 24 * MINUTES_IN_HOUR
private const val MINUTES_IN_WEEK = 7 * MINUTES_IN_DAY

/** Twitch counts a month as 30 days: its longest follow, 3 months, is 129600 minutes. */
private const val MINUTES_IN_MONTH = 30 * MINUTES_IN_DAY

private val DIGITS = Regex("\\d+")
private val FOLLOW_DURATION = Regex("(\\d+)\\s*([a-zA-Z]*)")

/** The units `/followers` takes, short and long, with a number alone counting minutes. */
private val FOLLOW_UNIT_MINUTES = buildMap {
    listOf("", "m", "min", "mins", "minute", "minutes").forEach { put(it, 1L) }
    listOf("h", "hr", "hrs", "hour", "hours").forEach { put(it, MINUTES_IN_HOUR) }
    listOf("d", "day", "days").forEach { put(it, MINUTES_IN_DAY) }
    listOf("w", "wk", "wks", "week", "weeks").forEach { put(it, MINUTES_IN_WEEK) }
    listOf("mo", "mon", "month", "months").forEach { put(it, MINUTES_IN_MONTH) }
}

/** What Send does with a chat mode command: shows why it cannot go, or sends its change. */
internal sealed interface ChatModeCommandRun {
    data class Show(val error: ChatSendError) : ChatModeCommandRun

    data class Send(val change: ChatModeChange) : ChatModeCommandRun
}

/** The free build ([locked]) points to Premium; Premium explains a value it cannot take, or sends the change. */
internal fun chatModeCommandRun(command: ChatModeCommandInput, locked: Boolean): ChatModeCommandRun = when {
    locked -> ChatModeCommandRun.Show(ChatSendError.ChatModesPremium)
    command is ChatModeCommandInput.Change -> ChatModeCommandRun.Send(command.change)
    command is ChatModeCommandInput.Invalid && command.command == ChatCommand.Followers ->
        ChatModeCommandRun.Show(ChatSendError.FollowersUsage)
    else -> ChatModeCommandRun.Show(ChatSendError.SlowUsage)
}

/**
 * A tap on the send error shows the Premium banner for a chat mode command or a `/shoutout` the free build holds;
 * null otherwise.
 */
internal fun sendErrorTap(error: ChatSendError?, control: ChatModeControl?): (() -> Unit)? =
    (control as? ChatModeControl.Locked)?.showPremium?.takeIf {
        error == ChatSendError.ChatModesPremium || error == ChatSendError.ShoutoutPremium
    }
