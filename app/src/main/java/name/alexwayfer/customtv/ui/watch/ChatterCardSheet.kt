package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import name.alexwayfer.customtv.chat.chatterFactCopyText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.AgeUnit
import name.alexwayfer.customtv.chat.CalendarAge
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.ChatterFollowSource
import name.alexwayfer.customtv.chat.ChatterSubscription
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.calendarAge
import name.alexwayfer.customtv.chat.chatterBadgeLines
import name.alexwayfer.customtv.chat.chatterCardOffersWhisper
import name.alexwayfer.customtv.chat.chatterCardShowsOwnFollow
import name.alexwayfer.customtv.chat.chatterFollowSource
import name.alexwayfer.customtv.chat.chatterSubscription
import name.alexwayfer.customtv.chat.moderatorChatterFollow
import name.alexwayfer.customtv.chat.moderatesChannel
import name.alexwayfer.customtv.chat.shownUnits
import name.alexwayfer.customtv.data.ChatBadgeRepository
import name.alexwayfer.customtv.data.ProfileDetailsRepository
import name.alexwayfer.customtv.data.toChatterProfile
import name.alexwayfer.customtv.data.ChatterCardCache
import name.alexwayfer.customtv.ui.components.lateralTransform
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal data class ChatterCardRequest(
    val login: String,
    val displayName: String,
    val userId: String?,
    val badges: List<ChatBadge>,
    val badgeUrls: Map<String, String>,
    /** The message the card was opened from, for the moderator to delete. */
    val message: ChatMessage? = null,
)

internal class OwnChannelFollow(
    val userId: String?,
    val login: String,
    private val loadFollow: suspend (broadcasterId: String) -> ChatterFollow?,
) {
    suspend fun follow(broadcasterId: String): ChatterFollow? = loadFollow(broadcasterId)
}

internal val LocalOwnChannelFollow = compositionLocalOf<OwnChannelFollow?> { null }

internal val LocalOpenChatterCard = compositionLocalOf<(ChatterCardRequest) -> Unit> { {} }

/** Opens a chatter's full channel profile; without one, the card's header is not a button. */
internal val LocalOpenChatterProfile = compositionLocalOf<((login: String) -> Unit)?> { null }

/** What the open chat's cards already loaded; without one, each card starts empty. */
internal val LocalChatterCardCache = staticCompositionLocalOf<ChatterCardCache?> { null }

