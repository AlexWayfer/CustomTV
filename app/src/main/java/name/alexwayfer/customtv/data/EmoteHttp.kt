package name.alexwayfer.customtv.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface EmoteHttpBody {
    data class Success(val body: String) : EmoteHttpBody
    data object Empty : EmoteHttpBody
    data class Failure(val retryable: Boolean) : EmoteHttpBody
}

internal object EmoteHttp {
    const val ATTEMPTS = 3

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
            is EmoteHttpBody.Success -> runCatching { parse(result.body) }.getOrNull()
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
            is EmoteHttpBody.Success -> runCatching { parse(result.body) }.getOrNull()
                ?.also { AssetFiles.write(file, result.body) }
            EmoteHttpBody.Empty -> empty.also { AssetFiles.delete(file) }
            is EmoteHttpBody.Failure -> null
        }
    }

    /** The answer [fetchStored] kept in [file], or null when there is none or it no longer parses. */
    fun <T> stored(file: String, parse: (String) -> T): T? =
        AssetFiles.read(file)?.let { body -> runCatching { parse(body) }.getOrNull() }

    suspend fun retry(
        attempts: Int,
        backoff: (Int) -> Duration = ::backoff,
        fetch: suspend () -> EmoteHttpBody,
    ): EmoteHttpBody {
        var last: EmoteHttpBody = EmoteHttpBody.Failure(retryable = true)
        for (attempt in 0 until attempts) {
            if (attempt > 0) delay(backoff(attempt))
            last = fetch()
            when (last) {
                is EmoteHttpBody.Success, EmoteHttpBody.Empty -> return last
                is EmoteHttpBody.Failure -> if (!last.retryable) return last
            }
        }
        return last
    }

    fun isRetryableStatus(code: Int): Boolean {
        return code == 408 || code == 425 || code == 429 || code in 500..599
    }

    fun backoff(attempt: Int): Duration {
        return (500L * (1L shl (attempt - 1).coerceAtLeast(0))).coerceAtMost(4_000L).milliseconds
    }

    fun classify(response: Response): EmoteHttpBody {
        val code = response.code
        return when {
            response.isSuccessful -> {
                val text = response.body.string()
                if (text.isBlank()) EmoteHttpBody.Empty else EmoteHttpBody.Success(text)
            }
            code == 404 -> EmoteHttpBody.Empty
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
                    Diagnostics.reportHttp("EmoteHttp", response.request.url.host, response.code, "", setOf(404))
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
