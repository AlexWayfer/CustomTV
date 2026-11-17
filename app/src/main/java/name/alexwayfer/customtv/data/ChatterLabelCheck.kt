package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONObject

internal data class ChatterCheckChannel(val login: String, val id: String?)

internal data class ChatterCheckMessage(
    val text: String,
    val timestamp: String,
    val id: String,
    val channelId: String,
)

internal sealed interface ChatterCheckResult {
    data class Done(val text: UiText, val messages: List<ChatterCheckMessage> = emptyList()) : ChatterCheckResult
    data class Failed(val text: UiText) : ChatterCheckResult
    data object TwitchLoginRequired : ChatterCheckResult
}

internal enum class ChatterCheckTwitchRequirement { None, Session, Browser }

private val twitchHelper = Regex("""\btwitch\s*(?:\??\.\s*(?:gql|getUser)\b|(?:\?\.)?\s*\[)|=\s*twitch\b""")
private val twitchBrowser = Regex(
    """\b(?:document|chrome|browser)\s*\??\.\s*cookies?\b|\bauth-token\b|\bgql\.twitch\.tv\b""",
)

internal fun chatterCheckTwitchRequirement(script: String): ChatterCheckTwitchRequirement = when {
    twitchBrowser.containsMatchIn(script) -> ChatterCheckTwitchRequirement.Browser
    twitchHelper.containsMatchIn(script) -> ChatterCheckTwitchRequirement.Session
    else -> ChatterCheckTwitchRequirement.None
}

internal fun parseChatterCheckResult(json: JSONObject): ChatterCheckResult = when (json.optString("kind")) {
    "twitch-login" -> ChatterCheckResult.TwitchLoginRequired
    "check-error" -> ChatterCheckResult.Failed(chatterCheckErrorText(json))
    "error" -> ChatterCheckResult.Failed(UiText.Raw(json.getString("text")))
    "result" -> parseChatterCheckValue(json.opt("value"))
    else -> ChatterCheckResult.Failed(UiText.Resource(R.string.chatter_check_failed))
}

private fun chatterCheckErrorText(json: JSONObject): UiText.Resource {
    val reason = json.optString("reason")
    val id = when (reason) {
        "channel-required" -> R.string.chatter_check_channel_required
        "timeout" -> R.string.chatter_check_timeout
        "result-type" -> R.string.chatter_check_result_type
        "message-array" -> R.string.chatter_check_message_array
        "message-type" -> R.string.chatter_check_message_type
        "keywords-type" -> R.string.chatter_check_keywords_type
        "keywords-empty" -> R.string.chatter_check_keywords_empty
        "keyword-format" -> R.string.chatter_check_keyword_format
        "options" -> R.string.chatter_check_archive_options
        "filter-type" -> R.string.chatter_check_filter_type
        "filter-result" -> R.string.chatter_check_filter_result
        "limits" -> R.string.chatter_check_archive_limits
        "pagination" -> R.string.chatter_check_archive_pagination
        "archive-request" -> R.string.chatter_check_archive_request
        "archive-rejected" -> R.string.chatter_check_archive_rejected
        "archive-too-large" -> R.string.chatter_check_archive_too_large
        "archive-timeout" -> R.string.chatter_check_archive_timeout
        "no-archive" -> R.string.chatter_check_no_archive
        "rate-limit" -> R.string.chatter_check_archive_rate_limit
        "archive-http" -> R.string.chatter_check_archive_http
        "archive-invalid" -> R.string.chatter_check_archive_invalid
        "twitch-input" -> R.string.chatter_check_twitch_input
        "twitch-user-input" -> R.string.chatter_check_twitch_user_input
        "twitch-user-missing" -> R.string.chatter_check_twitch_user_missing
        "twitch-request" -> R.string.chatter_check_twitch_request
        "twitch-timeout" -> R.string.chatter_check_twitch_timeout
        "twitch-http" -> R.string.chatter_check_twitch_http
        "twitch-auth" -> R.string.chatter_check_twitch_auth
        "twitch-data" -> R.string.chatter_check_twitch_data
        else -> R.string.chatter_check_failed
    }
    val args = if (reason == "archive-http" || reason == "twitch-http") listOf(json.optInt("status")) else emptyList()
    return UiText.Resource(id, args)
}

private fun parseChatterCheckValue(value: Any?): ChatterCheckResult.Done = when (value) {
    true -> ChatterCheckResult.Done(UiText.Resource(R.string.chatter_check_yes))
    false -> ChatterCheckResult.Done(UiText.Resource(R.string.chatter_check_no))
    null, JSONObject.NULL -> ChatterCheckResult.Done(UiText.Resource(R.string.chatter_check_unknown))
    is JSONObject -> ChatterCheckResult.Done(
        UiText.Raw(value.getString("text")),
        value.getJSONArray("messages").objects().map { message ->
            ChatterCheckMessage(
                message.getString("text"), message.optString("timestamp"),
                message.optString("id"), message.optString("channelId"),
            )
        },
    )
    else -> ChatterCheckResult.Done(UiText.Raw(value.toString()))
}