@Composable
internal fun ChatterCardHost(
    selfLogin: String,
    recentAuthorMessages: StateFlow<Map<String, List<ChatMessage>>>,
    fieldCardOpen: OneShotRequest<ChatterCardRequest>?,
    /** A chat row; `replyable` is false for a message from the moderation logs, which is not in this chat. */
    renderMessage: @Composable (message: ChatMessage, replyable: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    var request by remember { mutableStateOf<ChatterCardRequest?>(null) }
    val sheetKeyboard = LocalSheetKeyboard.current
    LaunchedEffect(fieldCardOpen) {
        val open = fieldCardOpen?.take() ?: return@LaunchedEffect
        sheetKeyboard.open { request = open }
    }
    CompositionLocalProvider(LocalOpenChatterCard provides { card ->
        sheetKeyboard.open { request = card }
    }) {
        content()
        request?.let { card ->
            ChatterCardSheet(
                login = card.login,
                userId = card.userId,
                displayName = card.displayName,
                badges = card.badges,
                badgeUrls = card.badgeUrls,
                sourceMessage = card.message,
                selfLogin = selfLogin,
                recentAuthorMessages = recentAuthorMessages,
                renderMessage = { renderMessage(it, true) },
                renderLoggedMessage = { renderMessage(it, false) },
                onDismiss = {
                    request = null
                    sheetKeyboard.onDismiss()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatterCardSheet(
    login: String,
    userId: String?,
    displayName: String,
    badges: List<ChatBadge>,
    badgeUrls: Map<String, String>,
    sourceMessage: ChatMessage?,
    selfLogin: String,
    recentAuthorMessages: StateFlow<Map<String, List<ChatMessage>>>,
    renderMessage: @Composable (ChatMessage) -> Unit,
    renderLoggedMessage: @Composable (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val history by recentAuthorMessages.collectAsStateWithLifecycle()
    val userMessages = history[login.lowercase()].orEmpty()
    var page by remember(login) { mutableStateOf(ChatterCardPage.Profile) }
    var modLogsTab by remember(login) { mutableIntStateOf(0) }
    val cache = LocalChatterCardCache.current
    // A card opened again shows the profile kept before at once, and still loads it in the background.
    var profile by remember(login) { mutableStateOf(ProfileDetailsRepository.cached(login)?.toChatterProfile()) }
    var loaded by remember(login) { mutableStateOf(profile != null) }
    LaunchedEffect(login) {
        ProfileDetailsRepository.load(login)?.let { profile = it.toChatterProfile() }
        loaded = true
    }
    val shownName = profile?.displayName?.takeIf { it.isNotBlank() } ?: displayName
    val shownLogin = profile?.login?.takeIf { it.isNotBlank() } ?: login
    var follow by remember(login) { mutableStateOf<ChatterFollow?>(null) }
    var followLoading by remember(login) { mutableStateOf(false) }
    val ownFollow = LocalOwnChannelFollow.current
    val previewChannel = LocalEmotePreviewChannel.current
    val broadcasterId = previewChannel.twitchUserId
    val channelName = previewChannel.displayName
    // A card opened by `/user` for someone who has not written here gets the ID from the loaded profile.
    val knownUserId = userId ?: profile?.userId
    LaunchedEffect(login, knownUserId, ownFollow?.userId, ownFollow?.login, broadcasterId) {
        follow = null
        followLoading = false
        if (broadcasterId.isNullOrBlank()) return@LaunchedEffect
        val ownCard = ownFollow != null && chatterCardShowsOwnFollow(knownUserId, login, ownFollow.userId, ownFollow.login)
        val source = chatterFollowSource(ownCard, knownUserId, moderatesChannel(broadcasterId))
        if (source == ChatterFollowSource.None) return@LaunchedEffect
        follow = cache?.cachedFollow(broadcasterId, login)
        followLoading = follow == null
        val loadedFollow = when (source) {
            ChatterFollowSource.Own -> ownFollow?.follow(broadcasterId)
            else -> knownUserId?.let { moderatorChatterFollow(broadcasterId, it) }
        }
        follow = cache?.rememberFollow(broadcasterId, login, loadedFollow) ?: loadedFollow
        followLoading = false
    }
    val subscription = remember(badges) { chatterSubscription(badges) }
    val headerImage = profile?.bannerImageUrl
    val badgeLines = remember(badges, badgeUrls) {
        val titles = buildMap {
            badgeUrls.forEach { (key, url) ->
                ChatBadgeRepository.titleForImage(url)?.let { put(key, it) }
            }
        }
        chatterBadgeLines(badges, badgeUrls, titles)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val profileScrollState = rememberScrollState()
    val messagesScrollState = rememberScrollState()
    // The page opens on the newest message and stays there as the author writes, unless scrolled up.
    LaunchedEffect(page) {
        if (page != ChatterCardPage.Messages) return@LaunchedEffect
        var following = true
        var previousMax = -1
        snapshotFlow { messagesScrollState.value to messagesScrollState.maxValue }.collect { (value, max) ->
            // The range is unknown until the page is measured.
            if (max == Int.MAX_VALUE) return@collect
            following = followChatterMessagesEnd(following, previousMax, value, max)
            previousMax = max
            if (following && value < max) messagesScrollState.scrollTo(max)
        }
    }
    val dismissSheet = rememberUpdatedState {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
        Unit
    }
    // A drag back up through either page stops at its top; closing the sheet takes a new drag.
    val headerCollapse = rememberChatterHeaderCollapse(profileScrollState)
    // Each page keeps its own edge stretch, which also takes the drag past the top that the sheet must not get.
    val profileOverscroll = rememberOverscrollEffect()
    val messagesOverscroll = rememberOverscrollEffect()
    val stopProfileAtTop = rememberStopAtScrollTop(profileScrollState, profileOverscroll) {
        profileScrollState.value == 0 && headerCollapse.collapsedPx == 0f
    }
    val stopMessagesAtTop = rememberStopAtScrollTop(messagesScrollState, messagesOverscroll)
    val modTools = rememberChatterModTools(knownUserId, shownLogin, badges)
    val openProfile = LocalOpenChatterProfile.current
    val openSettings = LocalOpenSettings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BackHandler {
            if (page != ChatterCardPage.Profile) page = ChatterCardPage.Profile else dismissSheet.value()
        }
        // The other pages keep the chatter's banner folded on top, so their own titles need no name.
        val pin by animateFloatAsState(
            targetValue = if (page == ChatterCardPage.Profile) 0f else 1f,
            label = "chatterCardHeaderPin",
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The sheet stops at the player, so the stream stays in sight.
                .heightIn(max = chatSheetContentMaxHeight()),
        ) {
            ChatterCardHeader(
                imageUrl = headerImage,
                avatarUrl = profile?.avatarUrl,
                name = shownName,
                login = shownLogin,
                collapse = headerCollapse,
                pinProgress = { pin },
                onOpenProfile = openProfile?.let { open ->
                    {
                        onDismiss()
                        open(shownLogin)
                    }
                },
            )
            // The recent messages and the logs pages come in from the end and go back the way they came.
            AnimatedContent(
                targetState = page,
                transitionSpec = { lateralTransform(forward = targetState != ChatterCardPage.Profile) },
                modifier = Modifier.weight(1f, fill = false),
                label = "chatterCardPage",
            ) { shownPage ->
                val messagesPage = shownPage == ChatterCardPage.Messages
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The card grows and shrinks smoothly as its facts load, and the sheet follows it.
                        .then(if (shownPage == ChatterCardPage.Profile) Modifier.animateContentSize() else Modifier),
                ) {
                    if (shownPage == ChatterCardPage.ModLogs) {
                        ChatterCardModLogsPage(
                            tools = modTools,
                            initialTab = modLogsTab,
                            renderMessage = renderLoggedMessage,
                            onBack = { page = ChatterCardPage.Profile },
                        )
                        return@Column
                    }
                    if (shownPage == ChatterCardPage.Labels) {
                        knownUserId?.let { id ->
                            ChatterCardLabelsPage(
                                userId = id,
                                login = shownLogin,
                                displayName = shownName,
                                onBack = { page = ChatterCardPage.Profile },
                            )
                        }
                        return@Column
                    }
                    if (shownPage == ChatterCardPage.Portrait) {
                        if (!broadcasterId.isNullOrBlank() && knownUserId != null) {
                            ChatterCardPortraitPage(
                                channelId = broadcasterId,
                                channelName = channelName,
                                userId = knownUserId,
                                login = shownLogin,
                                displayName = shownName,
                                selfLogin = selfLogin,
                                badgeTitles = badgeLines.map { it.title },
                                badgeUrls = badgeUrls,
                                createdAtMillis = profile?.createdAtMillis,
                                follow = follow,
                                onOpenSettings = openSettings?.let { open ->
                                    {
                                        onDismiss()
                                        open(SettingsScrollTarget.ChatterPortraits)
                                    }
                                },
                                onBack = { page = ChatterCardPage.Profile },
                            )
                        }
                        return@Column
                    }
                    // Each page's header stays in place while the rest scrolls under it.
                    if (messagesPage) {
                        ChatterRecentMessagesHeader(count = userMessages.size, onBack = { page = ChatterCardPage.Profile })
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            // The messages page opens at its end, so a drag down there scrolls back through them.
                            .nestedScroll(if (messagesPage) stopMessagesAtTop else stopProfileAtTop)
                            // The banner shrinks first, before the profile scrolls under it.
                            .then(if (messagesPage) Modifier else Modifier.nestedScroll(headerCollapse))
                            .verticalScroll(
                                state = if (messagesPage) messagesScrollState else profileScrollState,
                                overscrollEffect = if (messagesPage) messagesOverscroll else profileOverscroll,
                            )
                            .padding(bottom = 24.dp),
                    ) {
                        if (messagesPage) {
                            userMessages.forEach { message -> renderMessage(message) }
                            return@Column
                        }
                        val othersCard = chatterCardOffersWhisper(knownUserId, login, ownFollow?.userId, selfLogin)
                        AnimatedVisibility(visible = !loaded, enter = fadeIn(), exit = fadeOut()) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                color = TwitchPurple,
                                trackColor = TwitchDivider,
                                gapSize = 0.dp,
                            )
                        }
                        val createdAt = profile?.createdAtMillis
                        val shownCreatedAt = rememberLastNonNull(createdAt)
                        AnimatedVisibility(visible = createdAt != null, enter = fadeIn(), exit = fadeOut()) {
                            ChatterCardFact(
                                icon = ImageVector.vectorResource(R.drawable.ic_chatter_account_created),
                                title = stringResource(R.string.chatter_card_account_created),
                                supporting = shownCreatedAt?.let { formatChatterCreatedDate(it) },
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        // The spinner fades into the follow fact it stood for.
                        AnimatedContent(
                            targetState = followLoading to follow,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            contentKey = { (loading, shown) -> loading to shown?.javaClass },
                            label = "chatterCardFollow",
                        ) { (loading, shown) ->
                            if (loading) {
                                ChatterCardLoadingFact(
                                    icon = Icons.Filled.Favorite,
                                    description = stringResource(R.string.chatter_card_follow_loading),
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                            when (shown) {
                                is ChatterFollow.Following -> ChatterCardFact(
                                    icon = Icons.Filled.Favorite,
                                    title = stringResource(R.string.chatter_card_following_since),
                                    supporting = stringResource(
                                        R.string.chatter_card_followed_date_with_age,
                                        formatChatterCreatedDate(shown.atMillis),
                                        calendarAgeText(
                                            calendarAge(shown.atMillis, System.currentTimeMillis(), ZoneId.systemDefault()),
                                        ),
                                    ),
                                    copyable = true,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                                ChatterFollow.NotFollowing -> ChatterCardFact(
                                    icon = Icons.Filled.FavoriteBorder,
                                    title = stringResource(R.string.chatter_card_not_following),
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                                null -> Unit
                            }
                        }
                        val shownSubscription = rememberLastNonNull(subscription)
                        AnimatedVisibility(visible = subscription != null, enter = fadeIn(), exit = fadeOut()) {
                            shownSubscription?.let { sub ->
                                ChatterCardFact(
                                    icon = Icons.Filled.Star,
                                    title = subscriptionLabel(sub),
                                )
                            }
                        }
                        if (badgeLines.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.chatter_card_badges),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            badgeLines.forEach { badge ->
                                ChatterCardIconRow(image = badge.imageUrl, title = badge.title)
                            }
                        }
                        ChatterCardLabels(userId = knownUserId, onManage = { page = ChatterCardPage.Labels })
                        val recentMessagesAlpha by animateFloatAsState(
                            if (userMessages.isEmpty()) 0.38f else 1f,
                            label = "chatter recent messages",
                        )
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.chatter_card_last_messages_count, userMessages.size))
                            },
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_chatter_recent_messages), contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = userMessages.isNotEmpty()) { page = ChatterCardPage.Messages }
                                .graphicsLayer { alpha = recentMessagesAlpha },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        ChatterCardPortraitItem(
                            channelId = broadcasterId,
                            userId = knownUserId,
                            onOpen = { page = ChatterCardPage.Portrait },
                        )
                        ChatterCardAbout(about = profile?.about, links = profile?.links.orEmpty())
                        if (othersCard && knownUserId != null) {
                            // The row in the chat follows a deletion, so the card shows that copy of the message.
                            ChatterCardModeration(
                                tools = modTools,
                                displayName = shownName,
                                message = sourceMessage?.let { source ->
                                    userMessages.find { it.id == source.id } ?: source
                                },
                                renderMessage = renderMessage,
                                onOpenLogs = { tab ->
                                    modLogsTab = tab
                                    page = ChatterCardPage.ModLogs
                                },
                            )
                        }
                        if (othersCard) {
                            ChatterCardWhisperItem(
                                login = shownLogin,
                                displayName = shownName,
                                userId = knownUserId,
                                onOpened = onDismiss,
                            )
                        }
                        if (othersCard && knownUserId != null) {
                            ChatterCardReportItem(login = shownLogin, displayName = shownName, userId = knownUserId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatterCardFact(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    copyable: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val copyLabel = stringResource(R.string.chat_copy)
    val copy = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        context.copyChatText(chatterFactCopyText(title, supporting))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (copyable) {
                    Modifier
                        .pointerInput(title, supporting) { detectTapGestures(onLongPress = { copy() }) }
                        .semantics(mergeDescendants = true) {
                            onLongClick(label = copyLabel) {
                                copy()
                                true
                            }
                        }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ChatterCardIconRow(image: Any?, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = image,
            contentDescription = null,
            modifier = Modifier.size(24.dp).ffzModBadgeBackground(image),
            contentScale = ContentScale.Fit,
        )
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

private val OnImageTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.9f),
    offset = Offset(0f, 1f),
    blurRadius = 14f,
)

@Composable
private fun ChatterCardHeader(
    imageUrl: String?,
    avatarUrl: String?,
    name: String,
    login: String,
    collapse: ChatterHeaderCollapse,
    /** 0 on the profile, 1 on the card's other pages, which keep the banner folded. */
    pinProgress: () -> Float,
    onOpenProfile: (() -> Unit)?,
) {
    // The gray fill holds the banner's place, so the card does not jump when the image arrives.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Read while placing, so the banner shrinks with the scroll without composing again.
            .layout { measurable, constraints ->
                val shown = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress())
                val height = chatterHeaderHeightPx(ChatterHeaderExpandedHeight.roundToPx(), shown)
                val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .background(TwitchSurfaceAlt),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
            contentDescription = imageUrl?.let { stringResource(R.string.chatter_card_channel_image) },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset {
                    val maxShiftPx = (ChatterHeaderMargin - ChatterHeaderFoldedMargin).roundToPx()
                    val shown = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress())
                    IntOffset(0, chatterHeaderRowShiftPx(shown, collapse.rangePx, maxShiftPx))
                }
                // No top padding, and never squeezed by the banner: only the fold itself shrinks the avatar.
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .padding(start = ChatterHeaderMargin, end = ChatterHeaderMargin, bottom = ChatterHeaderMargin)
                // The avatar and the name are one button; the pill ripple hugs the round avatar.
                .then(
                    if (onOpenProfile != null) {
                        Modifier
                            .clip(CircleShape)
                            .clickable(
                                onClickLabel = stringResource(R.string.chatter_card_open_profile),
                                role = Role.Button,
                                onClick = onOpenProfile,
                            )
                    } else {
                        Modifier
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatterAvatar(avatarUrl, collapse, pinProgress)
            // The folded banner fits whichever is taller, the folded avatar or the names.
            Column(modifier = Modifier.onSizeChanged { collapse.fitNames(it.height) }) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge.copy(shadow = OnImageTextShadow),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                if (!name.equals(login, ignoreCase = true)) {
                    Text(
                        text = login,
                        style = MaterialTheme.typography.bodyMedium.copy(shadow = OnImageTextShadow),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatterAvatar(url: String?, collapse: ChatterHeaderCollapse, pinProgress: () -> Float) {
    val modifier = Modifier
        // Read while measuring, so the avatar shrinks with the banner without composing again.
        .layout { measurable, _ ->
            val size = chatterAvatarSizePx(
                collapsedPx = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress()),
                rangePx = collapse.rangePx,
                expandedPx = ChatterAvatarSize.roundToPx(),
                foldedPx = ChatterAvatarFoldedSize.roundToPx(),
            )
            val placeable = measurable.measure(Constraints.fixed(size, size))
            layout(size, size) { placeable.place(0, 0) }
        }
        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        .padding(2.dp)
        .clip(CircleShape)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = modifier,
            tint = Color.White,
        )
    }
}

@Composable
private fun subscriptionLabel(subscription: ChatterSubscription): String = when {
    subscription.founder && subscription.months != null ->
        pluralStringResource(
            R.plurals.chatter_card_founder_months,
            subscription.months,
            subscription.months,
        )
    subscription.founder -> stringResource(R.string.chatter_card_founder)
    subscription.months != null ->
        pluralStringResource(
            R.plurals.chatter_card_subscribed_months,
            subscription.months,
            subscription.months,
        )
    else -> stringResource(R.string.chatter_card_subscribed)
}

/** A fact row whose text is still loading: its icon holds the place, and a spinner stands for the text. */
@Composable
private fun ChatterCardLoadingFact(icon: ImageVector, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        CircularProgressIndicator(
            modifier = Modifier
                .size(20.dp)
                .semantics { contentDescription = description },
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun calendarAgeText(age: CalendarAge): String =
    age.shownUnits().map { (unit, count) ->
        val plural = when (unit) {
            AgeUnit.Years -> R.plurals.chatter_card_age_years
            AgeUnit.Months -> R.plurals.chatter_card_age_months
            AgeUnit.Days -> R.plurals.chatter_card_age_days
            AgeUnit.Hours -> R.plurals.chatter_card_age_hours
            AgeUnit.Minutes -> R.plurals.chatter_card_age_minutes
        }
        pluralStringResource(plural, count.toInt(), count.toInt())
    }.joinToString(" ")

internal fun formatChatterCreatedDate(millis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
