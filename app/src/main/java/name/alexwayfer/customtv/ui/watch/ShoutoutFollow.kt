package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChannelShoutout
import name.alexwayfer.customtv.data.ChannelFollowStatus

/** The heart of the shoutout's channel: whether the viewer follows it, and the follow or unfollow a tap makes. */
internal class ShoutoutFollow(val status: ChannelFollowStatus, val action: FollowAction)

/**
 * The follow of the channel [shoutout] names, as the stream header's heart follows the open channel. It lives
 * above the plaque, so a dialog it opens outlasts the plaque.
 */
@Composable
internal fun rememberShoutoutFollow(shoutout: ChannelShoutout?, account: WatchAccount): ShoutoutFollow {
    val channelFollow = rememberChannelFollow(
        shoutout?.targetId,
        account.userId,
        account.followedChannelIds,
        account.readOwnFollow,
        account.onFollowsChanged,
    )
    val action = rememberChannelFollowAction(
        channel = shoutout?.targetLogin.orEmpty(),
        channelId = shoutout?.targetId,
        channelName = shoutout?.targetDisplayName.orEmpty(),
        signedIn = account.signedIn,
        following = channelFollow.status == ChannelFollowStatus.Following,
        onFollowChanged = { following ->
            channelFollow.set(following)
            account.onFollowsChanged()
        },
        onLogIn = account.onLogIn,
    )
    return ShoutoutFollow(channelFollow.status, action)
}
