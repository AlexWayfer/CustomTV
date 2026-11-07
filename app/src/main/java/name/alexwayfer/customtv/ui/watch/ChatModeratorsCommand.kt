package name.alexwayfer.customtv.ui.watch

/** Only the command itself opens the list; names or other text are not discarded as a command. */
internal fun isModeratorsCommand(text: String): Boolean =
    text.trim().equals(ChatCommand.Moderators.word, ignoreCase = true)

internal enum class ChatMenuStart { Rows, Modes, Moderators }
