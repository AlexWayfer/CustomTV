package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.auth.twitchErrorMessage
import org.json.JSONObject

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
    if (httpCode in 400..499) return ChatSendResult.Rejected(httpCode, twitchErrorMessage(body))
    if (httpCode !in 200..299) return ChatSendResult.Unavailable
    val message = runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONArray("data")
        ?.optJSONObject(0)
        ?: return ChatSendResult.Unavailable
    if (message.optBoolean("is_sent")) return ChatSendResult.Sent
    val reason = message.optJSONObject("drop_reason")
    return ChatSendResult.Dropped(
        code = reason?.optString("code").orEmpty(),
        message = reason?.optString("message")?.trim()?.takeIf { it != "null" }.orEmpty(),
    )
}

/** What the user reads when a message did not post. */
internal sealed interface ChatSendError {
    data object LogIn : ChatSendError
    data object NotAllowed : ChatSendError
    data object TooLong : ChatSendError
    data object TooFast : ChatSendError
    /** Twitch's own words, such as why a followers-only or slow mode chat dropped the message. */
    data class Twitch(val message: String) : ChatSendError
    data object Failed : ChatSendError
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
        401 -> ChatSendError.LogIn
        403 -> ChatSendError.NotAllowed
        422 -> ChatSendError.TooLong
        429 -> ChatSendError.TooFast
        else -> result.message.takeIf { it.isNotEmpty() }?.let(ChatSendError::Twitch) ?: ChatSendError.Failed
    }
    ChatSendResult.Unavailable -> ChatSendError.Failed
}
