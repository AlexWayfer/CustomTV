package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.FollowForChatMode

/** Whether the user follows the open channel, as far as the app knows; their own channel has nothing to follow. */
internal enum class ChannelFollowStatus { Unknown, Following, NotFollowing, OwnChannel }

/** The heart shows only once the follow is known, and never on the user's own channel. */
internal val ChannelFollowStatus.showsFollowButton: Boolean
    get() = this == ChannelFollowStatus.Following || this == ChannelFollowStatus.NotFollowing

/** The open channel is the signed-in user's own, compared by ID. */
internal fun isOwnChannel(channelId: String, ownUserId: String?): Boolean =
    !ownUserId.isNullOrBlank() && channelId.trim() == ownUserId.trim()

/**
 * Home's follows list answers without a request. A request is needed only while that list is
 * unknown, or after the user comes back to the app, where they may have followed or unfollowed in a browser.
 */
internal fun channelFollowNeedsRequest(followedIds: Set<String>?, returned: Boolean): Boolean =
    followedIds == null || returned

/** A finished request is newer than Home's list; without one, the list decides, and without the list it is unknown. */
internal fun channelFollowStatus(
    followedIds: Set<String>?,
    checked: FollowForChatMode,
    channelId: String,
): ChannelFollowStatus = when (checked) {
    is FollowForChatMode.Following -> ChannelFollowStatus.Following
    FollowForChatMode.NotFollowing -> ChannelFollowStatus.NotFollowing
    FollowForChatMode.Unknown -> when {
        followedIds == null -> ChannelFollowStatus.Unknown
        channelId.trim() in followedIds -> ChannelFollowStatus.Following
        else -> ChannelFollowStatus.NotFollowing
    }
}

/** A request that disagrees with Home's loaded list means the list missed a follow or an unfollow made elsewhere. */
internal fun followListOutdated(followedIds: Set<String>?, checked: FollowForChatMode, channelId: String): Boolean {
    if (followedIds == null) return false
    val listed = channelId.trim() in followedIds
    return when (checked) {
        is FollowForChatMode.Following -> !listed
        FollowForChatMode.NotFollowing -> listed
        FollowForChatMode.Unknown -> false
    }
}
