package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

/** Rows a chat keeps on screen, live or replayed. */
internal const val MAX_CHAT_MESSAGES = 200

data class ChatMessage(
    val id: String,
    val userLogin: String,
    val displayName: String,
    val color: Color,
    val rawText: String,
    val parts: List<ChatPart>,
    val timestampMillis: Long,
    val notice: ChatNotice? = null,
    val badges: List<ChatBadge> = emptyList(),
    val eventKind: ChatEventKind = ChatEventKind.Normal,
    val systemText: String? = null,
    val accentColor: Color? = null,
    val reply: ChatReply? = null,
    val reward: ChatReward? = null,
    val deleted: Boolean = false,
    val deletedBy: String? = null,
    val timeoutSeconds: Long? = null,
    val banned: Boolean = false,
    val isAction: Boolean = false,
    val watchStreak: ChatWatchStreak? = null,
    val raid: ChatRaid? = null,
    val communityGift: ChatCommunityGift? = null,
    val emoteChange: ChatEmoteChange? = null,
    val firstInSession: Boolean = false,
    val firstInChannel: Boolean = false,
    val cheerBits: Int? = null,
    val primeSubscription: Boolean = false,
    val userId: String? = null,
    val moderationHold: ChatModerationHold? = null,
    val autoModNotice: AutoModNotice? = null,
    val moderationNotice: ChatModerationNotice? = null,
    val warningNotice: ChatWarningNotice? = null,
)

enum class ChatEventKind {
    Normal,
    Announcement,
    Subscription,
    Raid,
    Highlight,
    Reward,
    WatchStreak,
    EmoteChange,
    System,
    MessageDeleted,
    UserMessagesDeleted,

    /** A message AutoMod or a blocked term held for moderators. */
    ModerationHold,
}

data class ChatReward(
    val id: String,
    val title: String,
    val cost: Int,
    val backgroundColorHex: String? = null,
    val imageUrl: String? = null,
    val prompt: String? = null,
) {
    val giantEmote: Boolean
        get() = FfzGiantEmote.isMarked(title) || FfzGiantEmote.isMarked(prompt)
}

data class ChatWatchStreak(
    val consecutiveStreams: Int,
    val points: Int? = null,
)

data class ChatRaid(
    val viewerCount: Int,
    val fromDisplayName: String,
    val canceled: Boolean = false,
    val created: Boolean = false,
)

data class ChatCommunityGift(
    val count: Int,
    val tier: Int?,
    val gifterDisplayName: String?,
    val cumulativeCount: Int?,
    val anonymous: Boolean,
)

data class ChatBadge(
    val setId: String,
    val version: String,
) {
    val key: String get() = "$setId/$version"
}

data class ChatReply(
    val parentMsgId: String,
    val parentUserLogin: String,
    val parentDisplayName: String,
    val parentBody: String,
    val parentIsAction: Boolean = false,
    val threadParentMsgId: String = "",
    val threadParentUserLogin: String = "",
)

fun ChatMessage.copyableText(): String? {
    if (notice != null) return null
    val body = rawText.trim().ifEmpty { systemText?.trim().orEmpty() }
    val mention = reply?.let { replyMention(it) }
    val text = when {
        mention == null -> body
        body.startsWith(mention, ignoreCase = true) -> body
        body.isEmpty() -> mention
        else -> "$mention $body"
    }
    return text.takeIf { it.isNotEmpty() }
}

internal fun ChatMessage.copyableTextWithAuthor(timestamp: String): String? {
    val body = copyableText() ?: return null
    val name = displayName.trim().ifEmpty { userLogin.trim() }
    val clock = timestamp.trim()
    return buildString {
        if (clock.isNotEmpty()) {
            append(clock)
            append(' ')
        }
        if (name.isNotEmpty()) {
            append(name)
            append(": ")
        }
        append(body)
    }
}

private fun replyMention(reply: ChatReply): String? {
    val tag = reply.parentDisplayName.trim().ifEmpty { reply.parentUserLogin.trim() }
    return if (tag.isEmpty()) null else "@$tag"
}

enum class ChatNotice {
    Connecting,
    Welcome,
    Disconnected,
    Reconnecting,
    Connected,
    RecentChatFailed,
    ChannelNotFound,
}

fun chatNoticeMessage(notice: ChatNotice, id: String): ChatMessage {
    return ChatMessage(
        id = id,
        userLogin = "",
        displayName = "",
        color = Color.Unspecified,
        rawText = notice.name,
        parts = emptyList(),
        timestampMillis = System.currentTimeMillis(),
        notice = notice,
    )
}

