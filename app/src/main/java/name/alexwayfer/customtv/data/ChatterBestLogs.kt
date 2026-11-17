package name.alexwayfer.customtv.data

import java.io.IOException
import java.io.InterruptedIOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.json.JSONObject

private val bestLogsBaseUrl = "https://bestlogs.lurkology.com".toHttpUrl()
private val bestLogsTimeout = 10.seconds
private val bestLogsHttp by lazy {
    sharedHttpClient.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(bestLogsTimeout).build()
}
private val twitchId = Regex("[0-9]+")
private val bestLogsTerm = Regex("[\\p{L}\\p{M}\\p{N}_]+")
private val pageSpacing = 400.milliseconds
private val minRateLimitWait = 500.milliseconds
private val maxRateLimitWait = 3.seconds
private const val BEST_LOGS_PAGE_SIZE = 500
private const val BEST_LOGS_MAX_BODY_BYTES = 2_097_152L
private const val BEST_LOGS_TAG = "ChatterBestLogs"

/** The script supplies only the search term and pagination. User, channel, destination and credentials stay native. */
internal fun chatterBestLogsUrl(channelId: String, userId: String, query: JSONObject): HttpUrl? {
    val term = query.optString("query")
    val limit = query.optInt("limit", 0)
    val offset = query.optInt("offset", -1)
    if (!twitchId.matches(channelId) || !twitchId.matches(userId) || !bestLogsTerm.matches(term)) return null
    if (limit !in 1..BEST_LOGS_PAGE_SIZE || offset < 0) return null
    return bestLogsBaseUrl.newBuilder()
        .addPathSegment("channelid").addPathSegment(channelId)
        .addPathSegment("userid").addPathSegment(userId)
        .addPathSegment("search")
        .addQueryParameter("q", term)
        .addQueryParameter("jsonBasic", "1")
        .addQueryParameter("reverse", "1")
        .addQueryParameter("limit", limit.toString())
        .addQueryParameter("offset", offset.toString())
        .build()
}

/** A check's sequential archive requests share their pacing and one short Retry-After retry. */
internal class ChatterBestLogs(private val channelId: String?, private val userId: String) {
    private var requested = false
    private var retried = false

    suspend fun search(query: JSONObject): JSONObject {
        val url = channelId?.let { chatterBestLogsUrl(it, userId, query) }
        if (url == null) {
            AppLog.w(BEST_LOGS_TAG, "search request rejected before HTTP")
            return JSONObject().put("error", "archive-rejected")
        }
        return try {
            val response = loadPage(url)
            JSONObject().put("status", response.status).put("body", response.body)
        } catch (error: IOException) {
            bestLogsFailure(error)
        }
    }

    private suspend fun loadPage(url: HttpUrl): BestLogsPage {
        if (requested) delay(pageSpacing)
        requested = true
        val first = requestBestLogsPage(url)
        val retryWait = first.retryAfterMillis.milliseconds.coerceAtLeast(minRateLimitWait)
        return if (first.status == HTTP_TOO_MANY_REQUESTS && !retried && retryWait <= maxRateLimitWait) {
            retried = true
            delay(retryWait)
            requestBestLogsPage(url)
        } else {
            first
        }
    }
}

private data class BestLogsPage(val status: Int, val body: String, val retryAfterMillis: Long)
private class BestLogsResponseTooLargeException : IOException()

private fun bestLogsFailure(error: IOException): JSONObject {
    val cause = error.cause?.javaClass?.simpleName?.let { " ($it)" }.orEmpty()
    AppLog.w(BEST_LOGS_TAG, "search failed: ${error.javaClass.simpleName}$cause")
    val reason = when (error) {
        is BestLogsResponseTooLargeException -> "archive-too-large"
        is InterruptedIOException -> "archive-timeout"
        else -> "archive-request"
    }
    return JSONObject().put("error", reason)
}

private suspend fun requestBestLogsPage(url: HttpUrl): BestLogsPage = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).build()
    bestLogsHttp.newCall(request).executeAsync().use { response ->
        val source = response.body.source()
        if (source.request(BEST_LOGS_MAX_BODY_BYTES + 1)) throw BestLogsResponseTooLargeException()
        BestLogsPage(response.code, source.readUtf8(), bestLogsRetryAfter(response.header("Retry-After")))
    }
}

private fun bestLogsRetryAfter(header: String?): Long {
    val value = header?.trim().orEmpty()
    value.toLongOrNull()?.let { return it.seconds.inWholeMilliseconds }
    return try {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() -
            System.currentTimeMillis()
    } catch (_: DateTimeParseException) {
        1.seconds.inWholeMilliseconds
    }
}
