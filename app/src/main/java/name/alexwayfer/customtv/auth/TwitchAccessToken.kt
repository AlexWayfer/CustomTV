package name.alexwayfer.customtv.auth

import kotlin.time.Duration.Companion.seconds
import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import org.json.JSONObject

private const val ACCESS_REFRESH_MARGIN_MILLIS = 60_000L

/** Twitch's error message cut for a log line or a problem report. */
private const val AUTH_MESSAGE_MAX_LENGTH = 80

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

internal fun rejectedAuthClearsSession(httpCode: Int): Boolean = httpCode in HTTP_CLIENT_ERROR

internal fun twitchAuthMessage(body: String): String = twitchErrorMessage(body).take(AUTH_MESSAGE_MAX_LENGTH)

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
    return nowMillis + expiresInSeconds.seconds.inWholeMilliseconds
}
