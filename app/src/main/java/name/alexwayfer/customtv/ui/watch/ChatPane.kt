package name.alexwayfer.customtv.ui.watch

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatRaiders
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.chat.SuspiciousChatters
import name.alexwayfer.customtv.chat.emoteSources
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.ChatterCardCache
import name.alexwayfer.customtv.ui.settings.SettingsViewModel
import name.alexwayfer.customtv.ui.settings.beforeRestore
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchHighlightDefault
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.parseTwitchHexColor

/**
 * The chat a stream and a recording share: chat settings, channel colors, chatter labels, links,
 * pinned highlights, and the message list. Only the message source and the input differ.
 */
@Composable
internal fun ColumnScope.ChatPane(
    channel: String,
    channelProfile: ChannelProfile,
    /** Read here, so a new message recomposes the chat and not the screen that holds it. */
    messages: () -> List<ChatMessage>,
    recentAuthorMessages: StateFlow<Map<String, List<ChatMessage>>>,
    badgeUrls: Map<String, String>,
    sevenTvEmotes: Map<String, SevenTvEmote>,
    ffzEmotes: Map<String, SevenTvEmote>,
    bttvEmotes: Map<String, SevenTvEmote>,
    /** Read only by the rows that show a color, so a new chatter does not recompose the screen that holds the chat. */
    nickColors: () -> Map<String, Color>,
    listState: LazyListState,
    highlightPins: TemporaryHighlightPins,
    stickToBottom: Boolean,
    onStickToBottomChange: (Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    /** Opens a chatter's full channel profile from their card. */
    onOpenChatterProfile: (login: String) -> Unit,
    backgroundAlpha: () -> Float,
    selfLogin: String = "",
    selfDisplayName: String = "",
    ownFollow: OwnChannelFollow? = null,
    replyingToMessageId: String? = null,
    onReply: ((ChatMessage) -> Unit)? = null,
    fieldCardOpen: OneShotRequest<ChatterCardRequest>? = null,
    noticesHidden: Boolean = false,
    timelineJumpGeneration: Int = 0,
    /** How tall the chat is over a full screen video; a compact one shows only its latest messages in the corner. */
    overlayHeight: FullscreenOverlayChatHeight = FullscreenOverlayChatHeight.None,
    onOverlayExpand: () -> Unit = {},
    /** How compact the full overlaid chat looks as it rises or sinks; read when laid out and drawn. */
    overlayMorph: () -> Float = { 0f },
    /** Takes the height of the rows the compact chat will show, which the sinking full chat shrinks to. */
    compactHeight: FullscreenCompactChatHeight? = null,
    chatSettingsViewModel: ChatSettingsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    notices: @Composable (ChatAppearance) -> Unit = {},
    listTop: @Composable () -> Unit = {},
    belowList: @Composable (ChatAppearance) -> Unit = {},
) {
    val storedChatSettings by chatSettingsViewModel.settings.collectAsStateWithLifecycle()
    val chatSettings = storedChatSettings.beforeRestore()
    val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val settings = appSettings.beforeRestore()
    val sources = emoteSources(settings)
    val appearance = remember(
        chatSettings.readableColors, chatSettings.textSize, settings.meMessageItalic,
        badgeUrls, sources, sevenTvEmotes, ffzEmotes, bttvEmotes,
    ) {
        chatAppearance(
            chatSettings.readableColors, chatSettings.textSize, settings.meMessageItalic,
            badgeUrls, ThirdPartyEmotes(sources, sevenTvEmotes, ffzEmotes, bttvEmotes),
        )
    }
    val mentions = ChatMentions(settings.highlightMentions, selfLogin, selfDisplayName, settings.keywordPhrases)
    val listSettings = chatListSettings(chatSettings, settings, channelProfile, appearance, mentions)
    val feed = ChatFeed(
        messages = messages,
        recentAuthorMessages = recentAuthorMessages,
        listState = listState,
        highlightPins = highlightPins,
        stickToBottom = stickToBottom,
        onStickToBottomChange = onStickToBottomChange,
        timelineJumpGeneration = timelineJumpGeneration,
        replyingToMessageId = replyingToMessageId,
        onReply = onReply,
        fieldCardOpen = fieldCardOpen,
    )
    val overlay = ChatOverlay(overlayHeight, onOverlayExpand, overlayMorph, compactHeight, backgroundAlpha)
    CompositionLocalProvider(
        LocalOnChatLinkClick provides onOpenLink,
        LocalOpenChatterProfile provides onOpenChatterProfile,
        LocalOwnChannelFollow provides ownFollow,
    ) {
        ProvideChatRowLocals(channel, channelProfile, nickColors) {
            ChatPaneBody(feed, listSettings, overlay, ChatPaneSlots(noticesHidden, notices, listTop))
            belowList(appearance)
        }
    }
}

/** The settings every row of this chat draws with, read from the chat's and the app's settings. */
@Composable
private fun chatListSettings(
    chatSettings: ChatSettings,
    settings: AppSettings,
    channelProfile: ChannelProfile,
    appearance: ChatAppearance,
    mentions: ChatMentions,
): ChatListSettings {
    val highlightColor = remember(channelProfile.highlightColorHex) {
        parseTwitchHexColor(channelProfile.highlightColorHex) ?: TwitchHighlightDefault
    }
    val channelColor = remember(channelProfile.primaryColorHex) {
        parseTwitchHexColor(channelProfile.primaryColorHex) ?: TwitchPurple
    }
    return ChatListSettings(
        appearance = appearance,
        showTimestamps = chatSettings.timestamps,
        smoothScroll = chatSettings.smoothChatScroll,
        highlightColor = highlightColor,
        channelColor = channelColor,
        highlightRewardCost = channelProfile.highlightRewardCost ?: ChannelProfile.DEFAULT_HIGHLIGHT_COST,
        channelPointsIconUrl = channelProfile.channelPointsIconUrl,
        highlightFirstMessages = settings.highlightFirstMessages,
        linkPreviewMode = settings.linkPreviewMode,
        mentions = mentions,
        pinHighlights = settings.temporarilyPinHighlightedMessages,
        highlightPinSeconds = settings.highlightPinSeconds,
    )
}

/** Gives the rows of this chat what they read about the channel, its chatters, and their colors. */
@Composable
private fun ColumnScope.ProvideChatRowLocals(
    channel: String,
    channelProfile: ChannelProfile,
    nickColors: () -> Map<String, Color>,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chatterLabels by ChatterLabelsRepository.labelsByUserId.collectAsStateWithLifecycle()
    // Chatter cards opened again while this chat is open show what they loaded before at once.
    val chatterCards = remember(channel) { ChatterCardCache() }
    // One state object for the pane's lifetime: rows derive the colors they show from it.
    val currentNickColors = rememberUpdatedState(nickColors)
    val nickColorsState = remember { derivedStateOf { currentNickColors.value() } }
    // Filled only while the user moderates the open live channel.
    val suspiciousChatters by SuspiciousChatters.byUserId.collectAsStateWithLifecycle()
    val raiders by ChatRaiders.byMessageId.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalChatNickColors provides nickColorsState,
        LocalChatterLabels provides chatterLabels,
        LocalChatterCardCache provides chatterCards,
        LocalSuspiciousChatters provides suspiciousChatters,
        LocalChatRaiders provides raiders,
        LocalEmotePreviewChannel provides EmotePreviewChannel(
            twitchUserId = channelProfile.id,
            displayName = channelProfile.displayName.ifBlank { channel },
        ),
    ) {
        content()
    }
}

