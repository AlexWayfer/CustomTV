package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.auth.twitchErrorMessage

internal const val CHAT_MESSAGE_MAX_LENGTH = 500

internal fun chatMessageToSend(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.take(CHAT_MESSAGE_MAX_LENGTH)
}

internal sealed interface ChatSendResult {
    data object Sent : ChatSendResult
    /** Twitch took the request but did not post the message; [message] is its reason, written for the user. */
    data class Dropped(val code: String, val message: String) : ChatSendResult
    /** Twitch refused the request; [message] is its error text. */
    data class Rejected(val httpCode: Int, val message: String) : ChatSendResult
    data object Unavailable : ChatSendResult
}

internal fun parseChatSendResponse(httpCode: Int, body: String): ChatSendResult {
    if (httpCode in HTTP_CLIENT_ERROR) return ChatSendResult.Rejected(httpCode, twitchErrorMessage(body))
    if (httpCode !in HTTP_SUCCESS) return ChatSendResult.Unavailable
    val message = body.toJsonObjectOrNull()
        ?.optJSONArray("data")
        ?.optJSONObject(0)
        ?: return ChatSendResult.Unavailable
    if (message.optBoolean("is_sent")) return ChatSendResult.Sent
    val reason = message.optJSONObject("drop_reason")
    return ChatSendResult.Dropped(
        code = reason?.optString("code").orEmpty(),
        message = reason?.trimmedText("message").orEmpty(),
    )
}

/** What the user reads when a message or a chat mode command did not go through. */
internal sealed interface ChatSendError {
    data object LogIn : ChatSendError
    data object NotAllowed : ChatSendError
    data object TooLong : ChatSendError
    data object TooFast : ChatSendError
    /** Twitch's own words, such as why a followers-only or slow mode chat dropped the message. */
    data class Twitch(val message: String) : ChatSendError
    data object Failed : ChatSendError

    /** `/slow` with something other than a number of seconds. */
    data object SlowUsage : ChatSendError

    /** `/followers` with a time it does not understand. */
    data object FollowersUsage : ChatSendError

    /** Twitch could not be reached to change a chat mode. */
    data object ChatModeFailed : ChatSendError

    /** The free build: a chat mode command waits for Premium; a tap on the line shows the Premium banner. */
    data object ChatModesPremium : ChatSendError

    /** `/shoutout` without a nick that can be a channel. */
    data object ShoutoutUsage : ChatSendError

    /** A shoutout needs the channel giving it to be live. */
    data object ShoutoutOffline : ChatSendError

    /** Twitch allows a shoutout every 2 minutes, and one to the same channel every hour. */
    data object ShoutoutTooOften : ChatSendError

    /** No channel goes by the nick of a `/shoutout`. */
    data object ShoutoutNoChannel : ChatSendError

    /** A channel cannot give a shoutout to itself. */
    data object ShoutoutOwnChannel : ChatSendError

    /** Twitch could not be reached to give a shoutout. */
    data object ShoutoutFailed : ChatSendError

    /** The free build: `/shoutout` waits for Premium; a tap on the line shows the Premium banner. */
    data object ShoutoutPremium : ChatSendError
}

/**
 * A dropped message keeps Twitch's reason, which already names the chat mode and its wait. A
 * refused request gets the app's words for the cases Twitch documents; its own text is for developers.
 */
internal fun chatSendError(result: ChatSendResult): ChatSendError? = when (result) {
    ChatSendResult.Sent -> null
    is ChatSendResult.Dropped -> result.message.takeIf { it.isNotEmpty() }?.let(ChatSendError::Twitch)
        ?: ChatSendError.Failed
    is ChatSendResult.Rejected -> when (result.httpCode) {
        HTTP_UNAUTHORIZED -> ChatSendError.LogIn
        HTTP_FORBIDDEN -> ChatSendError.NotAllowed
        HTTP_UNPROCESSABLE_CONTENT -> ChatSendError.TooLong
        HTTP_TOO_MANY_REQUESTS -> ChatSendError.TooFast
        else -> result.message.takeIf { it.isNotEmpty() }?.let(ChatSendError::Twitch) ?: ChatSendError.Failed
    }
    ChatSendResult.Unavailable -> ChatSendError.Failed
}
