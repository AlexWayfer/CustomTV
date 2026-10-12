package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue
import name.alexwayfer.customtv.data.textOrNull

object IrcMessageParser {
    private val nameColors = listOf(
        Color(0xFFFF0000),
        Color(0xFF0000FF),
        Color(0xFF00FF00),
        Color(0xFFB22222),
        Color(0xFFFF7F50),
        Color(0xFF9ACD32),
        Color(0xFFFF4500),
        Color(0xFF2E8B57),
        Color(0xFFDAA520),
        Color(0xFFD2691E),
        Color(0xFF5F9EA0),
        Color(0xFF1E90FF),
        Color(0xFFFF69B4),
        Color(0xFF8A2BE2),
        Color(0xFF00FF7F),
    )

    fun parsePrivMsg(raw: String): ChatMessage? {
        val parsed = IrcLineParser.parse(raw) ?: return null
        if (parsed.command != "PRIVMSG") return null
        val text = parsed.trailing ?: return null
        val login = parsed.prefix
            ?.substringBefore('!')
            ?.removePrefix(":")
            ?.lowercase()
            .orEmpty()
        val displayName = parsed.tag("display-name") ?: login
        val reply = parseReply(parsed.tags)
        val stripped = strippedPrivMsgText(text, reply)
        val emotesTag = IrcEmoteParser.shiftEmoteTag(parsed.tags["emotes"].orEmpty(), stripped.emoteShift)
        val gifsTag = IrcEmoteParser.shiftGifTag(parsed.tags["gifs"].orEmpty(), stripped.emoteShift)
        val parts = IrcEmoteParser.parse(stripped.text, emotesTag, gifsTag).filterNot { part ->
            part is ChatPart.Text && part.text.isEmpty()
        }
        val rewardId = parsed.tag("custom-reward-id")
        return ChatMessage(
            id = parsed.tag("id") ?: "${login}-${System.nanoTime()}",
            userId = parsed.tag("user-id"),
            userLogin = login,
            displayName = displayName,
            color = parseColor(parsed.tags["color"]) ?: nameColor(displayName.ifEmpty { login }),
            rawText = stripped.text,
            parts = parts,
            timestampMillis = parsed.sentAtMillis(),
            badges = parseBadges(parsed.tags["badges"]),
            eventKind = privMsgEventKind(parsed.tags["msg-id"].orEmpty(), rewardId),
            reply = reply,
            reward = rewardId?.let { ChatReward(id = it, title = "", cost = 0) },
            isAction = stripped.isAction,
            firstInChannel = parsed.tags["first-msg"] == "1",
            cheerBits = parsed.tags["bits"]?.toIntOrNull()?.takeIf { it > 0 },
        )
    }

    fun parseUserNotice(raw: String): ChatMessage? {
        val parsed = IrcLineParser.parse(raw) ?: return null
        if (parsed.command != "USERNOTICE") return null
        val msgId = parsed.tags["msg-id"].orEmpty()
        val text = parsed.trailing?.takeIf { it.isNotBlank() }
        val login = parsed.tag("login")?.lowercase().orEmpty()
        val displayName = parsed.tag("display-name") ?: login
        val watchStreak = IrcUserNotice.watchStreak(parsed.tags)
        val systemText = userNoticeSystemText(parsed, displayName, watchStreak)
        if (systemText == null && text == null) return null
        val eventKind = if (watchStreak != null) ChatEventKind.WatchStreak else IrcUserNotice.eventKind(msgId)
        return ChatMessage(
            id = parsed.tag("id") ?: "usernotice-$msgId-${System.nanoTime()}",
            userId = parsed.tag("user-id"),
            userLogin = login,
            displayName = displayName,
            color = parseColor(parsed.tags["color"]) ?: nameColor(displayName.ifEmpty { login }),
            rawText = listOfNotNull(systemText, text).joinToString(" "),
            parts = text?.let {
                IrcEmoteParser.parse(it, parsed.tags["emotes"].orEmpty(), parsed.tags["gifs"].orEmpty())
            }.orEmpty(),
            timestampMillis = parsed.sentAtMillis(),
            badges = parseBadges(parsed.tags["badges"]),
            eventKind = eventKind,
            systemText = systemText,
            accentColor = announcementAccent(parsed, eventKind),
            watchStreak = watchStreak,
            raid = IrcUserNotice.raid(parsed.tags),
            communityGift = IrcUserNotice.communityGift(parsed.tags),
            primeSubscription = isPrimeSubscription(parsed, eventKind, msgId),
        )
    }

