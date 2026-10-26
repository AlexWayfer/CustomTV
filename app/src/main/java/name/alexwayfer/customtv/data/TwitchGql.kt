package name.alexwayfer.customtv.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val TWITCH_GQL_URL = "https://gql.twitch.tv/gql"
private const val TWITCH_WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"

private val twitchGqlJson = "application/json; charset=utf-8".toMediaType()

/**
 * A public GQL read; [retry] says which answers [GqlTransientRetry] sends again. Twitch answers it in [language], such
 * as category names and tags.
 */
internal fun twitchGqlRequest(
    body: String,
    retry: GqlRetryPolicy = GqlRetryPolicy.AnyTransientError,
    language: String = TwitchLanguage.current(),
): Request.Builder {
    return Request.Builder()
        .url(TWITCH_GQL_URL)
        .header("Client-ID", TWITCH_WEB_CLIENT_ID)
        .header(ACCEPT_LANGUAGE, language)
        .tag(GqlRetryPolicy::class.java, retry)
        .post(body.toRequestBody(twitchGqlJson))
}

/** The header that picks the language of Twitch's answer. */
internal const val ACCEPT_LANGUAGE = "Accept-Language"

/** Twitch GQL answers HTTP 400 to a query with more root field aliases than this. */
private const val GQL_ROOT_ALIAS_LIMIT = 15

/** Splits items asked with [aliasesEach] aliases each into batches that one query can hold, in order. */
internal fun <T> gqlAliasBatches(items: List<T>, aliasesEach: Int = 1): List<List<T>> =
    items.chunked(GQL_ROOT_ALIAS_LIMIT / aliasesEach)

/** Twitch's own server failing, like an HTTP 5xx: the next request may succeed. */
private val transientGqlErrors = listOf("service timeout", "service error", "service unavailable")

internal fun isTransientGqlMessage(message: String): Boolean =
    transientGqlErrors.any { message.contains(it, ignoreCase = true) }

/**
 * The errors of a GQL answer a developer has to look at: a changed schema, an outdated persisted
 * query, or a failed integrity check. Server failures that pass on their own are left out, and so are
 * channel links: Twitch withholds `socialMedias` behind its integrity check from some networks and not others.
 */
internal fun gqlErrorsToReport(body: String): List<String> {
    val errors = body.toJsonObjectOrNull()?.optJSONArray("errors") ?: return emptyList()
    return (0 until errors.length()).mapNotNull { index ->
        val error = errors.optJSONObject(index) ?: return@mapNotNull null
        val withheldLinks = error.optJSONObject("extensions")?.optString("code") == "IntegrityCheckFailed" &&
            error.optJSONArray("path")?.let { it.optString(it.length() - 1) } == "socialMedias"
        if (withheldLinks) return@mapNotNull null
        error.trimmedText("message")
    }.filterNot(::isTransientGqlMessage).distinct()
}

/**
 * Why a GQL answer could not be read, or left out a field: Twitch's own error messages, such as
 * "failed integrity check", or what is missing. Twitch answers these with HTTP 200.
 */
internal fun gqlFailureCause(body: String): String {
    val json = body.toJsonObjectOrNull() ?: return "not JSON"
    val errors = json.optJSONArray("errors")
    val messages = (0 until (errors?.length() ?: 0)).mapNotNull { index ->
        errors?.optJSONObject(index)?.trimmedText("message")
    }
    return when {
        messages.isNotEmpty() -> messages.joinToString("; ")
        !json.has("data") -> "no data"
        else -> "missing fields"
    }
}
