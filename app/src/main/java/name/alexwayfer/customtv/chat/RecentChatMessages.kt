package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.HTTP_SERVER_ERROR
import name.alexwayfer.customtv.data.sharedHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal const val RECENT_CHAT_TAG = "RecentChat"
private const val RECENT_MESSAGES_URL = "https://recent-messages.robotty.de/api/v2/recent-messages/"
private const val RECENT_MESSAGE_COUNT = 50
private const val RECENT_CHAT_ATTEMPTS = 2
private val recentChatHttp = sharedHttpClient.newBuilder()
    .callTimeout(10, TimeUnit.SECONDS)
    .build()

/**
 * A request that failed without an answer or with a server error is worth one more try; a 4xx,
 * such as an unknown channel, would fail the same way again.
 */
internal fun recentChatRetryable(httpCode: Int?, failure: Exception?): Boolean = when {
    httpCode != null -> httpCode >= HTTP_SERVER_ERROR.first
    else -> failure is IOException
}

/** The latest [count] messages of [channel]'s chat, oldest first. */
internal suspend fun loadRecentChatMessages(
    channel: String,
    count: Int = RECENT_MESSAGE_COUNT,
): RecentChatLoadResult = withContext(Dispatchers.IO) {
    if (channel.isBlank()) return@withContext RecentChatLoadResult()
    val request = Request.Builder()
        .url(RECENT_MESSAGES_URL + channel)
        .header("User-Agent", "CustomTV Android")
        .build()
    var attempt = 1
    var outcome = fetchRecentChat(request, attempt, count)
    while (outcome.retryable && attempt < RECENT_CHAT_ATTEMPTS) {
        attempt += 1
        outcome = fetchRecentChat(request, attempt, count)
    }
    outcome.result
}

private class RecentChatAttempt(val result: RecentChatLoadResult, val retryable: Boolean = false)

private fun fetchRecentChat(request: Request, attempt: Int, count: Int): RecentChatAttempt = try {
    recentChatHttp.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            RecentChatAttempt(RecentChatLoadResult(parseRecentChat(response.body.string(), count)))
        } else {
            AppLog.w(RECENT_CHAT_TAG, "recent messages failed status=${response.code} (attempt $attempt)")
            RecentChatAttempt(
                RecentChatLoadResult(failed = true),
                retryable = recentChatRetryable(response.code, null),
            )
        }
    }
} catch (failure: IOException) {
    failedRecentChat(failure, attempt)
} catch (failure: JSONException) {
    failedRecentChat(failure, attempt)
}

private fun failedRecentChat(failure: Exception, attempt: Int): RecentChatAttempt {
    AppLog.w(RECENT_CHAT_TAG, "recent messages failed: ${failure.javaClass.simpleName} (attempt $attempt)")
    return RecentChatAttempt(
        RecentChatLoadResult(failed = true),
        retryable = recentChatRetryable(null, failure),
    )
}

/** The latest messages of the answer, oldest first; none when it has no list. */
private fun parseRecentChat(body: String, count: Int): List<ChatMessage> {
    val raw = JSONObject(body).optJSONArray("messages") ?: return emptyList()
    return buildList {
        for (index in 0 until raw.length()) {
            raw.optString(index).takeIf { it.isNotBlank() }
                ?.let(IrcMessageParser::parsePrivMsg)
                ?.let(::add)
        }
    }.sortedBy { it.timestampMillis }.takeLast(count)
}
