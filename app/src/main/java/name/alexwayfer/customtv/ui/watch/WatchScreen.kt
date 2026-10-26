package name.alexwayfer.customtv.ui.watch

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.chatMentionName
import name.alexwayfer.customtv.chat.replySessionFor
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.data.CollaborationResult
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.data.collaborationRowOpensChannel
import name.alexwayfer.customtv.player.TwitchPlaybackTarget
import name.alexwayfer.customtv.player.streamInfoShownUnderPlayer
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.components.preview
import name.alexwayfer.customtv.ui.components.rememberChannelProfile
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.settings.SettingsViewModel
import name.alexwayfer.customtv.ui.settings.beforeRestore
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun WatchScreen(
    channel: String,
    preview: ChannelPreview,
    chatViewModel: ChatViewModel,
    navigation: WatchNavigation,
    account: WatchAccount,
    placement: WatchPlacement = WatchPlacement(),
) {
    val chatSettingsViewModel: ChatSettingsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val storedChatSettings by chatSettingsViewModel.settings.collectAsStateWithLifecycle()
    val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val settings = appSettings.beforeRestore()
    val data = collectWatchChatData(chatViewModel)
    WatchRaidEffects(channel, chatViewModel, data.outgoingRaid.value, account, navigation.onOpenChannel)
    val profile = rememberChannelProfile(channel, pollEvery = 30.seconds, forceRefresh = true, preview = preview)
    val ownBadges = rememberOwnChatBadges(
        account.userId,
        profile.id,
        account.login,
        { data.messages.value },
        data.badgeUrls.value,
    )
    val moderates = ownBadgesModerate(ownBadges?.badges)
    LiveMentionFeedback(chatViewModel, channel, account, settings, moderates)
    LiveChatHiding(chatViewModel, account, settings, moderates)
    val mode =rememberWatchChatMode(channel, profile, chatViewModel, account, ownBadges?.badges)
    val chat = remember(channel) { WatchChatState() }
    val streamHiding = rememberStreamHiding(
        channel,
        overScreen = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
    )
    val scene = WatchScene(
        channel = channel,
        chatViewModel = chatViewModel,
        navigation = navigation,
        account = account,
        placement = placement,
        settings = settings,
        storedChatSettings = storedChatSettings,
        data = data,
        chat = chat,
        profile = profile,
        mode = mode,
        follow = rememberWatchFollow(channel, profile, account, mode),
        chrome = remember(streamHiding) { PlayerChromeVisibility(outOfSight = { streamHiding.hidden }) },
        playerState = rememberMinimizablePlayerState(placement.minimized),
        streamHiding = streamHiding,
        highlightPins = rememberTemporaryHighlightPins(channel),
        ownFollow = rememberOwnChannelFollow(account),
        ownBadges = ownBadges,
        collaboration = rememberCollaboration(profile.id.takeIf { profile.sharedViewerCount != null }, channel),
        sheetKeyboard = rememberSheetKeyboard(),
        chatSettingsViewModel = chatSettingsViewModel,
        settingsViewModel = settingsViewModel,
    )
    WatchChannelEffects(scene)
    ReplySessionEffects(chat, data.messages)
    // One lambda for the screen's life, so the static local never redraws the stream screen.
    val dismissToSettingsLatest by rememberUpdatedState<(SettingsScrollTarget) -> Unit>(scene::dismissToSettings)
    val openSettings = remember { { target: SettingsScrollTarget -> dismissToSettingsLatest(target) } }
    CompositionLocalProvider(
        LocalSheetKeyboard provides scene.sheetKeyboard,
        LocalOpenSettings provides openSettings,
    ) {
        WatchPlayer(scene)
    }
}

