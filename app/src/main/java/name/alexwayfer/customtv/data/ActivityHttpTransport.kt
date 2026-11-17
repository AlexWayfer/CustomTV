package name.alexwayfer.customtv.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/** The login secret stays in memory and is never part of the analytics file or request body. */
internal data class ActivityAuthorization(val edition: String, val deviceSecret: String? = null) {
    override fun toString(): String = "ActivityAuthorization($edition)"
}

internal class ActivityHttpTransport(private val authorization: ActivityAuthorization) : ActivityTransport {
    override suspend fun post(endpoint: String, body: JSONObject): ActivityApiAnswer {
        currentCoroutineContext().ensureActive()
        val request = activityHttpRequest(endpoint, body, authorization)
        return http.newCall(request).executeAsync().use { response ->
            val source = response.body.source()
            if (source.request(MAX_RESPONSE_BYTES + 1)) throw IOException("Oversized activity response")
            val text = source.readUtf8()
            val json = try {
                JSONObject(text)
            } catch (error: JSONException) {
                throw IOException("Unreadable activity response HTTP ${response.code}", error)
            }
            ActivityApiAnswer(response.code, json)
        }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 8192L
        val http by lazy {
            sharedHttpClient.newBuilder().callTimeout(30.seconds)
                .followRedirects(false).followSslRedirects(false).build()
        }
    }
}

internal fun activityHttpRequest(
    endpoint: String,
    body: JSONObject,
    authorization: ActivityAuthorization,
): Request = Request.Builder().url("$MECENA_API_URL/$endpoint")
    .apply {
        // Key registration is installation-wide and has no Telegram authentication.
        if (endpoint == "activity" && authorization.edition == "premium") {
            val secret = authorization.deviceSecret ?: throw IOException("Premium analytics needs a device login")
            header("Authorization", "Bearer $secret")
        }
    }
    .post(body.toString().toRequestBody("application/json".toMediaType()))
    .build()
