package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.toJsonObjectOrNull

/**
 * A shoutout the channel [channelId] gave to [targetLogin], from the unofficial `shoutout.<channelId>` PubSub topic.
 * [sourceLogin] is who gave it, the broadcaster or a moderator; Twitch sends no display name for them.
 */
internal data class ChannelShoutout(
    val id: String,
    val channelId: String,
    val targetId: String,
    val targetLogin: String,
    val targetDisplayName: String,
    val targetAvatarUrl: String?,
    val sourceLogin: String?,
    val receivedAtMillis: Long,
)

/** How long the plaque shows, as on twitch.tv. */
internal const val SHOUTOUT_PLAQUE_MILLIS = 45_000L

/** The size put into the avatar URL template, whose `%s` stands for it. */
private const val SHOUTOUT_AVATAR_SIZE = "150x150"

internal fun parseShoutoutPubSubFrame(raw: String, nowMillis: Long): ChannelShoutout? {
    val frame = raw.toJsonObjectOrNull() ?: return null
    if (frame.optString("type") != "MESSAGE") return null
    val data = frame.optJSONObject("data") ?: return null
    if (!data.optString("topic").startsWith("shoutout.")) return null
    val message = data.text("message") ?: return null
    return parseShoutoutPubSubMessage(message, nowMillis)
}

/** The shoutout of a `create` message, which arrives at [nowMillis]; null for another type or a missing target. */
internal fun parseShoutoutPubSubMessage(raw: String, nowMillis: Long): ChannelShoutout? {
    val data = raw.toJsonObjectOrNull()
        ?.takeIf { it.optString("type") == "create" }
        ?.optJSONObject("data")
        ?: return null
    val id = data.text("shoutoutID")
    val channelId = data.text("broadcasterUserID")
    val target = data.text("targetUserID")?.let { targetId ->
        data.text("targetLogin")?.lowercase()?.let { login -> targetId to login }
    }
    return if (id == null || channelId == null || target == null) {
        null
    } else {
        ChannelShoutout(
            id = id,
            channelId = channelId,
            targetId = target.first,
            targetLogin = target.second,
            targetDisplayName = data.text("targetUserDisplayName") ?: target.second,
            targetAvatarUrl = data.text("targetUserProfileImageURL")?.replace("%s", SHOUTOUT_AVATAR_SIZE),
            sourceLogin = data.text("sourceLogin")?.lowercase(),
            receivedAtMillis = nowMillis,
        )
    }
}

/** The [shoutout] when the open channel [channelId] gave it. */
internal fun shoutoutForChannel(shoutout: ChannelShoutout?, channelId: String?): ChannelShoutout? =
    shoutout?.takeIf { channelId != null && it.channelId == channelId }

internal fun shoutoutPlaqueRemainingMillis(shoutout: ChannelShoutout, nowMillis: Long): Long =
    (shoutout.receivedAtMillis + SHOUTOUT_PLAQUE_MILLIS - nowMillis).coerceIn(0L, SHOUTOUT_PLAQUE_MILLIS)

/** The [latest] shoutout while its plaque lasts, unless the viewer hid it. */
internal fun visibleShoutout(latest: ChannelShoutout?, hiddenId: String?, nowMillis: Long): ChannelShoutout? = latest
    ?.takeUnless { it.id == hiddenId }
    ?.takeIf { shoutoutPlaqueRemainingMillis(it, nowMillis) > 0L }

/** A shoutout's line in the chat: [sourceName] gave it to [targetName], whose card the name opens. */
data class ChatShoutoutNotice(
    val targetLogin: String,
    val targetId: String,
    val targetName: String,
    val sourceName: String?,
)

/** The chat line of [shoutout] at the time it arrived, as twitch.tv adds one under the plaque. */
internal fun shoutoutChatRow(shoutout: ChannelShoutout, sourceName: String?) = ChatMessage(
    id = "shoutout-${shoutout.id}",
    userLogin = "",
    displayName = "",
    color = Color.Unspecified,
    rawText = "",
    parts = emptyList(),
    timestampMillis = shoutout.receivedAtMillis,
    eventKind = ChatEventKind.System,
    shoutoutNotice = ChatShoutoutNotice(
        targetLogin = shoutout.targetLogin,
        targetId = shoutout.targetId,
        targetName = shoutout.targetDisplayName,
        sourceName = sourceName,
    ),
)

/** Who gave the shoutout: their display name from this chat's [chatters] (login to display name), or the login. */
internal fun shoutoutSourceName(sourceLogin: String?, chatters: Map<String, String>): String? =
    sourceLogin?.let { login -> chatters[login]?.takeIf { it.isNotBlank() } ?: login }
