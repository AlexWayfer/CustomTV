package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.FollowedChannel

internal const val FOLLOWED_AUTO_REFRESH_INTERVAL_MS = 60_000L

internal fun followedAutomaticRefreshAllowed(
    lastAtMillis: Long?,
    nowMillis: Long,
    lastUserId: String?,
    userId: String,
): Boolean {
    return lastUserId != userId ||
        lastAtMillis == null ||
        nowMillis < lastAtMillis ||
        nowMillis - lastAtMillis >= FOLLOWED_AUTO_REFRESH_INTERVAL_MS
}

/** The shown follow list belongs to [shownUserId]; another account must not see it, even for a moment. */
internal fun followedListBelongsToAnotherAccount(shownUserId: String?, userId: String): Boolean =
    shownUserId != null && shownUserId != userId

internal fun followedListDuringReload(
    current: List<FollowedChannel>,
    incoming: List<FollowedChannel>,
    reloadFinished: Boolean,
): List<FollowedChannel> {
    if (current.isEmpty() || reloadFinished) return incoming
    return current
}

/** The follows a stream can be checked against without a request, or null until Home has loaded them. */
internal fun followedChannelIds(
    signedIn: Boolean,
    status: FollowedChannelsStatus,
    channels: List<FollowedChannel>,
): Set<String>? {
    if (!signedIn || status != FollowedChannelsStatus.Ready) return null
    return channels.mapTo(HashSet()) { it.id }
}
