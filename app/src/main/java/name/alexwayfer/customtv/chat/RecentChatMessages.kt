package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.sharedHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "RecentChat"
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
    httpCode != null -> httpCode >= 500
    else -> failure is IOException
}

internal suspend fun loadRecentChatMessages(channel: String): RecentChatLoadResult = withContext(Dispatchers.IO) {
    if (channel.isBlank()) return@withContext RecentChatLoadResult()
    val request = Request.Builder()
        .url(RECENT_MESSAGES_URL + channel)
        .header("User-Agent", "CustomTV Android")
        .build()
    var attempt = 1
    var outcome = fetchRecentChat(request, attempt)
    while (outcome.retryable && attempt < RECENT_CHAT_ATTEMPTS) {
        attempt += 1
        outcome = fetchRecentChat(request, attempt)
    }
    outcome.result
}

private class RecentChatAttempt(val result: RecentChatLoadResult, val retryable: Boolean = false)

private fun fetchRecentChat(request: Request, attempt: Int): RecentChatAttempt {
    try {
        recentChatHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                AppLog.w(TAG, "recent messages failed status=${response.code} (attempt $attempt)")
                return RecentChatAttempt(
                    RecentChatLoadResult(failed = true),
                    retryable = recentChatRetryable(response.code, null),
                )
            }
            val root = JSONObject(response.body.string())
            val raw = root.optJSONArray("messages") ?: return RecentChatAttempt(RecentChatLoadResult())
            return RecentChatAttempt(RecentChatLoadResult(buildList {
                for (index in 0 until raw.length()) {
                    raw.optString(index).takeIf { it.isNotBlank() }
                        ?.let(IrcMessageParser::parsePrivMsg)
                        ?.let(::add)
                }
            }.sortedBy { it.timestampMillis }.takeLast(RECENT_MESSAGE_COUNT)))
        }
    } catch (failure: Exception) {
        AppLog.w(TAG, "recent messages failed: ${failure.javaClass.simpleName} (attempt $attempt)")
        return RecentChatAttempt(
            RecentChatLoadResult(failed = true),
            retryable = recentChatRetryable(null, failure),
        )
    }
}
