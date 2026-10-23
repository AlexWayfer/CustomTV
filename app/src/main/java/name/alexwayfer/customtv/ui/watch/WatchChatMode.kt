package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatModePlaque
import name.alexwayfer.customtv.chat.ChatModeViewer
import name.alexwayfer.customtv.chat.ChatRoomModes
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.FollowForChatMode
import name.alexwayfer.customtv.chat.SubForChatMode
import name.alexwayfer.customtv.chat.chatModePlaques
import name.alexwayfer.customtv.chat.chatModesExempt
import name.alexwayfer.customtv.chat.plaqueTicks
import name.alexwayfer.customtv.chat.slowModeBlocksSend
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.data.ChannelProfile
import kotlin.time.Duration.Companion.seconds

/** Whether the viewer follows or subscribes as far as the chat's modes need it; read again on each resume. */
@Stable
internal class ChatModeAccess {
    var follow by mutableStateOf<FollowForChatMode>(FollowForChatMode.Unknown)
    var subscription by mutableStateOf<SubForChatMode>(SubForChatMode.Unknown)
}

/** The chat's modes, what they hold the viewer to, and whether slow mode holds the next message. */
internal class WatchChatMode(
    val roomModes: ChatRoomModes,
    val access: ChatModeAccess,
    val slowModeSend: SlowModeSendState,
    /** The broadcaster, a moderator, or a VIP: the modes show without a countdown or an offer, and hold no send. */
    val viewerExempt: Boolean,
    val plaques: List<ChatModePlaque>,
    val slowSendBlocked: Boolean,
)

@Composable
internal fun rememberWatchChatMode(
    channel: String,
    profile: ChannelProfile,
    chatViewModel: ChatViewModel,
    account: WatchAccount,
    ownBadges: List<ChatBadge>?,
): WatchChatMode {
    val roomModes by chatViewModel.roomModes.collectAsStateWithLifecycle()
    val ownsChannel = account.login.equals(channel, ignoreCase = true)
    val access = rememberChatModeAccess(channel, roomModes, profile.id, account, ownsChannel)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val channelName = profile.displayName.ifBlank { channel }
    val viewerExempt = chatModesExempt(ownsChannel, ownBadges)
    val slowModeSend = rememberSlowModeSend(
        channel = channel,
        ownUserId = account.userId,
        liveMessages = chatViewModel.liveIrcMessages,
        slowSeconds = roomModes.slowSeconds,
        viewerExempt = viewerExempt,
    )
    // One list while its inputs stay, so a chat message that recomposes this screen does not recompose the input.
    val slowDeadlineMillis = slowModeSend.deadlineMillis
    val follow = access.follow
    val subscription = access.subscription
    val plaques = remember(
        roomModes, viewerExempt, follow, subscription, ownsChannel, nowMillis, slowDeadlineMillis, channelName,
    ) {
        chatModePlaques(
            modes = roomModes,
            viewer = ChatModeViewer(viewerExempt, follow, subscription, ownsChannel),
            nowMillis = nowMillis,
            slowDeadlineMillis = slowDeadlineMillis,
            channelName = channelName,
        )
    }
    val countdownRunning = plaques.any(::plaqueTicks)
    LaunchedEffect(countdownRunning) {
        if (!countdownRunning) return@LaunchedEffect
        while (true) {
            delay(1.seconds)
            nowMillis = System.currentTimeMillis()
        }
    }
    val slowSendBlocked = slowModeBlocksSend(
        plaques.filterIsInstance<ChatModePlaque.Slow>().firstOrNull(),
        awaitingOwnEcho = slowModeSend.awaitingOwnEchoUntilMillis != null,
    )
    return WatchChatMode(roomModes, access, slowModeSend, viewerExempt, plaques, slowSendBlocked)
}

@Composable
private fun rememberChatModeAccess(
    channel: String,
    roomModes: ChatRoomModes,
    broadcasterId: String?,
    account: WatchAccount,
    ownsChannel: Boolean,
): ChatModeAccess {
    val lifecycleOwner = LocalLifecycleOwner.current
    var followResume by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) followResume++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val access = remember(channel) { ChatModeAccess() }
    val readOwnFollowState = rememberUpdatedState(account.readOwnFollow)
    val readOwnSubscriptionState = rememberUpdatedState(account.readOwnSubscription)
    val followersMinutes = roomModes.followersOnlyMinutes
    val subscribersOnly = roomModes.subscribersOnly
    val signedIn = account.signedIn
    // The owner always counts as following and subscribed, so neither is asked for their own channel.
    LaunchedEffect(channel, followersMinutes, signedIn, ownsChannel, broadcasterId, followResume) {
        access.follow = when {
            followersMinutes == null || ownsChannel -> FollowForChatMode.Unknown
            !signedIn -> FollowForChatMode.NotFollowing
            broadcasterId.isNullOrBlank() -> FollowForChatMode.Unknown
            else -> readOwnFollowState.value(broadcasterId)
        }
    }
    LaunchedEffect(channel, subscribersOnly, signedIn, ownsChannel, broadcasterId, followResume) {
        access.subscription = when {
            !subscribersOnly || ownsChannel -> SubForChatMode.Unknown
            !signedIn -> SubForChatMode.NotSubscribed
            broadcasterId.isNullOrBlank() -> SubForChatMode.Unknown
            else -> readOwnSubscriptionState.value(broadcasterId)
        }
    }
    return access
}

/** The channel's follow and the heart that changes it; a change also updates what the chat's modes hold. */
internal class WatchFollow(val channel: ChannelFollow, val action: FollowAction)

@Composable
internal fun rememberWatchFollow(
    channel: String,
    profile: ChannelProfile,
    account: WatchAccount,
    mode: WatchChatMode,
): WatchFollow {
    val channelFollow = rememberChannelFollow(
        profile.id,
        account.userId,
        account.followedChannelIds,
        account.readOwnFollow,
        account.onFollowsChanged,
    )
    val followersOnly = mode.roomModes.followersOnlyMinutes != null
    val action = rememberChannelFollowAction(
        channel = channel,
        channelId = profile.id,
        channelName = profile.displayName.ifBlank { channel },
        signedIn = account.signedIn,
        following = channelFollow.status == ChannelFollowStatus.Following,
        onFollowChanged = { following ->
            channelFollow.set(following)
            mode.access.follow = chatModeFollowAfterChange(followersOnly, following, System.currentTimeMillis())
            account.onFollowsChanged()
        },
        onLogIn = account.onLogIn,
    )
    return WatchFollow(channelFollow, action)
}
