package name.alexwayfer.customtv.data

import org.json.JSONObject

/** Whose ID a subscription names next to the broadcaster. */
internal enum class EventSubCondition(val userField: String?) {
    Broadcaster(null),

    /** The signed-in user as a moderator of the channel: Twitch refuses it for anyone else. */
    Moderator("moderator_user_id"),

    /** The signed-in user as a chatter of the channel. */
    User("user_id"),
}

/** A subscription a channel socket asks for. */
internal data class EventSubType(
    val name: String,
    val version: String,
    val condition: EventSubCondition = EventSubCondition.Broadcaster,
)

internal val STREAM_STATUS_EVENTSUB_TYPES = listOf(
    EventSubType("channel.update", "2"),
    EventSubType("stream.online", "1"),
    EventSubType("stream.offline", "1"),
)

/** The signed-in user's own messages that AutoMod holds in the channel. */
internal val OWN_MESSAGE_HOLD_EVENTSUB_TYPES = listOf(
    EventSubType("channel.chat.user_message_hold", "1", EventSubCondition.User),
    EventSubType("channel.chat.user_message_update", "1", EventSubCondition.User),
)

internal enum class EventSubSubscribeOutcome {
    Subscribed,

    /** Twitch refused a moderator subscription: the user does not moderate this channel (any more). */
    NotModerator,

    /** The login was refused or, for a subscription that needs no permission, unexpectedly forbidden. */
    LoginRejected,
    Outdated,

    /**
     * Too many sockets with subscriptions on this login. Right after a reconnect Twitch still counts the
     * sockets that just died, so the subscription goes through once they are gone.
     */
    TransportLimit,
    Rejected,
    Failed,
}

/**
 * A 403 on a moderator subscription is the routine answer for a viewer who does not moderate the channel. It must not
 * close the socket: the stream status subscriptions on the same socket keep working.
 */
internal fun eventSubSubscribeOutcome(
    httpCode: Int,
    condition: EventSubCondition,
    requestedSession: String,
    currentSession: String?,
    message: String = "",
): EventSubSubscribeOutcome = when {
    httpCode in 200..299 -> EventSubSubscribeOutcome.Subscribed
    condition == EventSubCondition.Moderator && httpCode == 403 -> EventSubSubscribeOutcome.NotModerator
    // A user subscription needs no permission beyond the login, so the socket stays for stream status.
    condition == EventSubCondition.User && httpCode == 403 -> EventSubSubscribeOutcome.Rejected
    // A retry after the user became a moderator may repeat a subscription that is already active.
    condition == EventSubCondition.Moderator && httpCode == 409 -> EventSubSubscribeOutcome.Subscribed
    httpCode == 401 || httpCode == 403 -> EventSubSubscribeOutcome.LoginRejected
    httpCode in 400..499 -> when {
        eventSubAnswerOutdated(requestedSession, currentSession) -> EventSubSubscribeOutcome.Outdated
        eventSubTransportLimit(httpCode, message) -> EventSubSubscribeOutcome.TransportLimit
        else -> EventSubSubscribeOutcome.Rejected
    }
    else -> EventSubSubscribeOutcome.Failed
}

/** The subscription type of a `notification` or `revocation` message, or null for any other message. */
internal fun eventSubMessageSubscriptionType(raw: String, messageType: String): String? {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (root.optJSONObject("metadata")?.optString("message_type") != messageType) return null
    return root.optJSONObject("payload")
        ?.optJSONObject("subscription")
        ?.optString("type")
        ?.takeIf { it.isNotBlank() }
}

/** Whether a refused subscription hit the limit of sockets per login, by the message Twitch sends with the 429. */
internal fun eventSubTransportLimit(httpCode: Int, message: String): Boolean =
    httpCode == 429 && message.contains("websocket transports limit", ignoreCase = true)

/** Twitch's `keepalive_timeout_seconds` when the welcome does not name one. */
private const val TWITCH_DEFAULT_KEEPALIVE_SECONDS = 10L

/** The `keepalive_timeout_seconds` of a `session_welcome` message, or null for any other message. */
internal fun eventSubKeepaliveSeconds(raw: String): Long? {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (root.optJSONObject("metadata")?.optString("message_type") != "session_welcome") return null
    val seconds = root.optJSONObject("payload")?.optJSONObject("session")?.optLong("keepalive_timeout_seconds", 0L) ?: 0L
    return seconds.takeIf { it > 0L }
}

/** How much longer than the keepalive timeout a socket may stay silent, for the network delay of the keepalive itself. */
private const val EVENTSUB_SILENCE_MARGIN_SECONDS = 5L

/**
 * How long a socket may go without any message before it counts as dead. Twitch sends a message at least every
 * keepalive timeout, so a longer silence means a connection that died without an error, such as after a network change.
 */
internal fun eventSubSilenceTimeoutSeconds(keepaliveSeconds: Long?): Long =
    (keepaliveSeconds?.takeIf { it > 0L } ?: TWITCH_DEFAULT_KEEPALIVE_SECONDS) + EVENTSUB_SILENCE_MARGIN_SECONDS

/**
 * How long to wait before asking again for a subscription refused for the socket limit, after
 * [attempt] earlier refusals: a keepalive timeout first, the time Twitch takes to notice a dead
 * socket, then twice as long each time.
 */
internal fun eventSubTransportRetrySeconds(keepaliveSeconds: Long?, attempt: Int): Long {
    val base = keepaliveSeconds?.takeIf { it > 0L } ?: TWITCH_DEFAULT_KEEPALIVE_SECONDS
    return base shl attempt.coerceIn(0, 20)
}
