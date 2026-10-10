package name.alexwayfer.customtv.auth

import org.json.JSONObject

internal const val TWITCH_LOGIN_SCOPES =
    "user:read:follows user:write:chat user:read:chat user:read:emotes user:read:subscriptions"
internal const val DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"

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

internal fun followListCanLoad(grantedScopes: Set<String>?): Boolean {
    return grantedScopes?.contains("user:read:follows") == true
}

internal fun loginClickShowsScopePrompt(scopeReloginPending: Boolean): Boolean {
    return scopeReloginPending
}

internal fun resumeSavedChannel(
    channelId: String?,
    openedOnHome: Boolean,
    scopeReloginPending: Boolean,
): Boolean = !channelId.isNullOrBlank() && openedOnHome && !scopeReloginPending

internal fun playerYieldsToReloginPrompt(
    reloginForPermissions: Boolean,
    reloginForSession: Boolean,
): Boolean = reloginForPermissions || reloginForSession

internal fun rejectedSessionAsksToLogInAgain(hadAccount: Boolean, alreadyDismissed: Boolean): Boolean {
    return hadAccount && !alreadyDismissed
}

internal fun latestSessionForRefresh(
    requested: TwitchSession,
    memory: TwitchSession?,
    persisted: TwitchSession?,
    deviceGrant: Boolean,
): TwitchSession {
    if (deviceGrant) return requested
    if (persisted != null && persisted.refreshToken != memory?.refreshToken) return persisted
    return memory ?: requested
}

/**
 * A token works when it has every scope this build asks for. Extra scopes are fine: a Premium login
 * keeps working in the free build, and a free login asks again for Premium's scopes after an upgrade.
 */
internal fun tokenScopesMatch(expectedRaw: String, grantedScopes: Set<String>): Boolean {
    val expected = expectedRaw.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    return grantedScopes.containsAll(expected)
}

internal fun savedSessionNeedsNewLogin(grantedScopes: Set<String>?, expectedRaw: String): Boolean {
    return grantedScopes != null && !tokenScopesMatch(expectedRaw, grantedScopes)
}

private val DEVICE_GRANT_STATUSES = setOf(
    "authorization_pending",
    "slow_down",
    "access_denied",
    "invalid device code",
    "expired_token",
    "expired device code",
)
private const val ACCESS_REFRESH_MARGIN_MILLIS = 60_000L
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
    if (httpCode in 500..599) return DeviceGrant.Unavailable(httpCode)
    if (httpCode in 200..299) {
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

internal fun rejectedAuthClearsSession(httpCode: Int): Boolean = httpCode in 400..499

internal fun devicePollKeepsWaiting(error: Throwable): Boolean {
    return error is java.io.IOException && error !is TwitchAuthException
}

internal fun pendingLoginPayload(login: TwitchDeviceLogin, nowMillis: Long): String {
    return JSONObject()
        .put("deviceCode", login.deviceCode)
        .put("userCode", login.userCode)
        .put("verificationUri", login.verificationUri)
        .put("expiresAtMillis", nowMillis + login.expiresInSeconds * 1000L)
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
    val remainingSeconds = (expiresAt - nowMillis + 999L) / 1000L
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
    return intervalSeconds.coerceAtLeast(1L) * 1_000L
}

internal fun twitchAuthMessage(body: String): String = twitchErrorMessage(body).take(80)

/** The whole message of a Twitch error answer, for a user to read; empty when there is none. */
internal fun twitchErrorMessage(body: String): String {
    val root = try {
        JSONObject(body)
    } catch (_: Exception) {
        return ""
    }
    return root.optString("message").ifBlank { root.optString("error") }.trim()
}

internal fun freshSavedFollowAccess(
    accessToken: String,
    userId: String?,
    expiresAtMillis: Long,
    nowMillis: Long,
): Pair<String, String>? {
    if (accessToken.isBlank()) return null
    val id = userId?.takeIf { it.isNotBlank() } ?: return null
    if (shouldRefreshAccessToken(expiresAtMillis, nowMillis)) return null
    return accessToken to id
}

internal fun shouldRefreshAccessToken(expiresAtMillis: Long, nowMillis: Long): Boolean {
    return expiresAtMillis - nowMillis <= ACCESS_REFRESH_MARGIN_MILLIS
}

internal fun accessExpiryMillis(nowMillis: Long, expiresInSeconds: Long): Long {
    return nowMillis + expiresInSeconds * 1000L
}

internal const val TWITCH_APP_PACKAGE = "tv.twitch.android.app"

internal fun twitchAppHandlesLogin(handlerPackage: String?): Boolean {
    return handlerPackage == TWITCH_APP_PACKAGE
}

internal fun loginNeedsLinkWarning(linkHandlingAllowed: Boolean?, handlerPackage: String?): Boolean {
    return linkHandlingAllowed ?: twitchAppHandlesLogin(handlerPackage)
}

internal fun loginSheetHeightPx(screenHeightPx: Int): Int {
    if (screenHeightPx <= 0) return 0
    return (screenHeightPx * 85L / 100L).toInt()
}

internal fun twitchLinkSettingsAction(sdkInt: Int): String {
    return if (sdkInt >= 31) {
        "android.settings.APP_OPEN_BY_DEFAULT_SETTINGS"
    } else {
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    }
}
