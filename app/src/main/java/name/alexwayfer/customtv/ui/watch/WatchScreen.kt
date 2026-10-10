package name.alexwayfer.customtv.ui.watch




import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.LocalActivity













import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxWidth




import androidx.compose.foundation.lazy.LazyListState


















import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf


import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color











import androidx.compose.ui.platform.LocalContext


import androidx.compose.foundation.text.input.TextFieldState
























import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import androidx.lifecycle.viewmodel.compose.viewModel

import kotlinx.coroutines.delay
import name.alexwayfer.customtv.MainActivity









import name.alexwayfer.customtv.chat.ChatModePlaque
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.FollowForChatMode
import name.alexwayfer.customtv.chat.SubForChatMode
import name.alexwayfer.customtv.chat.TwitchPlaqueAction
import name.alexwayfer.customtv.chat.subscribersPlaqueAction
import name.alexwayfer.customtv.chat.chatModePlaques
import name.alexwayfer.customtv.chat.plaqueTicks
import name.alexwayfer.customtv.chat.slowModeBlocksSend
import name.alexwayfer.customtv.chat.ChatReplySession
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.mergeThreadEntries
import name.alexwayfer.customtv.chat.replyParentMessageId
import name.alexwayfer.customtv.chat.replySessionCancelsToStarter
import name.alexwayfer.customtv.chat.replySessionChooses
import name.alexwayfer.customtv.chat.replySessionFor
import name.alexwayfer.customtv.chat.threadEntriesFromKnown
import name.alexwayfer.customtv.chat.threadReplyHidesChrome
import name.alexwayfer.customtv.data.ChatThreadRepository









import name.alexwayfer.customtv.player.TwitchPlaybackTarget
import name.alexwayfer.customtv.player.streamInfoExpandedUnderPlayer



import name.alexwayfer.customtv.ui.HoldGestureTips
import name.alexwayfer.customtv.ui.components.rememberChannelProfile
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.settings.SettingsViewModel
import name.alexwayfer.customtv.ui.settings.beforeRestore










import name.alexwayfer.customtv.data.collaborationRowOpensChannel

import kotlin.time.Duration.Companion.seconds
import name.alexwayfer.customtv.ui.theme.TwitchSurface

