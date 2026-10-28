package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/** The app's host on the Mecena server, which tells the server which app asks. */
internal const val MECENA_API_URL = "https://customtv.alexwayfer.name/api"

/** The log tag of CustomTV's badges: the list and the user's own badge. */
internal const val CUSTOM_TV_BADGES_TAG = "CustomTvBadges"

private val CALL_TIMEOUT = 30.seconds

private val JSON_TYPE = "application/json".toMediaType()

/**
 * Answers that are not the app's fault: a Twitch token refused, which the account session finds on its own; a secret
 * that lost access just now; and the user's own pace.
 */
private val ROUTINE_FAILURES = setOf(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN, HTTP_TOO_MANY_REQUESTS)

internal sealed interface CustomTvBadgeListAnswer {
    class Changed(val list: CustomTvBadgeList, val etag: String?) : CustomTvBadgeListAnswer
    data object Unchanged : CustomTvBadgeListAnswer
    data object Failed : CustomTvBadgeListAnswer
}

/** The badge endpoints of the Mecena server, which both editions call. */
internal object CustomTvBadgeApi {
    private val http by lazy { sharedHttpClient.newBuilder().callTimeout(CALL_TIMEOUT).build() }

    /** The public list of badges; unchanged when the server still has [etag]. */
    suspend fun list(etag: String?): CustomTvBadgeListAnswer = call("badges", CustomTvBadgeListAnswer.Failed) {
        val request = Request.Builder().url("$MECENA_API_URL/badges")
            .apply { if (etag != null) header("If-None-Match", etag) }
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == HTTP_NOT_MODIFIED) return@use CustomTvBadgeListAnswer.Unchanged
            val list = if (response.isSuccessful) parseCustomTvBadges(response.body.string()) else null
            if (list == null) {
                failed("badges", response.code, response.message)
                CustomTvBadgeListAnswer.Failed
            } else {
                CustomTvBadgeListAnswer.Changed(list, response.header("ETag"))
            }
        }
    }

    /**
     * Gives the account of [twitchToken] a badge, the Premium one with the device's [secret]; the edition the server
     * gave, or null when it gave none. The server reads the account from the token and keeps nothing of it.
     */
    suspend fun register(twitchToken: String, secret: String?): CustomTvBadgeEdition? =
        call("badge", fallback = null) {
            val body = JSONObject().put("twitch_token", twitchToken)
            post("badge", body, secret) { parseCustomTvBadgeEdition(it) }
        }

    /** Takes a Premium badge off [twitchUserId] by the device's [secret]; true once it is off. */
    suspend fun removePremium(secret: String, twitchUserId: String): Boolean = call("badge/off", fallback = false) {
        val id = twitchUserId.toLongOrNull() ?: return@call false
        val body = JSONObject().put("twitch_id", id)
        post("badge/off", body, secret) { true } == true
    }

    /** Takes the free badge off the account of [twitchToken]; true once it is off. */
    suspend fun removeFree(twitchToken: String): Boolean = call("badge/off", fallback = false) {
        val body = JSONObject().put("twitch_token", twitchToken)
        post("badge/off", body, secret = null) { true } == true
    }

    private fun <T : Any> post(endpoint: String, body: JSONObject, secret: String?, parse: (String) -> T?): T? {
        val request = Request.Builder().url("$MECENA_API_URL/$endpoint")
            .apply { if (secret != null) header("Authorization", "Bearer $secret") }
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()
        return http.newCall(request).execute().use { response ->
            val value = if (response.isSuccessful) parse(response.body.string()) else null
            if (value == null) failed(endpoint, response.code, response.message)
            value
        }
    }

    private suspend fun <T> call(endpoint: String, fallback: T, block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (error: IOException) {
            AppLog.w(CUSTOM_TV_BADGES_TAG, "$endpoint failed ${error.javaClass.simpleName}")
            fallback
        }
    }

    private fun failed(endpoint: String, httpCode: Int, message: String) {
        AppLog.w(CUSTOM_TV_BADGES_TAG, "$endpoint failed HTTP $httpCode")
        Diagnostics.reportHttp(CUSTOM_TV_BADGES_TAG, endpoint, httpCode, message, ROUTINE_FAILURES)
    }
}
