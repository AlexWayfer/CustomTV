package name.alexwayfer.customtv.auth

import kotlin.time.Duration.Companion.seconds
import name.alexwayfer.customtv.data.HTTP_SERVER_ERROR
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.secondsRoundedUp
import org.json.JSONObject

internal const val DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"

private val DEVICE_GRANT_STATUSES = setOf(
    "authorization_pending",
    "slow_down",
    "access_denied",
    "invalid device code",
    "expired_token",
    "expired device code",
)
private const val DEVICE_POLL_FLOOR_SECONDS = 5L
private const val FIRST_DEVICE_POLL_MILLIS = 1_000L

internal data class TwitchDeviceLogin(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Long,
    val intervalSeconds: Long,
)

internal sealed interface DeviceGrant {
    data object Pending : DeviceGrant
    data object SlowDown : DeviceGrant
    data object Denied : DeviceGrant
    data object Expired : DeviceGrant
    data object Failed : DeviceGrant
    data class Unavailable(val httpCode: Int) : DeviceGrant
    data class Approved(val tokens: TwitchTokens) : DeviceGrant
}

internal fun deviceCodeForm(clientId: String): List<Pair<String, String>> {
    return listOf(
        "client_id" to clientId,
        "scopes" to twitchLoginScopes(),
    )
}

internal fun devicePollForm(clientId: String, deviceCode: String): List<Pair<String, String>> {
    return listOf(
        "client_id" to clientId,
        "scopes" to twitchLoginScopes(),
        "device_code" to deviceCode,
        "grant_type" to DEVICE_CODE_GRANT,
    )
}

internal fun parseTwitchDeviceLogin(json: String): TwitchDeviceLogin? {
    val root = try {
        JSONObject(json)
    } catch (_: Exception) {
        return null
    }
    val deviceCode = root.optString("device_code").takeIf { it.isNotBlank() } ?: return null
    val userCode = root.optString("user_code").takeIf { it.isNotBlank() } ?: return null
    val verificationUri = root.optString("verification_uri").takeIf { it.isNotBlank() } ?: return null
    val expiresIn = root.optLong("expires_in", -1L).takeIf { it > 0 } ?: return null
    val interval = root.optLong("interval", DEVICE_POLL_FLOOR_SECONDS).takeIf { it > 0 }
        ?: DEVICE_POLL_FLOOR_SECONDS
    return TwitchDeviceLogin(
        deviceCode = deviceCode,
        userCode = userCode,
        verificationUri = verificationUri,
        expiresInSeconds = expiresIn,
        intervalSeconds = interval,
    )
}

internal fun parseDeviceGrant(httpCode: Int, body: String): DeviceGrant {
    if (httpCode in HTTP_SERVER_ERROR) return DeviceGrant.Unavailable(httpCode)
    if (httpCode in HTTP_SUCCESS) {
        val tokens = parseTwitchTokens(body) ?: return DeviceGrant.Failed
        return DeviceGrant.Approved(tokens)
    }
    return when (deviceGrantStatus(body)) {
        "authorization_pending" -> DeviceGrant.Pending
        "slow_down" -> DeviceGrant.SlowDown
        "access_denied" -> DeviceGrant.Denied
        "invalid device code", "expired_token", "expired device code" -> DeviceGrant.Expired
        else -> DeviceGrant.Failed
    }
}

internal fun deviceGrantStatus(body: String): String {
    val root = try {
        JSONObject(body)
    } catch (_: Exception) {
        return ""
    }
    val message = root.optString("message").lowercase()
    val error = root.optString("error").lowercase()
    if (error in DEVICE_GRANT_STATUSES) return error
    if (message in DEVICE_GRANT_STATUSES) return message
    return message.ifBlank { error }
}

internal fun devicePollKeepsWaiting(error: Throwable): Boolean {
    return error is java.io.IOException && error !is TwitchAuthException
}

internal fun pendingLoginPayload(login: TwitchDeviceLogin, nowMillis: Long): String {
    return JSONObject()
        .put("deviceCode", login.deviceCode)
        .put("userCode", login.userCode)
        .put("verificationUri", login.verificationUri)
        .put("expiresAtMillis", accessExpiryMillis(nowMillis, login.expiresInSeconds))
        .put("intervalSeconds", login.intervalSeconds)
        .toString()
}

internal fun restorePendingLogin(json: String, nowMillis: Long): TwitchDeviceLogin? {
    val root = try {
        JSONObject(json)
    } catch (_: Exception) {
        return null
    }
    val deviceCode = root.optString("deviceCode").takeIf { it.isNotBlank() } ?: return null
    val userCode = root.optString("userCode").takeIf { it.isNotBlank() } ?: return null
    val verificationUri = root.optString("verificationUri").takeIf { it.isNotBlank() } ?: return null
    if (!root.has("expiresAtMillis")) return null
    val expiresAt = root.getLong("expiresAtMillis")
    if (expiresAt <= nowMillis) return null
    val interval = root.optLong("intervalSeconds", DEVICE_POLL_FLOOR_SECONDS).takeIf { it > 0 }
        ?: DEVICE_POLL_FLOOR_SECONDS
    val remainingSeconds = secondsRoundedUp(expiresAt - nowMillis)
    return TwitchDeviceLogin(
        deviceCode = deviceCode,
        userCode = userCode,
        verificationUri = verificationUri,
        expiresInSeconds = remainingSeconds,
        intervalSeconds = interval,
    )
}

internal fun devicePollDelayMillis(completedPolls: Int, intervalSeconds: Long): Long {
    if (completedPolls == 0) return FIRST_DEVICE_POLL_MILLIS
    return intervalSeconds.coerceAtLeast(1L).seconds.inWholeMilliseconds
}
