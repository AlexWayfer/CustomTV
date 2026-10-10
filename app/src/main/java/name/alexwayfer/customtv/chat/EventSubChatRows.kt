package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** A chat row built from an EventSub chat message, for messages that IRC does not deliver. */
internal fun eventSubChatRow(
    id: String,
    event: JSONObject,
    message: JSONObject,
    kind: ChatEventKind,
    knownColors: Map<String, Color>,
    timestampMillis: Long,
): ChatMessage? {
    val login = event.eventSubText("user_login")?.lowercase() ?: return null
    val displayName = event.eventSubText("user_name") ?: login
    val parts = eventSubMessageParts(message.optJSONArray("fragments"), message.optString("text"))
    return ChatMessage(
        id = id,
        userLogin = login,
        displayName = displayName,
        color = knownColors[login] ?: knownColors[displayName.lowercase()] ?: IrcMessageParser.nameColor(displayName),
        rawText = message.optString("text"),
        parts = parts,
        timestampMillis = timestampMillis,
        eventKind = kind,
        userId = event.eventSubText("user_id"),
    )
}

/** EventSub message fragments as chat parts: Twitch emotes become images, everything else stays text. */
internal fun eventSubMessageParts(fragments: JSONArray?, text: String): List<ChatPart> {
    if (fragments == null || fragments.length() == 0) {
        return if (text.isEmpty()) emptyList() else listOf(ChatPart.Text(text))
    }
    val parts = mutableListOf<ChatPart>()
    for (index in 0 until fragments.length()) {
        val fragment = fragments.optJSONObject(index) ?: continue
        val fragmentText = fragment.optString("text")
        val emoteId = fragment.optJSONObject("emote")?.eventSubText("id")
        when {
            fragment.optString("type") == "emote" && emoteId != null ->
                parts += ChatPart.Emote(fragmentText, twitchEmoteUrl(emoteId))
            fragmentText.isEmpty() -> Unit
            parts.lastOrNull() is ChatPart.Text ->
                parts[parts.lastIndex] = ChatPart.Text((parts.last() as ChatPart.Text).text + fragmentText)
            else -> parts += ChatPart.Text(fragmentText)
        }
    }
    return parts
}

/** Adds a row once; a repeated notification for the same message keeps the row already shown. */
internal fun withChatRowOnce(messages: List<ChatMessage>, row: ChatMessage): List<ChatMessage> =
    if (messages.any { it.id == row.id }) messages else messages + row

internal fun JSONObject.eventSubText(name: String): String? =
    if (isNull(name)) null else optString(name).trim().takeIf { it.isNotEmpty() }
