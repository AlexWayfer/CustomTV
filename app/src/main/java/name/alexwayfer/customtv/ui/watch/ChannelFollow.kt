package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.FollowForChatMode
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.data.channelFollowNeedsRequest
import name.alexwayfer.customtv.data.channelFollowStatus
import name.alexwayfer.customtv.data.followListOutdated
import name.alexwayfer.customtv.ui.theme.TwitchPurple

/** The user's follow of the open channel; [set] records a follow or an unfollow the user just made in the app. */
internal class ChannelFollow(val status: ChannelFollowStatus, val set: (following: Boolean) -> Unit)

/**
 * Whether the user follows the channel, for its heart and its bell. Home's follows list answers when it is
 * loaded; otherwise, and after the user comes back to the app from a browser where they may have followed or
 * unfollowed, one request asks Twitch. A follow made in the app is newer than both. When the request disagrees
 * with Home's list, [onListOutdated] reloads it. Call it where it stays composed while the stream info collapses,
 * so collapsing does not ask again.
 */
@Composable
internal fun rememberChannelFollow(
    channelId: String?,
    followedIds: Set<String>?,
    readFollow: suspend (broadcasterId: String) -> FollowForChatMode,
    onListOutdated: () -> Unit,
): ChannelFollow {
    val id = channelId?.trim().orEmpty()
    var checked by remember(id) { mutableStateOf<FollowForChatMode>(FollowForChatMode.Unknown) }
    var returns by remember { mutableIntStateOf(0) }
    val returnsAtOpen = remember(id) { returns }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var stopped = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> stopped = true
                Lifecycle.Event.ON_START -> if (stopped) {
                    stopped = false
                    returns++
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val readFollowState = rememberUpdatedState(readFollow)
    val followedIdsState = rememberUpdatedState(followedIds)
    val listOutdated = rememberUpdatedState(onListOutdated)
    val listKnown = followedIds != null
    LaunchedEffect(id, listKnown, returns) {
        if (id.isEmpty() || !channelFollowNeedsRequest(followedIds, returned = returns != returnsAtOpen)) {
            return@LaunchedEffect
        }
        val read = readFollowState.value(id)
        checked = read
        if (followListOutdated(followedIdsState.value, read, id)) listOutdated.value()
    }
    val status = if (id.isEmpty()) ChannelFollowStatus.Unknown else channelFollowStatus(followedIds, checked, id)
    return ChannelFollow(status) { following ->
        checked = if (following) FollowForChatMode.Following(System.currentTimeMillis()) else FollowForChatMode.NotFollowing
    }
}

/**
 * A follow or an unfollow made in the app, for the followers-only chat plaque: a new follow counts from [nowMillis].
 * Outside that mode the plaque does not ask, so it stays unknown.
 */
internal fun chatModeFollowAfterChange(followersOnly: Boolean, following: Boolean, nowMillis: Long): FollowForChatMode =
    when {
        !followersOnly -> FollowForChatMode.Unknown
        following -> FollowForChatMode.Following(nowMillis)
        else -> FollowForChatMode.NotFollowing
    }

/** The heart beside the channel name: outlined while the user does not follow the channel, filled while they do. */
@Composable
internal fun ChannelFollowButton(following: Boolean, action: ChannelFollowAction) {
    val stateText = stringResource(if (following) R.string.channel_following else R.string.channel_not_following)
    IconButton(
        onClick = action::tap,
        enabled = !action.sending,
        modifier = Modifier
            .size(40.dp)
            .semantics { stateDescription = stateText },
    ) {
        Crossfade(targetState = following, label = "channelFollowHeart") { shownFollowing ->
            Icon(
                imageVector = if (shownFollowing) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(R.string.channel_follow),
                tint = if (shownFollowing) TwitchPurple else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
