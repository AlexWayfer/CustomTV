package name.alexwayfer.customtv.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TWITCH_GQL_URL = "https://gql.twitch.tv/gql"
private const val TWITCH_WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"

private val twitchGqlJson = "application/json; charset=utf-8".toMediaType()

internal fun twitchGqlRequest(body: String): Request.Builder {
    return Request.Builder()
        .url(TWITCH_GQL_URL)
        .header("Client-ID", TWITCH_WEB_CLIENT_ID)
        .post(body.toRequestBody(twitchGqlJson))
}

/** Twitch GQL answers HTTP 400 to a query with more root field aliases than this. */
private const val GQL_ROOT_ALIAS_LIMIT = 15

/** Splits items asked with one alias each into batches that one query can hold, in order. */
internal fun <T> gqlAliasBatches(items: List<T>): List<List<T>> = items.chunked(GQL_ROOT_ALIAS_LIMIT)

/** Twitch's own server failing, like an HTTP 5xx: the next request may succeed. */
private val transientGqlErrors = listOf("service timeout", "service error", "service unavailable")

/**
 * The errors of a GQL answer a developer has to look at: a changed schema, an outdated persisted
 * query, or a failed integrity check. Server failures that pass on their own are left out, and so are
 * channel links: Twitch withholds `socialMedias` behind its integrity check from some networks and not others.
 */
internal fun gqlErrorsToReport(body: String): List<String> {
    val errors = runCatching { JSONObject(body) }.getOrNull()?.optJSONArray("errors") ?: return emptyList()
    return (0 until errors.length()).mapNotNull { index ->
        val error = errors.optJSONObject(index) ?: return@mapNotNull null
        val withheldLinks = error.optJSONObject("extensions")?.optString("code") == "IntegrityCheckFailed" &&
            error.optJSONArray("path")?.let { it.optString(it.length() - 1) } == "socialMedias"
        if (withheldLinks) return@mapNotNull null
        error.optString("message").trim().takeIf { it.isNotEmpty() && it != "null" }
    }.filterNot { message -> transientGqlErrors.any { message.contains(it, ignoreCase = true) } }.distinct()
}

/**
 * Why a GQL answer could not be read, or left out a field: Twitch's own error messages, such as
 * "failed integrity check", or what is missing. Twitch answers these with HTTP 200.
 */
internal fun gqlFailureCause(body: String): String {
    val json = runCatching { JSONObject(body) }.getOrNull() ?: return "not JSON"
    val errors = json.optJSONArray("errors")
    val messages = (0 until (errors?.length() ?: 0)).mapNotNull { index ->
        errors?.optJSONObject(index)?.optString("message")?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    }
    return when {
        messages.isNotEmpty() -> messages.joinToString("; ")
        !json.has("data") -> "no data"
        else -> "missing fields"
    }
}
