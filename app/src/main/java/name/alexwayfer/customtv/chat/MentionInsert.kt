package name.alexwayfer.customtv.chat

/** A message can be answered with a mention of its chatter when it has one: not a notice from Twitch or the app. */
internal fun chatMessageCanBeMentioned(message: ChatMessage): Boolean = message.userLogin.isNotBlank()

/**
 * The nick a mention of a chatter names: their display name when it spells their login, otherwise the login, since
 * Twitch does not match a localized display name to the chatter.
 */
internal fun chatMentionName(login: String, displayName: String): String =
    displayName.takeIf { it.equals(login, ignoreCase = true) } ?: login

/**
 * [text] with `@name` in place of the selection, apart from the words around it by spaces, and the cursor after the
 * space that follows it. Null when it would pass [maxLength].
 */
internal fun textWithInsertedMention(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    name: String,
    maxLength: Int,
): EmoteInsertion? {
    if (name.isBlank()) return null
    return textWithInsertedEmote(text, selectionStart, selectionEnd, "@${name.trim()}", maxLength)
}