/** What the stream screen's parts read as of this composition. */
internal data class WatchScene(
    val channel: String,
    val chatViewModel: ChatViewModel,
    val navigation: WatchNavigation,
    val account: WatchAccount,
    val placement: WatchPlacement,
    val settings: AppSettings,
    val storedChatSettings: ChatSettings?,
    val data: WatchChatData,
    val chat: WatchChatState,
    val profile: ChannelProfile,
    val mode: WatchChatMode,
    val follow: WatchFollow,
    val chrome: PlayerChromeVisibility,
    val playerState: MinimizablePlayerState,
    val streamHiding: StreamHiding,
    val highlightPins: TemporaryHighlightPins,
    val ownFollow: OwnChannelFollow?,
    val ownBadges: OwnChatBadges?,
    val collaboration: CollaborationResult?,
    val sheetKeyboard: SheetKeyboard,
    val chatSettingsViewModel: ChatSettingsViewModel,
    val settingsViewModel: SettingsViewModel,
) {
    fun dismissToChannelProfile(target: ChannelPreview) {
        playerState.minimize(
            before = { navigation.onOpenChannelProfile(target) },
            after = navigation.onMinimizeOverSection,
        )
    }

    /** The category opens in Explore as the player minimizes onto it, as the profile does. */
    fun dismissToCategory(category: TwitchCategory) {
        val open = navigation.onOpenCategory ?: return
        playerState.minimize(before = { open(category) }, after = navigation.onMinimizeOverSection)
    }

    fun dismissToSettings(target: SettingsScrollTarget) {
        playerState.minimize(before = { navigation.onOpenSettings(target) }, after = navigation.onMinimizeOverSection)
    }

    /** Straight to the field, as [mention] goes: a keyboard already up stays up. */
    fun replyTo(message: ChatMessage) {
        chat.replySession = replySessionFor(message, data.messages.value)
        if (chat.replySession != null) chat.replyFocusRequest = OneShotRequest(Unit)
    }

    /** Straight to the field, which keeps its focus and keyboard while the message menu is open. */
    fun mention(login: String, displayName: String) {
        chat.mentionRequest = OneShotRequest(chatMentionName(login, displayName))
    }

    fun ownCardRequest(login: String): ChatterCardRequest =
        ownChatterCardRequest(ownBadges, login, account.displayName.orEmpty(), account.userId)

    /** Opens the card of a login typed after `/user`: the user's own, or a chatter's as far as the chat knows them. */
    fun openChatterCard(login: String) {
        chat.fieldCardOpen = OneShotRequest(
            if (login.equals(account.login, ignoreCase = true)) {
                ownCardRequest(login)
            } else {
                chatterCardRequestForLogin(login, data.messages.value, data.badgeUrls.value)
            },
        )
    }

    fun playerActions(): PlayerActions = PlayerActions(
        onMinimize = navigation.onMinimize,
        onExpand = navigation.onExpand,
        onClose = navigation.onClose,
        onBack = if (navigation.returnsToPreviousChannel) navigation.onReturnToPreviousChannel else null,
        onMinimizeDragDismissesKeyboard = navigation.onMinimizeDragDismissesKeyboard,
        onExpanded = navigation.onFullPlayerExpanded,
        onFullscreenSendChat = {
            if (account.signedIn) chat.replyFocusRequest = OneShotRequest(Unit) else account.onLogIn()
        },
    )
}

@Composable
private fun WatchPlayer(scene: WatchScene) {
    MinimizablePlayer(
        state = scene.playerState,
        chromeVisibility = scene.chrome,
        target = TwitchPlaybackTarget.Channel(scene.channel),
        minimized = scene.placement.minimized,
        actions = scene.playerActions(),
        inPictureInPicture = scene.placement.inPictureInPicture,
        appNavigationBarHeightPx = scene.placement.appNavigationBarHeightPx,
        streamHiding = scene.streamHiding,
        fullscreenInfo = { WatchChannelHeader(scene, expanded = true, fullscreen = true) },
        overlays = { compactSettled -> WatchOverlays(scene, compactSettled) },
    ) { layout ->
        WatchChatColumn(scene, layout)
        WatchInputBar(scene, layout)
    }
}

/**
 * The channel's header under the player, where a drag up or down hides or shows the stream. Under a [fullscreen]
 * video it shows whole with the controls and without the tags, and a drag up hides the stream there too.
 */