internal fun replaceOrAppendReconnectNotice(
    messages: List<ChatMessage>,
    notice: ChatNotice,
    newId: String,
    maxMessages: Int,
): List<ChatMessage> {
    val last = messages.lastOrNull()
    return if (last?.notice == ChatNotice.Reconnecting) {
        messages.dropLast(1) + chatNoticeMessage(notice, last.id)
    } else {
        (messages + chatNoticeMessage(notice, newId)).takeLast(maxMessages)
    }
}

/**
 * The notice a live chat starts with, Connecting and then the welcome on joining; recent chat loaded from elsewhere
 * goes before it, and live messages after it.
 */
internal const val CHAT_WELCOME_NOTICE_ID = "system-initial"

internal fun replaceOrAppendWelcomeNotice(
    messages: List<ChatMessage>,
    initialNoticeId: String,
    maxMessages: Int,
): List<ChatMessage> {
    val welcome = chatNoticeMessage(ChatNotice.Welcome, initialNoticeId)
    return if (messages.any { it.id == initialNoticeId }) {
        messages.map { message ->
            if (message.id == initialNoticeId) welcome else message
        }
    } else {
        (messages + welcome).takeLast(maxMessages)
    }
}

internal fun insertRecentChatBeforeWelcome(
    messages: List<ChatMessage>,
    recent: List<ChatMessage>,
    initialNoticeId: String,
    maxMessages: Int,
): List<ChatMessage> {
    if (recent.isEmpty()) return messages
    val existingIds = messages.asSequence().map { it.id }.toHashSet()
    val additions = recent.distinctBy { it.id }.filter { it.id !in existingIds }
    if (additions.isEmpty()) return messages
    val boundary = messages.indexOfFirst { it.id == initialNoticeId }
    val merged = if (boundary >= 0) {
        messages.take(boundary) + additions + messages.drop(boundary)
    } else {
        additions + messages
    }
    return merged.takeLast(maxMessages)
}

internal fun markMessageDeleted(
    messages: List<ChatMessage>,
    messageId: String,
    deletedBy: String?,
): List<ChatMessage> = messages.map { message ->
    if (message.id == messageId) {
        message.copy(deleted = true, deletedBy = deletedBy ?: message.deletedBy)
    } else {
        message
    }
}

internal fun markUserMessagesDeleted(
    messages: List<ChatMessage>,
    userLogin: String,
    deletedBy: String?,
    timeoutSeconds: Long? = null,
    banned: Boolean = false,
): List<ChatMessage> {
    if (userLogin.isBlank()) return messages
    return messages.map { message ->
        if (message.notice == null && message.userLogin == userLogin) {
            message.copy(
                deleted = true,
                deletedBy = deletedBy ?: message.deletedBy,
                timeoutSeconds = timeoutSeconds,
                banned = banned,
            )
        } else {
            message
        }
    }
}

internal fun formatTimeoutDuration(seconds: Long): String {
    val days = seconds / 86_400
    val hours = seconds % 86_400 / 3_600
    val minutes = seconds % 3_600 / 60
    val remainingSeconds = seconds % 60
    return when {
        days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
        hours > 0 -> "%d:%02d:%02d".format(java.util.Locale.ROOT, hours, minutes, remainingSeconds)
        else -> "%d:%02d".format(java.util.Locale.ROOT, minutes, remainingSeconds)
    }
}

sealed class ChatPart {
    data class Text(val text: String) : ChatPart()
    data class Emote(
        val name: String,
        val url: String,
        val aspectRatio: Float = 1f,
        val overlays: List<Emote> = emptyList(),
        val effects: EmoteEffects = EmoteEffects(),
        val modifiers: List<EmoteModifier> = emptyList(),
    ) : ChatPart()
    data class Gif(
        val name: String,
        val url: String,
        val aspectRatio: Float = 16f / 9f,
    ) : ChatPart()
}

data class EmoteModifier(
    val name: String,
    val platform: EmotePlatform,
    val effects: EmoteEffects,
)

data class SevenTvEmote(
    val url: String,
    val aspectRatio: Float = 1f,
    val overlay: Boolean = false,
    val effects: EmoteEffects? = null,
    val id: String? = null,
    /** 7TV only: the keywords its author gave it. */
    val tags: List<String> = emptyList(),
    /** 7TV only: the emote's own name when the channel renamed it. */
    val originalName: String? = null,
)

enum class ChatConnectionState {
    Connecting,
    Connected,
    Reconnecting,
    Disconnected,
}

enum class EmotePlatform {
    SevenTv,
    Bttv,
    Ffz,
}

enum class EmoteChangeAction {
    Added,
    Removed,
    Renamed,
}

data class ChatEmoteChange(
    val platform: EmotePlatform,
    val action: EmoteChangeAction,
    val actorName: String,
    val actorColor: Color,
    val emoteName: String,
    val previousName: String? = null,
    val emote: SevenTvEmote? = null,
    val emoteId: String? = null,
)
