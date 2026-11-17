package name.alexwayfer.customtv.data

/** A parsed answer, keeping a temporary failure different from a permanent refusal or an empty list. */
internal data class BadgeFetch<T>(val value: T?, val retryable: Boolean = false)

internal fun <T> parseBadgeFetch(response: EmoteHttpBody, parse: (String) -> T?): BadgeFetch<T> = when (response) {
    is EmoteHttpBody.Success -> BadgeFetch(parse(response.body))
    is EmoteHttpBody.Failure -> BadgeFetch(null, retryable = response.retryable)
    EmoteHttpBody.Empty -> BadgeFetch(null)
}

/** HTTP success may still carry a temporary GQL failure; a refused request must not loop unchanged. */
internal fun badgeQueryResponse(httpCode: Int, body: String): EmoteHttpBody = when {
    httpCode in HTTP_SERVER_ERROR -> EmoteHttpBody.Failure(retryable = true)
    httpCode !in HTTP_SUCCESS -> EmoteHttpBody.Failure(retryable = false)
    hasTransientGqlError(body) -> EmoteHttpBody.Failure(retryable = true)
    body.toJsonObjectOrNull()?.optJSONArray("errors")?.length()?.let { it > 0 } == true ->
        EmoteHttpBody.Failure(retryable = false)
    else -> EmoteHttpBody.Success(body)
}
