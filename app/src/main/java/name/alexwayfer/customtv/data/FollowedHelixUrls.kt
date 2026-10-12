package name.alexwayfer.customtv.data

import kotlin.time.Duration.Companion.seconds
import okhttp3.HttpUrl.Companion.toHttpUrl

internal fun userSubscriptionUrl(userId: String, broadcasterId: String): String {
    return helixUrl("subscriptions/user") {
        addQueryParameter("user_id", userId)
        addQueryParameter("broadcaster_id", broadcasterId)
    }
}

internal fun ownFollowedAtUrl(userId: String, broadcasterId: String): String {
    return helixUrl("channels/followed") {
        addQueryParameter("user_id", userId)
        addQueryParameter("broadcaster_id", broadcasterId)
    }
}

internal fun followedChannelsUrl(userId: String, cursor: String?): String {
    return helixUrl("channels/followed") {
        addQueryParameter("user_id", userId)
        addQueryParameter("first", FOLLOWED_PAGE_SIZE.toString())
        if (!cursor.isNullOrBlank()) addQueryParameter("after", cursor)
    }
}

internal fun streamByLoginUrl(login: String): String {
    return helixUrl("streams") {
        addQueryParameter("user_login", login)
    }
}

internal fun followedStreamsUrl(userIds: List<String>): String {
    return helixUrl("streams") {
        addQueryParameter("first", FOLLOWED_PAGE_SIZE.toString())
        userIds.forEach { addQueryParameter("user_id", it) }
    }
}

/** Only the followed channels that are live, so one page covers a follow list of any size. */
internal fun liveFollowedStreamsUrl(userId: String, cursor: String?): String {
    return helixUrl("streams/followed") {
        addQueryParameter("user_id", userId)
        addQueryParameter("first", FOLLOWED_PAGE_SIZE.toString())
        if (!cursor.isNullOrBlank()) addQueryParameter("after", cursor)
    }
}

internal fun followedUsersUrl(userIds: List<String>): String {
    return helixUrl("users") {
        userIds.forEach { addQueryParameter("id", it) }
    }
}

internal fun helixRejectsFollowScope(httpCode: Int, body: String): Boolean {
    return httpCode == HTTP_UNAUTHORIZED && body.contains("scope", ignoreCase = true)
}

/** Helix sends the end of its rate-limit window as Unix seconds in `Ratelimit-Reset`. */
internal fun helixRateLimitResetMillis(header: String?): Long? {
    val resetAt = header?.trim()?.toLongOrNull() ?: return null
    return resetAt.takeIf { it > 0 }?.seconds?.inWholeMilliseconds
}

private fun helixUrl(path: String, fill: okhttp3.HttpUrl.Builder.() -> Unit): String {
    return "https://api.twitch.tv/helix/$path".toHttpUrl().newBuilder().apply(fill).build().toString()
}