@Composable
private fun WatchChannelHeader(scene: WatchScene, expanded: Boolean, fullscreen: Boolean) {
    val profile = scene.profile
    val hideDrag = rememberWatchStreamHideDrag(scene.streamHiding)
    ChannelHeader(
        modifier = Modifier.streamHideDrag(scene.streamHiding, hideDrag),
        channel = scene.channel,
        profile = profile,
        expanded = expanded,
        onViewersClick = if (profile.sharedViewerCount != null && !profile.id.isNullOrBlank()) {
            { scene.sheetKeyboard.open { scene.chat.showCollaboration = true } }
        } else {
            null
        },
        // Toggles what the panel shows, so a tap on the panel shown whole over a full screen player hides the controls.
        onToggle = { scene.chrome.show(!expanded) },
        onOpenProfile = { scene.dismissToChannelProfile(profile.preview()) },
        onOpenCategory = scene.navigation.onOpenCategory?.let { scene::dismissToCategory },
        followStatus = scene.follow.channel.status,
        followAction = scene.follow.action,
        onOpenSettings = { scene.dismissToSettings(SettingsScrollTarget.Notifications) },
        onHeldChange = { held -> scene.chrome.headerHeld = held },
        showTags = !fullscreen,
        // In landscape the panel of a hidden stream sits at the screen's top, with no status bar over it.
        contentPadding = streamInfoCornersPadding(
            active = !fullscreen && scene.streamHiding.overScreen && scene.streamHiding.partlyHidden,
        ),
    )
}

/** The channels that share the stream, the follow dialogs, and the question before a chat link opens. */
@Composable
private fun WatchOverlays(scene: WatchScene, compactSettled: Boolean) {
    val chat = scene.chat
    val activity = LocalActivity.current as MainActivity
    if (chat.showCollaboration && !scene.profile.id.isNullOrBlank() && !compactSettled) {
        CollaborationSheet(
            result = scene.collaboration,
            sharedViewerCount = scene.profile.sharedViewerCount,
            currentLogin = scene.channel,
            onOpenChannel = { target ->
                if (!collaborationRowOpensChannel(target.login, scene.channel)) return@CollaborationSheet
                chat.showCollaboration = false
                scene.sheetKeyboard.onDismiss()
                scene.navigation.onOpenChannel(target)
            },
            onDismiss = {
                chat.showCollaboration = false
                scene.sheetKeyboard.onDismiss()
            },
        )
    }
    FollowDialogs(scene.follow.action, hidden = compactSettled)
    ChatOpenLinkOverlay(
        url = chat.pendingChatLink,
        onDismiss = { chat.pendingChatLink = null },
        onOpen = { url ->
            chat.pendingChatLink = null
            activity.openExternalLink(url)
        },
    )
}

/** The channel's header and the chat under the player, or the chat alone beside a full screen player. */
@Composable
private fun ColumnScope.WatchChatColumn(scene: WatchScene, layout: MinimizablePlayerLayout) {
    // A compact overlaid chat takes only the height of its latest messages.
    val chatFills = layout.overlayChat != FullscreenOverlayChatHeight.Compact
    Column(
        modifier = Modifier
            .then(if (chatFills) Modifier.weight(1f) else Modifier)
            .fillMaxWidth()
            .blockClicks(),
    ) {
        // Beside a full screen player and with the keyboard up it slides up out of the way, and the chat takes the
        // freed room. A chat laid over the video is a block in its corner, where the folding info would show as a
        // strip, so there it goes at once.
        val headerSlides = !layout.chatOverVideo()
        AnimatedVisibility(
            visible = streamInfoShownUnderPlayer(layout.fullscreen, scene.chat.composerOpen),
            enter = if (headerSlides) expandVertically(expandFrom = Alignment.Bottom) else EnterTransition.None,
            exit = if (headerSlides) shrinkVertically(shrinkTowards = Alignment.Bottom) else ExitTransition.None,
        ) {
            WatchChannelHeader(scene, expanded = scene.chrome.headerExpanded, fullscreen = false)
        }
        Column(
            modifier = Modifier
                .then(if (chatFills) Modifier.weight(1f) else Modifier)
                .fillMaxWidth(),
        ) {
            WatchChatPane(scene, layout)
        }
    }
}