@Composable
fun WatchScreen(
    channel: String,
    chatViewModel: ChatViewModel,
    onMinimize: () -> Unit,
    onFullPlayerCovering: () -> Unit,
    onFullPlayerExpanded: () -> Unit,
    onLogIn: () -> Unit,
    onMinimizeDragDismissesKeyboard: () -> Unit = {},
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onLiveStatus: (Boolean) -> Unit = {},
    onOpenChannel: (String) -> Unit = {},
    onOpenChannelProfile: (String) -> Unit = {},
    onOpenSettings: (SettingsScrollTarget) -> Unit = {},
    onMinimizeOverSection: () -> Unit = {},
    returnsToPreviousChannel: Boolean = false,
    onReturnToPreviousChannel: () -> Unit = {},
    signedIn: Boolean = false,
    signedInUserId: String? = null,
    signedInLogin: String? = null,
    signedInDisplayName: String? = null,
    raidToken: suspend () -> String? = { null },
    ownChatterFollow: suspend (broadcasterId: String) -> ChatterFollow? = { null },
    readOwnFollow: suspend (broadcasterId: String) -> FollowForChatMode = { FollowForChatMode.Unknown },
    readOwnSubscription: suspend (broadcasterId: String) -> SubForChatMode = { SubForChatMode.Unknown },
    followedChannelIds: Set<String>? = null,
    onFollowsChanged: () -> Unit = {},
    inPictureInPicture: Boolean = false,
    minimized: Boolean = false,
    appNavigationBarHeightPx: Int = 0,
    chatSettingsViewModel: ChatSettingsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val recentChatLoadState by chatViewModel.recentChatLoadState.collectAsStateWithLifecycle()
    val sessionChatters by chatViewModel.sessionChatters.collectAsStateWithLifecycle()
    val storedChatSettings by chatSettingsViewModel.settings.collectAsStateWithLifecycle()
    val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val playbackSettings = appSettings.beforeRestore()
    val sevenTvEnabled = playbackSettings.sevenTvEmotes
    val ffzEnabled = playbackSettings.ffzEmotes
    val bttvEnabled = playbackSettings.bttvEmotes
    val keywordPhrases = playbackSettings.keywordPhrases
    MentionFeedback(
        messages = chatViewModel.messages,
        channel = channel,
        selfLogin = signedInLogin,
        selfDisplayName = signedInDisplayName,
        vibrate = playbackSettings.mentionVibration,
        vibrationMs = playbackSettings.mentionVibrationMs,
        vibrationPercent = playbackSettings.mentionVibrationPercent,
        sound = playbackSettings.mentionSound,
        soundUri = playbackSettings.mentionSoundUri,
        phrases = keywordPhrases,
    )
    val badgeUrls by chatViewModel.badgeUrls.collectAsStateWithLifecycle()
    val sevenTvEmotes by chatViewModel.sevenTvEmotes.collectAsStateWithLifecycle()
    val ffzEmotes by chatViewModel.ffzEmotes.collectAsStateWithLifecycle()
    val bttvEmotes by chatViewModel.bttvEmotes.collectAsStateWithLifecycle()
    val nickColors by chatViewModel.nickColors.collectAsStateWithLifecycle()
    val pinnedChat by chatViewModel.pinnedChat.collectAsStateWithLifecycle()
    val outgoingRaid by chatViewModel.outgoingRaid.collectAsStateWithLifecycle()
    val channelEvents by chatViewModel.channelEvents.collectAsStateWithLifecycle()
    val openChannel = rememberUpdatedState(onOpenChannel)
    OutgoingRaidCountdown(
        raid = outgoingRaid,
        currentChannel = channel,
        onOpen = { openChannel.value(it) },
        onFinished = chatViewModel::dismissOutgoingRaid,
    )
    val readRaidToken = rememberUpdatedState(raidToken)
    val readOwnChatterFollow = rememberUpdatedState(ownChatterFollow)
    val ownFollow = remember(signedInUserId, signedInLogin) {
        val login = signedInLogin?.takeIf { it.isNotBlank() } ?: return@remember null
        OwnChannelFollow(userId = signedInUserId, login = login) { broadcasterId ->
            readOwnChatterFollow.value(broadcasterId)
        }
    }
    LaunchedEffect(channel, signedIn) {
        val token = if (signedIn) readRaidToken.value() else null
        chatViewModel.setRaidToken(token)
    }
    val channelProfile = rememberChannelProfile(channel, pollEvery = 30.seconds, forceRefresh = true)
    val roomModes by chatViewModel.roomModes.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var followResume by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) followResume++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var followForMode by remember(channel) { mutableStateOf<FollowForChatMode>(FollowForChatMode.Unknown) }
    val readOwnFollowState = rememberUpdatedState(readOwnFollow)
    val readOwnSubscriptionState = rememberUpdatedState(readOwnSubscription)
    val followersMinutes = roomModes.followersOnlyMinutes
    val subscribersOnly = roomModes.subscribersOnly
    val broadcasterId = channelProfile.id
    val channelFollow = rememberChannelFollow(broadcasterId, followedChannelIds, readOwnFollow, onFollowsChanged)
    val followAction = rememberChannelFollowAction(
        channel = channel,
        channelId = broadcasterId,
        channelName = channelProfile.displayName.ifBlank { channel },
        signedIn = signedIn,
        following = channelFollow.status == ChannelFollowStatus.Following,
        onFollowChanged = { following ->
            channelFollow.set(following)
            followForMode = chatModeFollowAfterChange(followersMinutes != null, following, System.currentTimeMillis())
            onFollowsChanged()
        },
        onLogIn = onLogIn,
    )
    LaunchedEffect(channel, followersMinutes, signedIn, broadcasterId, followResume) {
        followForMode = when {
            followersMinutes == null -> FollowForChatMode.Unknown
            !signedIn -> FollowForChatMode.NotFollowing
            broadcasterId.isNullOrBlank() -> FollowForChatMode.Unknown
            else -> readOwnFollowState.value(broadcasterId)
        }
    }
    var subForMode by remember(channel) { mutableStateOf<SubForChatMode>(SubForChatMode.Unknown) }
    LaunchedEffect(channel, subscribersOnly, signedIn, broadcasterId, followResume) {
        subForMode = when {
            !subscribersOnly -> SubForChatMode.Unknown
            !signedIn -> SubForChatMode.NotSubscribed
            broadcasterId.isNullOrBlank() -> SubForChatMode.Unknown
            else -> readOwnSubscriptionState.value(broadcasterId)
        }
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val channelName = channelProfile.displayName.ifBlank { channel }
    val viewerIsBroadcaster = signedInLogin.equals(channel, ignoreCase = true)
    val slowModeSend = rememberSlowModeSend(
        channel = channel,
        ownUserId = signedInUserId,
        liveMessages = chatViewModel.liveIrcMessages,
        slowSeconds = roomModes.slowSeconds,
        viewerIsBroadcaster = viewerIsBroadcaster,
    )
    // One list while its inputs stay, so a chat message that recomposes this screen does not recompose the input.
    val slowDeadlineMillis = slowModeSend.deadlineMillis
    val modePlaques = remember(
        roomModes, viewerIsBroadcaster, followForMode, subForMode, nowMillis, slowDeadlineMillis, channelName,
    ) {
        chatModePlaques(
            modes = roomModes,
            viewerIsBroadcaster = viewerIsBroadcaster,
            follow = followForMode,
            subscription = subForMode,
            nowMillis = nowMillis,
            slowDeadlineMillis = slowDeadlineMillis,
            channelName = channelName,
        )
    }
    val modeCountdownRunning = modePlaques.any(::plaqueTicks)
    LaunchedEffect(modeCountdownRunning) {
        if (!modeCountdownRunning) return@LaunchedEffect
        while (true) {
            delay(1.seconds)
            nowMillis = System.currentTimeMillis()
        }
    }
    val slowSendBlocked = slowModeBlocksSend(
        modePlaques.filterIsInstance<ChatModePlaque.Slow>().firstOrNull(),
        awaitingOwnEcho = slowModeSend.awaitingOwnEchoUntilMillis != null,
    )
    val context = LocalContext.current
    ChannelEventSubUpdates(
        broadcasterId = channelProfile.id,
        signedIn = signedIn,
        signedInUserId = signedInUserId,
        accessToken = raidToken,
        chatViewModel = chatViewModel,
    )
    WatchPresenceEffect(channelLogin = channel, isLive = channelProfile.isLive)
    val liveStatusHandler = rememberUpdatedState(onLiveStatus)
    LaunchedEffect(channelProfile.isLive) {
        liveStatusHandler.value(channelProfile.isLive)
    }
    val chromeVisibility = remember(channel) { PlayerChromeVisibility() }
    RevealHeaderOnStreamInfoChange(
        snapshot = StreamInfoSnapshot(
            login = channelProfile.login,
            title = channelProfile.streamTitle,
            categoryName = channelProfile.categoryName,
        ),
        chromeVisibility = chromeVisibility,
    )
    var pendingChatLink by remember(channel) { mutableStateOf<String?>(null) }
    var showCollaboration by remember(channel) { mutableStateOf(false) }
    var replySession by remember(channel) { mutableStateOf<ChatReplySession?>(null) }
    HoldGestureTips(held = replySession != null)
    var replyFocusRequest by remember(channel) { mutableStateOf<OneShotRequest<Unit>?>(null) }
    var fieldCardOpen by remember(channel) { mutableStateOf<OneShotRequest<ChatterCardRequest>?>(null) }
    val ownBadges = rememberOwnChatBadges(signedInUserId, channelProfile.id, signedInLogin, { messages }, badgeUrls)
    var chatComposerOpen by remember(channel) { mutableStateOf(false) }
    val replyHidesChrome = threadReplyHidesChrome(replying = replySession != null, composerOpen = chatComposerOpen)
    val threadRepository = remember { ChatThreadRepository() }
    val replyStarterId = replySession?.starterId
    LaunchedEffect(replyStarterId) {
        val starterId = replyStarterId ?: return@LaunchedEffect
        val loaded = threadRepository.load(starterId) ?: return@LaunchedEffect
        replySession = replySession?.takeIf { it.starterId == starterId }?.let { current ->
            current.copy(entries = mergeThreadEntries(starterId, current.entries, loaded))
        }
    }
    // Collects the messages instead of keying on them, so a new chat message does not recompose this screen.
    LaunchedEffect(replyStarterId) {
        if (replyStarterId == null) return@LaunchedEffect
        snapshotFlow { messages }.collect { known ->
            val current = replySession ?: return@collect
            if (current.starterId != replyStarterId) return@collect
            val local = threadEntriesFromKnown(current.starterId, null, known)
            val merged = mergeThreadEntries(current.starterId, current.entries, local)
            if (merged != current.entries) {
                replySession = current.copy(entries = merged)
            }
        }
    }
    val collaboration = rememberCollaboration(
        channelProfile.id.takeIf { channelProfile.sharedViewerCount != null },
        channel,
    )
    val activity = LocalActivity.current as MainActivity
    val chatListState = remember(channel) { LazyListState() }
    val chatHighlightPins = rememberTemporaryHighlightPins(channel)
    var chatStickToBottom by remember(channel) { mutableStateOf(true) }
    val chatDraft = remember(channel) { TextFieldState() }
    val playerState = rememberMinimizablePlayerState(minimized)

    fun dismissToChannelProfile(target: String) {
        playerState.minimize(
            before = { onOpenChannelProfile(target) },
            after = onMinimizeOverSection,
        )
    }

    fun dismissToSettings(target: SettingsScrollTarget) {
        playerState.minimize(before = { onOpenSettings(target) }, after = onMinimizeOverSection)
    }

    LaunchedEffect(channel) {
        chatViewModel.connect(channel, playbackSettings.loadRecentChatOnOpen)
    }
    LaunchedEffect(channel, minimized, inPictureInPicture) {
        if (minimized || inPictureInPicture) return@LaunchedEffect
        withFrameNanos { }
        onFullPlayerCovering()
    }
    DisposableEffect(chatViewModel) {
        chatViewModel.setForeground(true)
        onDispose { chatViewModel.setForeground(false) }
    }

    val sheetKeyboard = rememberSheetKeyboard()
    // One lambda for the screen's life, so the static local never redraws the stream screen.
    val dismissToSettingsLatest by rememberUpdatedState<(SettingsScrollTarget) -> Unit>(::dismissToSettings)
    val openSettings = remember { { target: SettingsScrollTarget -> dismissToSettingsLatest(target) } }
    CompositionLocalProvider(
        LocalSheetKeyboard provides sheetKeyboard,
        LocalOpenSettings provides openSettings,
    ) {
    // Under the player, and over it in full screen, where it shows whole with the controls on a see-through ground
    // and without the tags.
    val channelHeader: @Composable (expanded: Boolean, overVideo: Boolean) -> Unit = { expanded, overVideo ->
        ChannelHeader(
            channel = channel,
            profile = channelProfile,
            expanded = expanded,
            onViewersClick = if (
                channelProfile.sharedViewerCount != null &&
                !channelProfile.id.isNullOrBlank()
            ) {
                { sheetKeyboard.open { showCollaboration = true } }
            } else {
                null
            },
            // Toggles what the panel shows, so a tap on the panel kept collapsed by the keyboard does not hide the controls.
            onToggle = { chromeVisibility.show(!expanded) },
            onOpenProfile = { dismissToChannelProfile(channel) },
            followStatus = channelFollow.status,
            followAction = followAction,
            onOpenSettings = { dismissToSettings(SettingsScrollTarget.Notifications) },
            onHeldChange = { held -> chromeVisibility.headerHeld = held },
            containerColor = if (overVideo) Color.Transparent else TwitchSurface,
            showTags = !overVideo,
        )
    }
    MinimizablePlayer(
        state = playerState,
        chromeVisibility = chromeVisibility,
        target = TwitchPlaybackTarget.Channel(channel),
        minimized = minimized,
        onMinimize = onMinimize,
        onExpand = onExpand,
        onClose = onClose,
        onBack = if (returnsToPreviousChannel) onReturnToPreviousChannel else null,
        inPictureInPicture = inPictureInPicture,
        appNavigationBarHeightPx = appNavigationBarHeightPx,
        onMinimizeDragDismissesKeyboard = onMinimizeDragDismissesKeyboard,
        onExpanded = onFullPlayerExpanded,
        fullscreenInfo = { channelHeader(true, true) },
        onFullscreenSendChat = {
            if (signedIn) replyFocusRequest = OneShotRequest(Unit) else onLogIn()
        },
        overlays = { compactSettled ->
            val broadcasterId = channelProfile.id
            if (showCollaboration && !broadcasterId.isNullOrBlank() && !compactSettled) {
                CollaborationSheet(
                    result = collaboration,
                    sharedViewerCount = channelProfile.sharedViewerCount,
                    currentLogin = channel,
                    onOpenChannel = { login ->
                        if (!collaborationRowOpensChannel(login, channel)) return@CollaborationSheet
                        showCollaboration = false
                        sheetKeyboard.onDismiss()
                        onOpenChannel(login)
                    },
                    onDismiss = {
                        showCollaboration = false
                        sheetKeyboard.onDismiss()
                    },
                )
            }
            ChannelFollowDialogs(followAction, hidden = compactSettled)
            ChatOpenLinkOverlay(
                url = pendingChatLink,
                onDismiss = { pendingChatLink = null },
                onOpen = { url ->
                    pendingChatLink = null
                    activity.openExternalLink(url)
                },
            )
        },
    ) { layout ->
        // A compact overlaid chat takes only the height of its latest messages.
        val chatFills = layout.overlayChat != FullscreenOverlayChatHeight.Compact
        Column(
            modifier = Modifier
                .then(if (chatFills) Modifier.weight(1f) else Modifier)
                .fillMaxWidth()
                .blockClicks(),
        ) {
            // Beside a full screen player it slides up out of the way, and the chat takes the freed room. It stays with
            // the keyboard up, so a tap on the player still expands it. A chat laid over the video is a block in its
            // corner, where the folding info would show as a strip, so there it goes at once.
            val headerSlides = !layout.chatOverVideo()
            AnimatedVisibility(
                visible = !layout.fullscreen,
                enter = if (headerSlides) expandVertically(expandFrom = Alignment.Bottom) else EnterTransition.None,
                exit = if (headerSlides) shrinkVertically(shrinkTowards = Alignment.Bottom) else ExitTransition.None,
            ) {
            channelHeader(streamInfoExpandedUnderPlayer(chromeVisibility.headerExpanded, chatComposerOpen), false)
            }
            Column(
                modifier = Modifier
                    .then(if (chatFills) Modifier.weight(1f) else Modifier)
                    .fillMaxWidth(),
            ) {
                val replyToMessage: (ChatMessage) -> Unit = { message ->
                    val known = messages
                    sheetKeyboard.open {
                        replySession = replySessionFor(message, known)
                        if (replySession != null) replyFocusRequest = OneShotRequest(Unit)
                    }
                }
                ChatPane(
                    channel = channel,
                    channelProfile = channelProfile,
                    messages = { messages },
                    recentAuthorMessages = chatViewModel.recentAuthorMessages,
                    badgeUrls = badgeUrls,
                    sevenTvEmotes = sevenTvEmotes,
                    ffzEmotes = ffzEmotes,
                    bttvEmotes = bttvEmotes,
                    nickColors = { nickColors },
                    listState = chatListState,
                    highlightPins = chatHighlightPins,
                    stickToBottom = chatStickToBottom,
                    onStickToBottomChange = { chatStickToBottom = it },
                    onOpenLink = { pendingChatLink = it },
                    onOpenChatterProfile = ::dismissToChannelProfile,
                    backgroundAlpha = layout.chatBackgroundAlpha,
                    selfLogin = signedInLogin.orEmpty(),
                    selfDisplayName = signedInDisplayName.orEmpty(),
                    ownFollow = ownFollow,
                    replyingToMessageId = replySession?.targetId,
                    onReply = replyToMessage,
                    fieldCardOpen = fieldCardOpen,
                    noticesHidden = chatComposerOpen,
                    overlayHeight = layout.overlayChat,
                    onOverlayExpand = layout.onOverlayChatExpand,
                    overlayMorph = layout.overlayChatMorph,
                    compactHeight = layout.compactChatHeight,
                    chatSettingsViewModel = chatSettingsViewModel,
                    settingsViewModel = settingsViewModel,
                    notices = { appearance ->
                        ChatNotices(
                            messages = { messages },
                            pin = pinnedChat,
                            raid = outgoingRaid,
                            events = channelEvents,
                            pointsIconUrl = channelProfile.channelPointsIconUrl,
                            channelId = broadcasterId,
                            channelLogin = channel,
                            selfLogin = signedInLogin,
                            appearance = appearance,
                            onHidePin = chatViewModel::hidePinnedForSelf,
                            onOpenTwitchChat = {
                                openTwitchChatInBrowser(
                                    context = context,
                                    channelLogin = channel,
                                    sheetHeightPx = chatBrowserSheetHeightPx(
                                        containerHeightPx = layout.containerHeightPx,
                                        playerBottomPx = layout.playerBottomPx(),
                                    ),
                                )
                            },
                            onHideEvent = chatViewModel::hideChannelEvent,
                        )
                    },
                    listTop = { RecentChatLoadingIndicator(recentChatLoadState) },
                    belowList = { appearance ->
                        replySession?.let { session ->
                            ChatThreadPanel(
                                session = session,
                                onChoose = { messageId ->
                                    replySession = replySession?.let { replySessionChooses(it, messageId) }
                                },
                                onCancel = {
                                    replySession = replySession?.let { replySessionCancelsToStarter(it) }
                                },
                                onClose = {
                                    replySession = null
                                    sheetKeyboard.onDismiss()
                                },
                                appearance = appearance,
                            )
                        }
                    },
                )
            }
        }
        // Over the video the bar folds away until Send chat opens the keyboard; it stays composed to take the focus.
        val inputBarShown = animateFloatAsState(
            targetValue = if (chatInputBarFolded(layout.chatOverVideo(), chatComposerOpen)) 0f else 1f,
            label = "chat input bar shown",
        )
        Box(modifier = Modifier.foldedAway { inputBarShown.value }) {
            ChatInputBar(
                channelLogin = channel,
                browserSheetHeightPx = {
                    chatBrowserSheetHeightPx(
                        containerHeightPx = layout.containerHeightPx,
                        playerBottomPx = layout.playerBottomPx(),
                    )
                },
                canSend = signedIn,
                onLogIn = onLogIn,
                broadcasterId = channelProfile.id,
                chatRules = channelProfile.chatRules,
                senderId = signedInUserId,
                liveChatMessages = chatViewModel.liveIrcMessages,
                accessToken = raidToken,
                replyParentMessageId = replyParentMessageId(replySession),
                replyFocusRequest = replyFocusRequest,
                sessionChatters = { sessionChatters },
                channelOwner = ChatNick(
                    login = channel,
                    displayName = channelProfile.displayName.ifBlank { channel },
                ),
                chatSettings = storedChatSettings,
                onReadableColorsChange = chatSettingsViewModel::setReadableColors,
                onTimestampsChange = chatSettingsViewModel::setTimestamps,
                onTextSizeChange = chatSettingsViewModel::setTextSize,
                onSmoothChatScrollChange = chatSettingsViewModel::setSmoothChatScroll,
                onRefreshEmotes = chatViewModel::refreshEmotes,
                onRefreshLabels = ChatterLabelsRepository::refresh,
                signedInLogin = signedInLogin,
                signedInDisplayName = signedInDisplayName,
                channelPoints = { visible -> ChannelPointsChip(channel, channelProfile.channelPointsIconUrl, channelEvents.prediction, visible) },
                // Hidden for the whole reply, keyboard up or not. Called on every pass, so the offer keeps its state
                // and returns once the thread is closed, until the user closes the offer itself.
                shareOffer = rememberChatShareOffer(channel, channelProfile.channelPointsIconUrl)
                    ?.takeIf { replySession == null },
                onOpenOwnProfile = signedInLogin?.takeIf { it.isNotBlank() }?.let { login ->
                    {
                        fieldCardOpen = OneShotRequest(
                            ownChatterCardRequest(ownBadges, login, signedInDisplayName.orEmpty(), signedInUserId),
                        )
                    }
                },
                onOpenChatterCard = { login ->
                    fieldCardOpen = OneShotRequest(
                        if (login.equals(signedInLogin, ignoreCase = true)) {
                            ownChatterCardRequest(ownBadges, login, signedInDisplayName.orEmpty(), signedInUserId)
                        } else {
                            chatterCardRequestForLogin(login, messages, badgeUrls)
                        },
                    )
                },
                draft = chatDraft,
                sevenTvEnabled = sevenTvEnabled,
                bttvEnabled = bttvEnabled,
                ffzEnabled = ffzEnabled,
                emoteCompletionWithoutColon = playbackSettings.emoteCompletionWithoutColon,
                keepKeyboardAfterSend = playbackSettings.keepKeyboardAfterSend,
                sevenTvEmotes = sevenTvEmotes,
                bttvEmotes = bttvEmotes,
                ffzEmotes = ffzEmotes,
                onSent = {
                    slowModeSend.onSent(roomModes.slowSeconds, viewerIsBroadcaster)
                    replySession = null
                    chatStickToBottom = true
                },
                plaques = if (replyHidesChrome) emptyList() else modePlaques,
                slowSendBlocked = slowSendBlocked,
                onFollow = followAction::tap,
                onSubscribe = {
                    when (val action = subscribersPlaqueAction(signedIn, channel)) {
                        TwitchPlaqueAction.LogIn -> onLogIn()
                        is TwitchPlaqueAction.Open -> {
                            context.startActivity(Intent(Intent.ACTION_VIEW, action.url.toUri()))
                        }
                    }
                },
                onComposerOpenChange = {
                    chatComposerOpen = it
                    layout.onComposerOpenChange(it)
                },
                fullscreen = layout.fullscreen,
                overVideo = layout.chatOverVideo(),
                onPickerPanelPx = layout.onComposerPanelPx,
            )
        }
    }
    }
}
