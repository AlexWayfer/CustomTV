package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.FollowedChannelsResult

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

/**
 * Whether a load reads the whole follow list rather than only who of the shown channels is live: the whole list is
 * asked for, none is shown yet, or the last whole load failed and the shown list may be out of date.
 */
internal fun followedLoadIsFull(liveOnly: Boolean, listEmpty: Boolean, lastFullFailed: Boolean): Boolean =
    !liveOnly || listEmpty || lastFullFailed

/** Whether a whole load is tried once more right away: only a failure that may pass, not a refusal. */
internal fun followedLoadRetries(result: FollowedChannelsResult): Boolean =
    result == FollowedChannelsResult.Unavailable

/**
 * Whether a failed whole load tells the user: only when it leaves an out-of-date list on screen (an empty list says
 * so itself), and once per run of failures unless the user asked for this load.
 */
internal fun followedLoadFailureShown(
    result: FollowedChannelsResult,
    listShown: Boolean,
    failedBefore: Boolean,
    userAsked: Boolean,
): Boolean = result == FollowedChannelsResult.Unavailable && listShown && (userAsked || !failedBefore)

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