    fun parseNotice(raw: String): ChatMessage? {
        val parsed = IrcLineParser.parse(raw) ?: return null
        if (parsed.command != "NOTICE") return null
        val text = parsed.trailing?.takeIf { it.isNotBlank() } ?: return null
        val msgId = parsed.tags["msg-id"].orEmpty()
        val id = parsed.tags["id"]?.takeIf { it.isNotBlank() }
            ?: "notice-${msgId.ifBlank { "room" }}-${System.nanoTime()}"
        return ChatMessage(
            id = id,
            userLogin = "",
            displayName = "",
            color = Color.Unspecified,
            rawText = text,
            parts = emptyList(),
            timestampMillis = parsed.tags["tmi-sent-ts"]?.toLongOrNull()
                ?: System.currentTimeMillis(),
            eventKind = ChatEventKind.System,
            systemText = text,
        )
    }

    fun parseClearMsg(raw: String): ChatMessage? {
        val parsed = IrcLineParser.parse(raw) ?: return null
        if (parsed.command != "CLEARMSG") return null
        val targetId = parsed.tags["target-msg-id"]?.takeIf { it.isNotBlank() } ?: return null
        return ChatMessage(
            id = targetId,
            userLogin = parsed.tags["login"].orEmpty().lowercase(),
            displayName = "",
            color = Color.Unspecified,
            rawText = parsed.trailing.orEmpty(),
            parts = emptyList(),
            timestampMillis = parsed.tags["tmi-sent-ts"]?.toLongOrNull()
                ?: System.currentTimeMillis(),
            eventKind = ChatEventKind.MessageDeleted,
            deleted = true,
            deletedBy = deletedByFromTags(parsed.tags),
        )
    }

    fun parseClearChat(raw: String): ChatMessage? {
        val parsed = IrcLineParser.parse(raw) ?: return null
        if (parsed.command != "CLEARCHAT") return null
        val login = parsed.trailing?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val timeoutSeconds = parsed.tags["ban-duration"]?.toLongOrNull()?.takeIf { it > 0 }
        return ChatMessage(
            id = "clearchat-$login-${parsed.tags["tmi-sent-ts"].orEmpty()}",
            userLogin = login,
            displayName = login,
            color = Color.Unspecified,
            rawText = "",
            parts = emptyList(),
            timestampMillis = parsed.tags["tmi-sent-ts"]?.toLongOrNull()
                ?: System.currentTimeMillis(),
            eventKind = ChatEventKind.UserMessagesDeleted,
            deleted = true,
            deletedBy = deletedByFromTags(parsed.tags),
            timeoutSeconds = timeoutSeconds,
            banned = timeoutSeconds == null,
        )
    }

    fun parseReply(tags: Map<String, String>): ChatReply? {
        val parentId = tags["reply-parent-msg-id"]?.takeIf { it.isNotBlank() } ?: return null
        val login = tags["reply-parent-user-login"].orEmpty()
        val displayName = tags["reply-parent-display-name"]
            ?.takeIf { it.isNotBlank() }
            ?: login
        val body = tags["reply-parent-msg-body"].orEmpty()
        if (displayName.isBlank() && body.isBlank()) return null
        val action = IrcMessageText.stripCtcpAction(body)
        return ChatReply(
            parentMsgId = parentId,
            parentUserLogin = login,
            parentDisplayName = displayName,
            parentBody = action.text,
            parentIsAction = action.isAction,
            threadParentMsgId = tags["reply-thread-parent-msg-id"].orEmpty(),
            threadParentUserLogin = tags["reply-thread-parent-user-login"].orEmpty(),
        )
    }

