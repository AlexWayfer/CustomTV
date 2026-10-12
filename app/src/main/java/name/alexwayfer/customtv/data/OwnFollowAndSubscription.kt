package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatterFollow

internal sealed interface UserSubscriptionRead {
    data object Subscribed : UserSubscriptionRead
    data object NotSubscribed : UserSubscriptionRead
    data object Unavailable : UserSubscriptionRead
}

internal fun userSubscriptionRead(httpCode: Int, body: String): UserSubscriptionRead {
    if (httpCode == HTTP_NOT_FOUND) return UserSubscriptionRead.NotSubscribed
    if (httpCode in HTTP_SUCCESS) {
        val listed = body.toJsonObjectOrNull()?.optJSONArray("data")?.length() ?: 0
        return if (listed > 0) UserSubscriptionRead.Subscribed else UserSubscriptionRead.NotSubscribed
    }
    return UserSubscriptionRead.Unavailable
}

internal sealed interface OwnFollowRead {
    data class Following(val atMillis: Long) : OwnFollowRead
    data object NotFollowing : OwnFollowRead
    data object Unavailable : OwnFollowRead
}

/** The card's follow line from an own follow read: an empty answer is Not following, a failed one is unknown. */
internal fun chatterFollowOf(read: OwnFollowRead): ChatterFollow? = when (read) {
    is OwnFollowRead.Following -> ChatterFollow.Following(read.atMillis)
    OwnFollowRead.NotFollowing -> ChatterFollow.NotFollowing
    OwnFollowRead.Unavailable -> null
}

internal fun parseOwnFollowedAt(body: String, broadcasterId: String): Long? {
    val data = body.toJsonObjectOrNull()?.optJSONArray("data") ?: return null
    val item = (0 until data.length()).asSequence()
        .mapNotNull { data.optJSONObject(it) }
        .firstOrNull { it.optString("broadcaster_id") == broadcasterId }
        ?: return null
    return item.text("followed_at")?.let(::instantMillisOrNull)
}
