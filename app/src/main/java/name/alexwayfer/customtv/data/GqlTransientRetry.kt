package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

/** Every GQL read is sent at most this many times while Twitch's own server keeps failing on it. */
private const val GQL_ATTEMPTS = 3

private const val TAG = "TwitchGql"

/**
 * Which GQL answers [GqlTransientRetry] sends again: those [shouldRetry] accepts. A request without this
 * tag, such as a mutation, is sent once, since repeating it could do the action twice.
 */
internal class GqlRetryPolicy(val shouldRetry: (String) -> Boolean) {
    companion object {
        /** Any of Twitch's own server failures, wherever in the answer it is. */
        val AnyTransientError = GqlRetryPolicy(::hasTransientGqlError)
    }
}

/**
 * Sends a GQL read again when Twitch's own server failed on it: Twitch answers HTTP 200 with a "service
 * error" for a moment and then answers the same request. It waits [EmoteHttp.backoff] before each repeat.
 */
internal object GqlTransientRetry : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val policy = request.tag(GqlRetryPolicy::class.java) ?: return chain.proceed(request)
        var response = chain.proceed(request)
        for (attempt in 1 until GQL_ATTEMPTS) {
            val failure = retriedFailure(chain, response, policy) ?: break
            response.close()
            AppLog.w(TAG, "$failure, retry $attempt")
            Thread.sleep(EmoteHttp.backoff(attempt).inWholeMilliseconds)
            response = chain.proceed(request)
        }
        return response
    }

    /** Where Twitch failed in [response] when [policy] sends it again, or null when the answer stays. */
    private fun retriedFailure(chain: Interceptor.Chain, response: Response, policy: GqlRetryPolicy): String? {
        if (!response.isSuccessful || chain.call().isCanceled()) return null
        val body = response.peekBody(Long.MAX_VALUE).string()
        return if (policy.shouldRetry(body)) transientGqlErrorPlace(body) else null
    }
}

/** Whether an error in a GQL answer is Twitch's own server failing, which the next request may not hit. */
internal fun hasTransientGqlError(body: String): Boolean = transientGqlErrors(body).isNotEmpty()

/** The first server failure in a GQL answer and the field it hit, such as `service error at user.videos`. */
internal fun transientGqlErrorPlace(body: String): String {
    val error = transientGqlErrors(body).firstOrNull() ?: return "no server failure"
    val message = error.trimmedText("message").orEmpty()
    val path = error.optJSONArray("path") ?: return message
    return "$message at ${(0 until path.length()).joinToString(".") { path.opt(it).toString() }}"
}

private fun transientGqlErrors(body: String): List<JSONObject> {
    val errors = body.toJsonObjectOrNull()?.optJSONArray("errors") ?: return emptyList()
    return errors.objects().filter { error -> isTransientGqlMessage(error.trimmedText("message").orEmpty()) }
}

/**
 * Whether an error in [json] hit the field at [path] or one inside it, so that field may be null or miss
 * parts. A path names fields by name and list items by index, as Twitch reports them.
 */
internal fun gqlErrorWithin(json: JSONObject, path: List<Any>): Boolean {
    val errors = json.optJSONArray("errors") ?: return false
    return errors.objects().any { error ->
        val errorPath = error.optJSONArray("path") ?: return@any false
        errorPath.length() >= path.size && path.indices.all { errorPath.opt(it) == path[it] }
    }
}

/** Whether [payload] is a GQL mutation, which must not be sent twice. A persisted query is a read. */
internal fun isGqlMutation(payload: JSONObject): Boolean =
    payload.optString("query").trimStart().startsWith("mutation")