/** How the chat lies over a full screen video. */
private data class ChatOverlay(
    /** How tall the chat is over a full screen video; a compact one shows only its latest messages in the corner. */
    val height: FullscreenOverlayChatHeight,
    val onExpand: () -> Unit,
    /** How compact the full overlaid chat looks as it rises or sinks; read when laid out and drawn. */
    val morph: () -> Float,
    /** Takes the height of the rows the compact chat will show, which the sinking full chat shrinks to. */
    val compactHeight: FullscreenCompactChatHeight?,
    val backgroundAlpha: () -> Float,
)

/** What the screen puts over the list: the notices, hidden while typing, and a line at the list's top. */
private class ChatPaneSlots(
    val noticesHidden: Boolean,
    val notices: @Composable (ChatAppearance) -> Unit,
    val listTop: @Composable () -> Unit,
)

@Composable
private fun ColumnScope.ChatPaneBody(
    feed: ChatFeed,
    settings: ChatListSettings,
    overlay: ChatOverlay,
    slots: ChatPaneSlots,
) {
    // The notices lie over the chat instead of above it, so the chat shows around and under them.
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val compact = overlay.height == FullscreenOverlayChatHeight.Compact
    // Null until the chat first looks at its latest messages.
    var compactShownAt by remember { mutableStateOf<Map<String, Long>?>(null) }
    Box(
        modifier = Modifier
            .then(if (compact) Modifier else Modifier.weight(1f))
            .fillMaxWidth(),
    ) {
        // When the latest messages came, kept while the compact chat is gone, such as while typing, so it does not
        // show them again on its return.
        // Recent chat loaded from elsewhere stays out of the compact chat.
        val compactCandidates = compactChatCandidates(feed.messages())
        val latestIds = compactCandidates.takeLast(COMPACT_CHAT_MESSAGES).map { it.id }
        LaunchedEffect(latestIds) {
            compactShownAt = compactChatShownAt(
                compactShownAt,
                latestIds,
                SystemClock.uptimeMillis(),
                COMPACT_CHAT_MESSAGE_MILLIS,
            )
        }
        if (compact) {
            CompactChatPane(settings, compactCandidates, compactShownAt.orEmpty(), overlay.onExpand)
        } else {
            val rowMorph = rememberOverlayRowMorph(feed, overlay, latestIds, compactShownAt.orEmpty())
            val layout = ChatListLayout(
                topInset = with(LocalDensity.current) { overlayHeightPx.toDp() },
                // Overlaid, the rows keep the compact bubbles' width, so turning compact does not wrap them again.
                sideInset = if (overlay.height == FullscreenOverlayChatHeight.Full) CompactChatSideGap else 0.dp,
                overlayRowMorph = rowMorph,
            )
            ChatList(
                feed = feed,
                settings = settings,
                layout = layout,
                listTop = slots.listTop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // The shared ground gives way to the bubbles as the chat turns compact.
                        val morph = overlay.morph().coerceIn(0f, 1f)
                        drawRect(TwitchBg.copy(alpha = overlay.backgroundAlpha() * (1f - morph)))
                    },
            )
            ChatNoticesOverlay(slots.noticesHidden, onHeightChange = { overlayHeightPx = it }) {
                slots.notices(settings.appearance)
                TemporaryHighlightedMessageHost(feed, settings)
            }
        }
    }
}