    fun parseBadges(raw: String?): List<ChatBadge> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { token ->
            val slash = token.indexOf('/')
            if (slash <= 0 || slash == token.lastIndex) return@mapNotNull null
            ChatBadge(
                setId = token.substring(0, slash),
                version = token.substring(slash + 1),
            )
        }
    }

    fun nameColor(name: String): Color {
        val index = nameColorIndex(name.lowercase().hashCode(), nameColors.size)
        return nameColors[index]
    }

    private fun deletedByFromTags(tags: Map<String, String>): String? {
        val name = tags["deleted-by"]
            ?: tags["moderator-login"]
            ?: tags["moderator"]
        return name.textOrNull()
    }

    private fun parseColor(raw: String?): Color? {
        if (raw.isNullOrBlank() || !raw.startsWith('#')) return null
        return opaqueArgbFromHex(raw)?.let { Color(it) }
    }

}

/** A tag's value, or null when it is missing or blank. */
private fun IrcLine.tag(name: String): String? = tags[name]?.takeIf { it.isNotBlank() }

/** When the server sent the line, or now for a line without the time. */
private fun IrcLine.sentAtMillis(): Long = tags["tmi-sent-ts"]?.toLongOrNull() ?: System.currentTimeMillis()

/** A message's text as shown, and how many code points its emote positions move by after what was cut. */
private class StrippedText(val text: String, val emoteShift: Int, val isAction: Boolean)

/** The text without the `/me` wrapping and, in a reply, without the leading mention of the parent's author. */
private fun strippedPrivMsgText(text: String, reply: ChatReply?): StrippedText {
    val action = IrcMessageText.stripCtcpAction(text)
    val shown = reply?.let { IrcMessageText.stripLeadingReplyMention(action.text, it) } ?: action.text
    val mentionCodePoints = action.text.codePointCount(0, action.text.length - shown.length)
    return StrippedText(shown, action.prefixCodePoints + mentionCodePoints, action.isAction)
}

private fun privMsgEventKind(msgId: String, rewardId: String?): ChatEventKind = when {
    msgId == "highlighted-message" -> ChatEventKind.Highlight
    rewardId != null -> ChatEventKind.Reward
    else -> ChatEventKind.Normal
}

/** The line Twitch writes for the event; a watch streak gets the app's own. */
private fun userNoticeSystemText(parsed: IrcLine, displayName: String, watchStreak: ChatWatchStreak?): String? =
    if (watchStreak != null) {
        IrcUserNotice.watchStreakSystemText(displayName, watchStreak.consecutiveStreams)
    } else {
        parsed.tag("system-msg")
    }

private fun announcementAccent(parsed: IrcLine, eventKind: ChatEventKind): Color? =
    if (eventKind == ChatEventKind.Announcement) {
        IrcUserNotice.announcementColor(parsed.tags["msg-param-color"])
    } else {
        null
    }

private fun isPrimeSubscription(parsed: IrcLine, eventKind: ChatEventKind, msgId: String): Boolean =
    eventKind == ChatEventKind.Subscription &&
        msgId in setOf("sub", "resub") &&
        parsed.tags["msg-param-sub-plan"].equals("Prime", ignoreCase = true)

internal fun nameColorIndex(hash: Int, paletteSize: Int): Int {
    if (paletteSize <= 0) return 0
    if (hash == Int.MIN_VALUE) return 0
    return hash.absoluteValue % paletteSize
}

internal data class ChatGifSpan(
    val start: Int,
    val endInclusive: Int,
    val id: String,
    val url: String,
)
