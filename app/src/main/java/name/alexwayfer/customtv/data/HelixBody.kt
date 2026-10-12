package name.alexwayfer.customtv.data

import okhttp3.OkHttpClient

/** A Helix answer read whole: its code, body, and when its rate-limit window resets. */
internal data class HelixBody(val code: Int, val body: String, val rateLimitReset: String? = null)

internal fun OkHttpClient.helixGet(url: String, accessToken: String, clientId: String): HelixBody {
    val request = helixRequest(url, accessToken, clientId)
        .build()
    newCall(request).execute().use { response ->
        return HelixBody(response.code, response.body.string(), response.header("Ratelimit-Reset"))
    }
}
