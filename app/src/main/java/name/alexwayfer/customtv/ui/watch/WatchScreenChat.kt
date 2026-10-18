package name.alexwayfer.customtv.ui.watch

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.TwitchPlaqueAction
import name.alexwayfer.customtv.chat.replyParentMessageId
import name.alexwayfer.customtv.chat.replySessionCancelsToStarter
import name.alexwayfer.customtv.chat.replySessionChooses
import name.alexwayfer.customtv.chat.subscribersPlaqueAction
import name.alexwayfer.customtv.chat.threadReplyHidesChrome
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.links.openOutsideApp
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget

/** The live chat with its notices over it and the reply thread under it. */
@Composable
internal fun ColumnScope.WatchChatPane(scene: WatchScene, layout: MinimizablePlayerLayout) {
    val chat = scene.chat
    val data = scene.data
    ChatRecapPresenceEffect(
        channelId = scene.profile.id,
        shown = !scene.placement.minimized && !scene.placement.inPictureInPicture,
    )
    ChatPane(
        channel = scene.channel,
        channelProfile = scene.profile,
        messages = { data.messages.value },
        chatterHistory = scene.chatViewModel.chatterHistory,
        badgeUrls = data.badgeUrls.value,
        sevenTvEmotes = data.sevenTvEmotes.value,
        ffzEmotes = data.ffzEmotes.value,
        bttvEmotes = data.bttvEmotes.value,
        nickColors = { data.nickColors.value },
        listState = chat.listState,
        highlightPins = scene.highlightPins,
        stickToBottom = chat.stickToBottom,
        onStickToBottomChange = { chat.stickToBottom = it },
        onOpenLink = { chat.pendingChatLink = it },
        onOpenChatterProfile = scene::dismissToChannelProfile,
        backgroundAlpha = layout.chatBackgroundAlpha,
        selfLogin = scene.account.login.orEmpty(),
        selfDisplayName = scene.account.displayName.orEmpty(),
        ownFollow = scene.ownFollow,
        replyingToMessageId = chat.replySession?.targetId,
        onReply = scene::replyTo,
        onMention = scene::mention,
        fieldCardOpen = chat.fieldCardOpen,
        noticesHidden = chat.composerOpen,
        overlayHeight = layout.overlayChat,
        onOverlayExpand = layout.onOverlayChatExpand,
        overlayMorph = layout.overlayChatMorph,
        compactHeight = layout.compactChatHeight,
        chatSettingsViewModel = scene.chatSettingsViewModel,
        settingsViewModel = scene.settingsViewModel,
        notices = { appearance -> WatchChatNotices(scene, layout, appearance) },
        listTop = { RecentChatLoadingIndicator(data.recentChatLoadState.value, data.reconnecting.value) },
        belowList = { appearance -> WatchThreadPanel(scene, appearance) },
    )
}

@Composable
private fun WatchChatNotices(scene: WatchScene, layout: MinimizablePlayerLayout, appearance: ChatAppearance) {
    val context = LocalContext.current
    val data = scene.data
    val chatViewModel = scene.chatViewModel
    ChatNotices(
        messages = { data.messages.value },
        pin = data.pinnedChat.value,
        raid = data.outgoingRaid.value,
        events = data.channelEvents.value,
        pointsIconUrl = scene.profile.channelPointsIconUrl,
        channelId = scene.profile.id,
        channelLogin = scene.channel,
        selfLogin = scene.account.login,
        appearance = appearance,
        onHidePin = chatViewModel.pins::hideForSelf,
        onOpenTwitchChat = {
            openTwitchChatInBrowser(
                context = context,
                channelLogin = scene.channel,
                sheetHeightPx = layout.browserSheetHeightPx(),
            )
        },
        onHideEvent = chatViewModel::hideChannelEvent,
    )
}

/** A browser sheet opened from the chat stops at the player's bottom edge. */
private fun MinimizablePlayerLayout.browserSheetHeightPx(): Int = chatBrowserSheetHeightPx(
    containerHeightPx = containerHeightPx,
    playerBottomPx = playerBottomPx(),
)

@Composable
private fun WatchThreadPanel(scene: WatchScene, appearance: ChatAppearance) {
    val chat = scene.chat
    chat.replySession?.let { session ->
        ChatThreadPanel(
            session = session,
            onChoose = { messageId ->
                chat.replySession = chat.replySession?.let { replySessionChooses(it, messageId) }
            },
            onCancel = {
                chat.replySession = chat.replySession?.let { replySessionCancelsToStarter(it) }
            },
            onClose = {
                chat.replySession = null
                scene.sheetKeyboard.onDismiss()
            },
            appearance = appearance,
        )
    }
}

