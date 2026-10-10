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
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.ChatterCardCache
import name.alexwayfer.customtv.ui.settings.SettingsViewModel
import name.alexwayfer.customtv.ui.settings.beforeRestore
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchHighlightDefault
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.parseTwitchHexColor
import java.util.concurrent.atomic.AtomicBoolean

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
    val chatterLabels by ChatterLabelsRepository.labelsByUserId.collectAsStateWithLifecycle()
    val keywordPhrases = settings.keywordPhrases
    val sources = emoteSources(settings)
    val appearance = remember(
        chatSettings.readableColors,
        chatSettings.textSize,
        settings.meMessageItalic,
        badgeUrls,
        sources,
        sevenTvEmotes,
        ffzEmotes,
        bttvEmotes,
    ) {
        chatAppearance(
            readableColors = chatSettings.readableColors,
            textSize = chatSettings.textSize,
            meMessageItalic = settings.meMessageItalic,
            badgeUrls = badgeUrls,
            sources = sources,
            sevenTvEmotes = sevenTvEmotes,
            ffzEmotes = ffzEmotes,
            bttvEmotes = bttvEmotes,
        )
    }
    val highlightColor = remember(channelProfile.highlightColorHex) {
        parseTwitchHexColor(channelProfile.highlightColorHex) ?: TwitchHighlightDefault
    }
    val channelColor = remember(channelProfile.primaryColorHex) {
        parseTwitchHexColor(channelProfile.primaryColorHex) ?: TwitchPurple
    }
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
        LocalOnChatLinkClick provides onOpenLink,
        LocalOpenChatterProfile provides onOpenChatterProfile,
        LocalChatterLabels provides chatterLabels,
        LocalChatterCardCache provides chatterCards,
        LocalOwnChannelFollow provides ownFollow,
        LocalSuspiciousChatters provides suspiciousChatters,
        LocalChatRaiders provides raiders,
        LocalEmotePreviewChannel provides EmotePreviewChannel(
            twitchUserId = channelProfile.id,
            displayName = channelProfile.displayName.ifBlank { channel },
        ),
    ) {
        // The notices lie over the chat instead of above it, so the chat shows around and under them.
        var overlayHeightPx by remember { mutableIntStateOf(0) }
        val compact = overlayHeight == FullscreenOverlayChatHeight.Compact
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
            val compactCandidates = compactChatCandidates(messages())
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
                val rowListState = remember { LazyListState() }
                val rowAutoScrolling = remember { AtomicBoolean(false) }
                FullscreenCompactChat(
                    messages = compactCandidates,
                    shownAt = compactShownAt.orEmpty(),
                    onExpand = onOverlayExpand,
                ) { message ->
                    val mentioned = remember(
                        message,
                        settings.highlightMentions,
                        selfLogin,
                        selfDisplayName,
                        keywordPhrases,
                    ) {
                        chatRowMentioned(message, settings.highlightMentions, selfLogin, selfDisplayName, keywordPhrases)
                    }
                    ChatRow(
                        message = message,
                        appearance = appearance,
                        showTimestamps = chatSettings.timestamps,
                        highlightColor = highlightColor,
                        channelColor = channelColor,
                        highlightRewardCost = channelProfile.highlightRewardCost ?: ChannelProfile.DEFAULT_HIGHLIGHT_COST,
                        channelPointsIconUrl = channelProfile.channelPointsIconUrl,
                        highlightFirstMessages = settings.highlightFirstMessages,
                        linkPreviewMode = settings.linkPreviewMode,
                        mentioned = mentioned,
                        listState = rowListState,
                        autoScrolling = rowAutoScrolling,
                        bottomOverlayPx = 0,
                        selected = false,
                        onSelect = {},
                        onDismissSelection = {},
                        onReply = null,
                        onReplySwipingChange = {},
                    )
                }
            } else {
                // While it sinks, the rows the compact chat will show turn into its bubbles, and it shrinks to them.
                val currentMorph = rememberUpdatedState(overlayMorph)
                val morphing by remember { derivedStateOf { currentMorph.value() > 0f } }
                val rowMorph = remember(morphing) {
                    if (!morphing) return@remember null
                    val ids = compactChatVisibleIds(
                        latestIds,
                        compactShownAt.orEmpty(),
                        SystemClock.uptimeMillis(),
                        COMPACT_CHAT_MESSAGE_MILLIS,
                    )
                    OverlayChatRowMorph(
                        progress = { currentMorph.value() },
                        compactIds = ids.toList(),
                        fadedId = compactChatFadedId(latestIds, ids, COMPACT_CHAT_MESSAGES),
                    )
                }
                // A chat scrolled back to older messages returns to the latest as it starts to turn compact.
                LaunchedEffect(morphing) {
                    if (!morphing || stickToBottom) return@LaunchedEffect
                    val lastIndex = messages().lastIndex
                    if (lastIndex >= 0) listState.scrollToItem(lastIndex)
                    onStickToBottomChange(true)
                }
                val bubbleGapPx = with(LocalDensity.current) { CompactChatBubbleGap.roundToPx() }
                DisposableEffect(rowMorph, compactHeight, bubbleGapPx) {
                    val holder = compactHeight ?: return@DisposableEffect onDispose {}
                    holder.sinkTargetPx = rowMorph?.let { morph ->
                        {
                            compactRowsHeightPx(
                                listState.layoutInfo.visibleItemsInfo.map { it.key to it.size },
                                morph.compactIds,
                                bubbleGapPx,
                            )
                        }
                    }
                    onDispose { holder.sinkTargetPx = null }
                }
                ChatList(
                    rows = messages(),
                    recentAuthorMessages = recentAuthorMessages,
                    appearance = appearance,
                    showTimestamps = chatSettings.timestamps,
                    smoothChatScroll = chatSettings.smoothChatScroll,
                    highlightColor = highlightColor,
                    channelColor = channelColor,
                    highlightRewardCost = channelProfile.highlightRewardCost ?: ChannelProfile.DEFAULT_HIGHLIGHT_COST,
                    channelPointsIconUrl = channelProfile.channelPointsIconUrl,
                    highlightFirstMessages = settings.highlightFirstMessages,
                    highlightMentions = settings.highlightMentions,
                    selfLogin = selfLogin,
                    selfDisplayName = selfDisplayName,
                    listState = listState,
                    stickToBottom = stickToBottom,
                    onStickToBottomChange = onStickToBottomChange,
                    keywordPhrases = keywordPhrases,
                    linkPreviewMode = settings.linkPreviewMode,
                    onReply = onReply,
                    fieldCardOpen = fieldCardOpen,
                    topInset = with(LocalDensity.current) { overlayHeightPx.toDp() },
                    listTop = listTop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // The shared ground gives way to the bubbles as the chat turns compact.
                            val morph = currentMorph.value().coerceIn(0f, 1f)
                            drawRect(TwitchBg.copy(alpha = backgroundAlpha() * (1f - morph)))
                        },
                    timelineJumpGeneration = timelineJumpGeneration,
                    overlayRowMorph = rowMorph,
                    // Overlaid, the rows keep the compact bubbles' width, so turning compact does not wrap them again.
                    sideInset = if (overlayHeight == FullscreenOverlayChatHeight.Full) CompactChatSideGap else 0.dp,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { overlayHeightPx = it.height },
                ) {
                    // Slides up out of the way while typing; the column shrinks to nothing once it is gone,
                    // so the chat takes the freed room.
                    AnimatedVisibility(
                        visible = !noticesHidden,
                        modifier = Modifier.clipToBounds(),
                        enter = slideInVertically { -it },
                        exit = slideOutVertically { -it },
                    ) {
                        Column {
                            notices(appearance)
                            TemporaryHighlightedMessageHost(
                                messages = messages(),
                                pins = highlightPins,
                                listState = listState,
                                enabled = settings.temporarilyPinHighlightedMessages,
                                pinSeconds = settings.highlightPinSeconds,
                                stickToBottom = stickToBottom,
                                timelineJumpGeneration = timelineJumpGeneration,
                                highlightMentions = settings.highlightMentions,
                                selfLogin = selfLogin,
                                selfDisplayName = selfDisplayName,
                                keywordPhrases = keywordPhrases,
                                appearance = appearance,
                                replyingToMessageId = replyingToMessageId,
                                onReply = onReply,
                            )
                        }
                    }
                }
            }
        }
        belowList(appearance)
    }
}
