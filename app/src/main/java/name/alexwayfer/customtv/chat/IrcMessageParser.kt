package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

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
        val parsed = parseLine(raw) ?: return null
        if (parsed.command != "PRIVMSG") return null
        val text = parsed.trailing ?: return null
        val login = parsed.prefix
            ?.substringBefore('!')
            ?.removePrefix(":")
            ?.lowercase()
            .orEmpty()
        val displayName = parsed.tags["display-name"]
            ?.takeIf { it.isNotBlank() }
            ?: login
        val color = parseColor(parsed.tags["color"]) ?: nameColor(displayName.ifEmpty { login })
        val id = parsed.tags["id"]?.takeIf { it.isNotBlank() }
            ?: "${login}-${System.nanoTime()}"
        val timestampMillis = parsed.tags["tmi-sent-ts"]?.toLongOrNull()
            ?: System.currentTimeMillis()
        val reply = parseReply(parsed.tags)
        val action = stripCtcpAction(text)
        var displayText = action.text
        var emoteShift = action.prefixCodePoints
        if (reply != null) {
            val withoutMention = stripLeadingReplyMention(displayText, reply)
            if (withoutMention.length != displayText.length) {
                emoteShift += displayText.codePointCount(
                    0,
                    displayText.length - withoutMention.length,
                )
                displayText = withoutMention
            }
        }
        val emotesTag = shiftEmoteTag(parsed.tags["emotes"].orEmpty(), emoteShift)
        val gifsTag = shiftGifTag(parsed.tags["gifs"].orEmpty(), emoteShift)
        val parts = parseEmotes(displayText, emotesTag, gifsTag).filterNot { part ->
            part is ChatPart.Text && part.text.isEmpty()
        }
        val badges = parseBadges(parsed.tags["badges"])
        val msgId = parsed.tags["msg-id"].orEmpty()
        val rewardId = parsed.tags["custom-reward-id"]?.takeIf { it.isNotBlank() }
        val eventKind = when {
            msgId == "highlighted-message" -> ChatEventKind.Highlight
            rewardId != null -> ChatEventKind.Reward
            else -> ChatEventKind.Normal
        }
        return ChatMessage(
            id = id,
            userId = parsed.tags["user-id"]?.takeIf { it.isNotBlank() },
            userLogin = login,
            displayName = displayName,
            color = color,
            rawText = displayText,
            parts = parts,
            timestampMillis = timestampMillis,
            badges = badges,
            eventKind = eventKind,
            reply = reply,
            reward = rewardId?.let { ChatReward(id = it, title = "", cost = 0) },
            isAction = action.isAction,
            firstInChannel = parsed.tags["first-msg"] == "1",
            cheerBits = parsed.tags["bits"]?.toIntOrNull()?.takeIf { it > 0 },
        )
    }

    fun parseUserNotice(raw: String): ChatMessage? {
        val parsed = parseLine(raw) ?: return null
        if (parsed.command != "USERNOTICE") return null
        val msgId = parsed.tags["msg-id"].orEmpty()
        val text = parsed.trailing?.takeIf { it.isNotBlank() }
        val login = parsed.tags["login"]
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
            .orEmpty()
        val displayName = parsed.tags["display-name"]
            ?.takeIf { it.isNotBlank() }
            ?: login
        val watchStreak = parseWatchStreak(parsed.tags)
        val raid = parseRaid(parsed.tags)
        val communityGift = parseCommunityGift(parsed.tags)
        val systemText = if (watchStreak != null) {
            formatWatchStreakSystemText(displayName, watchStreak.consecutiveStreams)
        } else {
            parsed.tags["system-msg"]?.takeIf { it.isNotBlank() }
        }
        if (systemText == null && text == null) return null
        val color = parseColor(parsed.tags["color"]) ?: nameColor(displayName.ifEmpty { login })
        val id = parsed.tags["id"]?.takeIf { it.isNotBlank() }
            ?: "usernotice-$msgId-${System.nanoTime()}"
        val timestampMillis = parsed.tags["tmi-sent-ts"]?.toLongOrNull()
            ?: System.currentTimeMillis()
        val parts = if (text != null) {
            parseEmotes(text, parsed.tags["emotes"].orEmpty(), parsed.tags["gifs"].orEmpty())
        } else {
            emptyList()
        }
        val eventKind = if (watchStreak != null) {
            ChatEventKind.WatchStreak
        } else {
            userNoticeKind(msgId)
        }
        val accent = if (eventKind == ChatEventKind.Announcement) {
            announcementColor(parsed.tags["msg-param-color"])
        } else {
            null
        }
        return ChatMessage(
            id = id,
            userId = parsed.tags["user-id"]?.takeIf { it.isNotBlank() },
            userLogin = login,
            displayName = displayName,
            color = color,
            rawText = listOfNotNull(systemText, text).joinToString(" "),
            parts = parts,
            timestampMillis = timestampMillis,
            badges = parseBadges(parsed.tags["badges"]),
            eventKind = eventKind,
            systemText = systemText,
            accentColor = accent,
            watchStreak = watchStreak,
            raid = raid,
            communityGift = communityGift,
            primeSubscription = eventKind == ChatEventKind.Subscription &&
                msgId in setOf("sub", "resub") &&
                parsed.tags["msg-param-sub-plan"].equals("Prime", ignoreCase = true),
        )
    }

    fun parseNotice(raw: String): ChatMessage? {
        val parsed = parseLine(raw) ?: return null
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
        val parsed = parseLine(raw) ?: return null
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
        val parsed = parseLine(raw) ?: return null
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
        val action = stripCtcpAction(body)
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

    internal fun stripCtcpAction(text: String): CtcpActionStrip {
        return IrcMessageText.stripCtcpAction(text)
    }

    internal fun stripLeadingReplyMention(text: String, reply: ChatReply): String {
        return IrcMessageText.stripLeadingReplyMention(text, reply)
    }

    internal fun shiftEmoteTag(emotesTag: String, shift: Int): String {
        return IrcEmoteParser.shiftEmoteTag(emotesTag, shift)
    }

    internal fun shiftGifTag(gifsTag: String, shift: Int): String {
        return IrcEmoteParser.shiftGifTag(gifsTag, shift)
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

    fun parseLine(raw: String): IrcLine? {
        return IrcLineParser.parse(raw)
    }

    fun parseEmotes(message: String, emotesTag: String, gifsTag: String = ""): List<ChatPart> {
        return IrcEmoteParser.parse(message, emotesTag, gifsTag)
    }

    fun nameColor(name: String): Color {
        val index = nameColorIndex(name.lowercase().hashCode(), nameColors.size)
        return nameColors[index]
    }

    private fun userNoticeKind(msgId: String): ChatEventKind {
        return IrcUserNotice.eventKind(msgId)
    }

    internal fun parseWatchStreak(tags: Map<String, String>): ChatWatchStreak? {
        return IrcUserNotice.watchStreak(tags)
    }

    internal fun parseRaid(tags: Map<String, String>): ChatRaid? {
        return IrcUserNotice.raid(tags)
    }

    internal fun parseCommunityGift(tags: Map<String, String>): ChatCommunityGift? {
        return IrcUserNotice.communityGift(tags)
    }

    internal fun formatWatchStreakSystemText(displayName: String, consecutiveStreams: Int): String {
        return IrcUserNotice.watchStreakSystemText(displayName, consecutiveStreams)
    }

    internal fun announcementColor(raw: String?): Color {
        return IrcUserNotice.announcementColor(raw)
    }

    private fun deletedByFromTags(tags: Map<String, String>): String? {
        val name = tags["deleted-by"]
            ?: tags["moderator-login"]
            ?: tags["moderator"]
        return name?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun parseColor(raw: String?): Color? {
        if (raw.isNullOrBlank() || !raw.startsWith('#') || raw.length != 7) return null
        return try {
            val r = raw.substring(1, 3).toInt(16)
            val g = raw.substring(3, 5).toInt(16)
            val b = raw.substring(5, 7).toInt(16)
            Color(red = r, green = g, blue = b)
        } catch (_: NumberFormatException) {
            null
        }
    }

}

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
