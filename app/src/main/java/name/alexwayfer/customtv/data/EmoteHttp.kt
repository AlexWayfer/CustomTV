package name.alexwayfer.customtv.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal sealed interface EmoteHttpBody {
    data class Success(val body: String) : EmoteHttpBody
    data object Empty : EmoteHttpBody
    data class Failure(val retryable: Boolean) : EmoteHttpBody
}

internal object EmoteHttp {
    const val ATTEMPTS = 3

    private val FIRST_BACKOFF = 500.milliseconds
    private val MAX_BACKOFF = 4.seconds

    suspend fun get(client: OkHttpClient, url: String): EmoteHttpBody {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        return retry(ATTEMPTS) { execute(client, request) }
    }

    suspend fun <T> fetchMap(
        client: OkHttpClient,
        url: String,
        empty: T,
        parse: (String) -> T,
    ): T? {
        return when (val result = get(client, url)) {
            is EmoteHttpBody.Success -> parsedOrNull(result.body, parse)
            EmoteHttpBody.Empty -> empty
            is EmoteHttpBody.Failure -> null
        }
    }

    /** Like [fetchMap], and keeps the parsed answer in [file] for the next start. A 404 removes it. */
    suspend fun <T> fetchStored(
        client: OkHttpClient,
        url: String,
        file: String,
        empty: T,
        parse: (String) -> T,
    ): T? {
        return when (val result = get(client, url)) {
            is EmoteHttpBody.Success -> parsedOrNull(result.body, parse)
                ?.also { AssetFiles.write(file, result.body) }
            EmoteHttpBody.Empty -> empty.also { AssetFiles.delete(file) }
            is EmoteHttpBody.Failure -> null
        }
    }

    /** The answer [fetchStored] kept in [file], or null when there is none or it no longer parses. */
    fun <T> stored(file: String, parse: (String) -> T): T? =
        AssetFiles.read(file)?.let { body -> parsedOrNull(body, parse) }

    /** What [parse] reads from [body], or null when the body is not the JSON it expects. */
    private fun <T> parsedOrNull(body: String, parse: (String) -> T): T? = try {
        parse(body)
    } catch (_: JSONException) {
        null
    }

    suspend fun retry(
        attempts: Int,
        backoff: (Int) -> Duration = ::backoff,
        fetch: suspend () -> EmoteHttpBody,
    ): EmoteHttpBody {
        var last: EmoteHttpBody = EmoteHttpBody.Failure(retryable = true)
        for (attempt in 0 until attempts) {
            if (attempt > 0) delay(backoff(attempt))
            last = fetch()
            if (last !is EmoteHttpBody.Failure || !last.retryable) break
        }
        return last
    }

    fun isRetryableStatus(code: Int): Boolean {
        return code == HTTP_REQUEST_TIMEOUT || code == HTTP_TOO_EARLY || code == HTTP_TOO_MANY_REQUESTS ||
            code in HTTP_SERVER_ERROR
    }

    fun backoff(attempt: Int): Duration {
        return (FIRST_BACKOFF.inWholeMilliseconds * (1L shl (attempt - 1).coerceAtLeast(0)))
            .coerceAtMost(MAX_BACKOFF.inWholeMilliseconds).milliseconds
    }

    fun classify(response: Response): EmoteHttpBody {
        val code = response.code
        return when {
            response.isSuccessful -> {
                val text = response.body.string()
                if (text.isBlank()) EmoteHttpBody.Empty else EmoteHttpBody.Success(text)
            }
            code == HTTP_NOT_FOUND -> EmoteHttpBody.Empty
            isRetryableStatus(code) -> EmoteHttpBody.Failure(retryable = true)
            else -> EmoteHttpBody.Failure(retryable = false)
        }
    }

    private suspend fun execute(client: OkHttpClient, request: Request): EmoteHttpBody {
        val call = client.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isActive) return
                    continuation.resume(EmoteHttpBody.Failure(retryable = !call.isCanceled()))
                }

                override fun onResponse(call: Call, response: Response) {
                    // A channel without emotes there answers 404; the host names the service.
                    Diagnostics.reportHttp(
                        "EmoteHttp",
                        response.request.url.host,
                        response.code,
                        "",
                        setOf(HTTP_NOT_FOUND),
                    )
                    val classified = try {
                        response.use { classify(it) }
                    } catch (_: IOException) {
                        EmoteHttpBody.Failure(retryable = true)
                    }
                    if (continuation.isActive) {
                        continuation.resume(classified)
                    }
                }
            })
        }
    }
}