/** The latest messages of an overlaid chat in the corner, drawn as the list draws them. */
@Composable
private fun CompactChatPane(
    settings: ChatListSettings,
    messages: List<ChatMessage>,
    shownAt: Map<String, Long>,
    onExpand: () -> Unit,
) {
    val place = ChatRowPlace(remember { LazyListState() }, bottomOverlayPx = 0)
    val rowLook = ChatListRowLook(
        settings,
        place,
        overlayRowMorph = null,
        onReplySwipingChange = {},
        onReplySwipeEnd = {},
    )
    FullscreenCompactChat(messages = messages, shownAt = shownAt, onExpand = onExpand) { message ->
        rowLook.Message(message, selected = false, onSelect = {}, onDismissSelection = {}, onReply = null)
    }
}

/**
 * While the full overlaid chat sinks, the rows the compact chat will show turn into its bubbles, and it shrinks to
 * them. A chat scrolled back to older messages returns to the latest as it starts to turn compact.
 */
@Composable
private fun rememberOverlayRowMorph(
    feed: ChatFeed,
    overlay: ChatOverlay,
    latestIds: List<String>,
    shownAt: Map<String, Long>,
): OverlayChatRowMorph? {
    val currentMorph = rememberUpdatedState(overlay.morph)
    val morphing by remember { derivedStateOf { currentMorph.value() > 0f } }
    val rowMorph = remember(morphing) {
        if (!morphing) return@remember null
        val ids = compactChatVisibleIds(latestIds, shownAt, SystemClock.uptimeMillis(), COMPACT_CHAT_MESSAGE_MILLIS)
        OverlayChatRowMorph(
            progress = { currentMorph.value() },
            compactIds = ids.toList(),
            fadedId = compactChatFadedId(latestIds, ids, COMPACT_CHAT_MESSAGES),
        )
    }
    LaunchedEffect(morphing) {
        if (!morphing || feed.stickToBottom) return@LaunchedEffect
        val lastIndex = feed.messages().lastIndex
        if (lastIndex >= 0) feed.listState.scrollToItem(lastIndex)
        feed.onStickToBottomChange(true)
    }
    val bubbleGapPx = with(LocalDensity.current) { CompactChatBubbleGap.roundToPx() }
    val compactHeight = overlay.compactHeight
    DisposableEffect(rowMorph, compactHeight, bubbleGapPx) {
        val holder = compactHeight ?: return@DisposableEffect onDispose {}
        holder.sinkTargetPx = rowMorph?.let { morph ->
            {
                compactRowsHeightPx(
                    feed.listState.layoutInfo.visibleItemsInfo.map { it.key to it.size },
                    morph.compactIds,
                    bubbleGapPx,
                )
            }
        }
        onDispose { holder.sinkTargetPx = null }
    }
    return rowMorph
}

/** The notices over the list's top. They slide up out of the way while typing and take no room once gone. */
@Composable
private fun ChatNoticesOverlay(hidden: Boolean, onHeightChange: (Int) -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChange(it.height) },
    ) {
        AnimatedVisibility(
            visible = !hidden,
            modifier = Modifier.clipToBounds(),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
        ) {
            Column { content() }
        }
    }
}