/** The message bar; over the video it folds away until Send chat opens the keyboard, and stays to take the focus. */
@Composable
internal fun WatchInputBar(scene: WatchScene, layout: MinimizablePlayerLayout) {
    val inputBarShown = animateFloatAsState(
        targetValue = if (chatInputBarFolded(layout.chatOverVideo(), scene.chat.composerOpen)) 0f else 1f,
        label = "chat input bar shown",
    )
    val context = LocalContext.current
    Box(modifier = Modifier.foldedAway { inputBarShown.value }) {
        ChatInputBar(
            channel = scene.inputChannel(layout),
            user = scene.inputUser(),
            compose = watchCompose(scene),
            menu = scene.chatMenu(),
            bar = scene.inputBarLook(layout, context),
        )
    }
}

private fun WatchScene.inputChannel(layout: MinimizablePlayerLayout) = ChatInputChannel(
    login = channel,
    broadcasterId = profile.id,
    owner = ChatNick(login = channel, displayName = profile.displayName.ifBlank { channel }),
    rules = profile.chatRules,
    liveMessages = chatViewModel.liveIrcMessages,
    browserSheetHeightPx = { layout.browserSheetHeightPx() },
)

private fun WatchScene.inputUser() = ChatInputUser(
    canSend = account.signedIn,
    userId = account.userId,
    login = account.login,
    displayName = account.displayName,
    accessToken = account.raidToken,
    onLogIn = account.onLogIn,
)

@Composable
private fun watchCompose(scene: WatchScene): ChatCompose {
    val settings = scene.settings
    val data = scene.data
    val chat = scene.chat
    return ChatCompose(
        draft = chat.draft,
        emotes = ChatInputEmotes(
            sevenTvEnabled = settings.sevenTvEmotes,
            bttvEnabled = settings.bttvEmotes,
            ffzEnabled = settings.ffzEmotes,
            sevenTv = data.sevenTvEmotes.value,
            bttv = data.bttvEmotes.value,
            ffz = data.ffzEmotes.value,
            completionWithoutColon = settings.emoteCompletionWithoutColon,
        ),
        replyParentMessageId = replyParentMessageId(chat.replySession),
        replyFocusRequest = chat.replyFocusRequest,
        mentionRequest = chat.mentionRequest,
        sessionChatters = { data.sessionChatters.value },
        chatterLastSpokeAt = { scene.chatViewModel.chatterRecency.lastSpokeAt },
        // Hidden for the whole reply, keyboard up or not. Called on every pass, so the offer keeps its
        // state and returns once the thread is closed, until the user closes the offer itself.
        shareOffer = rememberChatShareOffer(scene.channel, scene.profile.channelPointsIconUrl)
            ?.takeIf { chat.replySession == null },
        keepKeyboardAfterSend = settings.keepKeyboardAfterSend,
        slowSendBlocked = scene.mode.slowSendBlocked,
        onSent = {
            scene.mode.slowModeSend.onSent(scene.mode.roomModes.slowSeconds, scene.mode.viewerIsBroadcaster)
            chat.replySession = null
            chat.stickToBottom = chatFollowsAfterSend(chat.stickToBottom, settings.scrollToLatestAfterSend)
        },
        onOpenChatterCard = scene::openChatterCard,
    )
}

private fun WatchScene.chatMenu() = ChatMenu(
    settings = storedChatSettings,
    onReadableColorsChange = chatSettingsViewModel::setReadableColors,
    onTimestampsChange = chatSettingsViewModel::setTimestamps,
    onTextSizeChange = chatSettingsViewModel::setTextSize,
    onSmoothChatScrollChange = chatSettingsViewModel::setSmoothChatScroll,
    onRefreshEmotes = chatViewModel::refreshEmotes,
    onRefreshLabels = ChatterLabelsRepository::refresh,
    onOpenOwnProfile = account.login?.takeIf { it.isNotBlank() }?.let { login ->
        { chat.fieldCardOpen = OneShotRequest(ownCardRequest(login)) }
    },
    recap = profile.id?.takeIf { it.isNotBlank() }?.let { channelId ->
        ChatRecapSource(
            channelId = channelId,
            channelName = profile.displayName.ifBlank { channel },
            messageTimes = { data.messages.value.map { it.timestampMillis } },
            onOpenSettings = { dismissToSettings(SettingsScrollTarget.ChatterPortraits) },
        )
    },
)

private fun WatchScene.inputBarLook(layout: MinimizablePlayerLayout, context: Context) = ChatInputBarLook(
    fullscreen = layout.fullscreen,
    overVideo = layout.chatOverVideo(),
    plaques = if (threadReplyHidesChrome(chat.replySession != null, chat.composerOpen)) emptyList() else mode.plaques,
    onFollow = follow.action::tap,
    onSubscribe = {
        when (val action = subscribersPlaqueAction(account.signedIn, channel)) {
            TwitchPlaqueAction.LogIn -> account.onLogIn()
            is TwitchPlaqueAction.Open -> openOutsideApp(context, action.url)
        }
    },
    channelPoints = { visible ->
        ChannelPointsChip(channel, profile.channelPointsIconUrl, data.channelEvents.value.prediction, visible)
    },
    onComposerOpenChange = {
        chat.composerOpen = it
        layout.onComposerOpenChange(it)
    },
    onPickerPanelPx = layout.onComposerPanelPx,
)
