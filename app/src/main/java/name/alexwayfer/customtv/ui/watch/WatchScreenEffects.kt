package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import name.alexwayfer.customtv.chat.ChatMentionSelf
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.chat.mergeThreadEntries
import name.alexwayfer.customtv.chat.threadEntriesFromKnown
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ChatThreadRepository
import name.alexwayfer.customtv.ui.HoldGestureTips

/** Mentions of the user in the live chat vibrate or sound, as the settings ask, and gather on their own card. */
@Composable
internal fun LiveMentionFeedback(
    chatViewModel: ChatViewModel,
    channel: String,
    account: WatchAccount,
    settings: AppSettings,
) {
    val self = ChatMentionSelf(account.login.orEmpty(), account.displayName.orEmpty(), settings.keywordPhrases)
    LaunchedEffect(chatViewModel, self) { chatViewModel.setMentionSelf(self) }
    MentionFeedback(
        messages = chatViewModel.messages,
        channel = channel,
        selfLogin = account.login,
        selfDisplayName = account.displayName,
        settings = settings,
    )
}

/** The raid the user started counts down and opens its target; the raid commands use the user's token. */
@Composable
internal fun WatchRaidEffects(
    channel: String,
    chatViewModel: ChatViewModel,
    outgoingRaid: OutgoingRaid?,
    account: WatchAccount,
    onOpenChannel: (String) -> Unit,
) {
    val openChannel = rememberUpdatedState(onOpenChannel)
    OutgoingRaidCountdown(
        raid = outgoingRaid,
        currentChannel = channel,
        onOpen = { openChannel.value(it) },
        onFinished = chatViewModel.raid::dismiss,
    )
    val readRaidToken = rememberUpdatedState(account.raidToken)
    val signedIn = account.signedIn
    LaunchedEffect(channel, signedIn) {
        val token = if (signedIn) readRaidToken.value() else null
        chatViewModel.setRaidToken(token)
    }
}

/** The signed-in user's own follow of a channel, read for their chatter card; null when logged out. */
@Composable
internal fun rememberOwnChannelFollow(account: WatchAccount): OwnChannelFollow? {
    val readOwnChatterFollow = rememberUpdatedState(account.ownChatterFollow)
    return remember(account.userId, account.login) {
        val login = account.login?.takeIf { it.isNotBlank() } ?: return@remember null
        OwnChannelFollow(userId = account.userId, login = login) { broadcasterId ->
            readOwnChatterFollow.value(broadcasterId)
        }
    }
}

/** The channel's live events, presence, and status, and the chat's connection while the screen is open. */
@Composable
internal fun WatchChannelEffects(scene: WatchScene) {
    val profile = scene.profile
    val account = scene.account
    val chatViewModel = scene.chatViewModel
    ChannelEventSubUpdates(
        broadcasterId = profile.id,
        signedIn = account.signedIn,
        signedInUserId = account.userId,
        accessToken = account.raidToken,
        chatViewModel = chatViewModel,
    )
    WatchPresenceEffect(channelLogin = scene.channel, isLive = profile.isLive)
    val liveStatusHandler = rememberUpdatedState(scene.navigation.onLiveStatus)
    LaunchedEffect(profile.isLive) {
        liveStatusHandler.value(profile.isLive)
    }
    RevealHeaderOnStreamInfoChange(
        snapshot = StreamInfoSnapshot(
            login = profile.login,
            title = profile.streamTitle,
            categoryName = profile.categoryName,
        ),
        chromeVisibility = scene.chrome,
    )
    HoldGestureTips(held = scene.chat.replySession != null)
    val loadRecentChat = scene.settings.loadRecentChatOnOpen
    LaunchedEffect(scene.channel) {
        chatViewModel.connect(scene.channel, loadRecentChat)
    }
    val minimized = scene.placement.minimized
    val inPictureInPicture = scene.placement.inPictureInPicture
    val onFullPlayerCovering = scene.navigation.onFullPlayerCovering
    LaunchedEffect(scene.channel, minimized, inPictureInPicture) {
        if (minimized || inPictureInPicture) return@LaunchedEffect
        withFrameNanos { }
        onFullPlayerCovering()
    }
    DisposableEffect(chatViewModel) {
        chatViewModel.setForeground(true)
        onDispose { chatViewModel.setForeground(false) }
    }
}

/** The reply thread fills in from the server and from the chat as it goes on. */
@Composable
internal fun ReplySessionEffects(chat: WatchChatState, messages: State<List<ChatMessage>>) {
    val threadRepository = remember { ChatThreadRepository() }
    val replyStarterId = chat.replySession?.starterId
    LaunchedEffect(replyStarterId) {
        val starterId = replyStarterId ?: return@LaunchedEffect
        val loaded = threadRepository.load(starterId) ?: return@LaunchedEffect
        chat.replySession = chat.replySession?.takeIf { it.starterId == starterId }?.let { current ->
            current.copy(entries = mergeThreadEntries(starterId, current.entries, loaded))
        }
    }
    // Collects the messages instead of keying on them, so a new chat message does not recompose this screen.
    LaunchedEffect(replyStarterId) {
        if (replyStarterId == null) return@LaunchedEffect
        snapshotFlow { messages.value }.collect { known ->
            val current = chat.replySession ?: return@collect
            if (current.starterId != replyStarterId) return@collect
            val local = threadEntriesFromKnown(current.starterId, null, known)
            val merged = mergeThreadEntries(current.starterId, current.entries, local)
            if (merged != current.entries) {
                chat.replySession = current.copy(entries = merged)
            }
        }
    }
}
