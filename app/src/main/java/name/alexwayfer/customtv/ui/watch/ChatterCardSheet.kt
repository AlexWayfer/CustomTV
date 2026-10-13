package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.AgeUnit
import name.alexwayfer.customtv.chat.CalendarAge
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.ChatterHistory
import name.alexwayfer.customtv.chat.ChatterProfile
import name.alexwayfer.customtv.chat.ChatterSubscription
import name.alexwayfer.customtv.chat.chatterCardOffersWhisper
import name.alexwayfer.customtv.chat.chatterFactCopyText
import name.alexwayfer.customtv.chat.chatterSubscription
import name.alexwayfer.customtv.chat.shownUnits
import name.alexwayfer.customtv.data.ChatterCardCache
import name.alexwayfer.customtv.ui.components.lateralTransform
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
    chatterHistory: StateFlow<ChatterHistory>,
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
                card = card,
                selfLogin = selfLogin,
                chatterHistory = chatterHistory,
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
    card: ChatterCardRequest,
    selfLogin: String,
    chatterHistory: StateFlow<ChatterHistory>,
    renderMessage: @Composable (ChatMessage) -> Unit,
    renderLoggedMessage: @Composable (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val history by chatterHistory.collectAsStateWithLifecycle()
    val key = card.login.lowercase()
    val userMessages = history.messages[key].orEmpty()
    val userMentions = history.mentions[key].orEmpty()
    val nav = remember(card.login) { ChatterCardNavigation() }
    val load = rememberChatterCardLoad(card.login, card.userId)
    val chatter = chatterCardChatter(card, selfLogin, load.profile, userMessages, userMentions)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scroll = rememberChatterCardScroll()
    FollowChatterMessagesEnd(nav.page, scroll)
    val dismissSheet = rememberHideThenDismiss(sheetState, onDismiss)
    val openProfile = LocalOpenChatterProfile.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BackHandler {
            if (nav.page != ChatterCardPage.Profile) nav.back() else dismissSheet.value()
        }
        // The other pages keep the chatter's banner folded on top, so their own titles need no name.
        val pinTarget = if (nav.page == ChatterCardPage.Profile) 0f else 1f
        val pin by animateFloatAsState(pinTarget, label = "chatterCardHeaderPin")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The sheet stops at the player, so the stream stays in sight.
                .heightIn(max = chatSheetContentMaxHeight()),
        ) {
            ChatterCardHeader(
                profile = load.profile,
                chatter = chatter,
                collapse = scroll.headerCollapse,
                pinProgress = { pin },
                onOpenProfile = openProfile?.let { open ->
                    {
                        onDismiss()
                        open(chatter.login)
                    }
                },
            )
            // The recent messages and the logs pages come in from the end and go back the way they came.
            AnimatedContent(
                targetState = nav.page,
                transitionSpec = { lateralTransform(forward = targetState != ChatterCardPage.Profile) },
                modifier = Modifier.weight(1f, fill = false),
                label = "chatterCardPage",
            ) { shownPage ->
                ChatterCardPageContent(
                    page = shownPage,
                    chatter = chatter,
                    load = load,
                    scroll = scroll,
                    nav = nav,
                    renderMessage = renderMessage,
                    renderLoggedMessage = renderLoggedMessage,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/** Slides the sheet away, then lets it go once it is hidden. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberHideThenDismiss(sheetState: SheetState, onDismiss: () -> Unit): State<() -> Unit> {
    val scope = rememberCoroutineScope()
    return rememberUpdatedState {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }
}

/** The chatter of the [card] as far as their loaded [profile] tells, with their [messages] and [mentions] here. */
@Composable
private fun chatterCardChatter(
    card: ChatterCardRequest,
    selfLogin: String,
    profile: ChatterProfile?,
    messages: List<ChatMessage>,
    mentions: List<ChatMessage>,
): ChatterCardChatter {
    val shownLogin = profile?.login?.takeIf { it.isNotBlank() } ?: card.login
    // A card opened by `/user` for someone who has not written here gets the ID from the loaded profile.
    val knownUserId = card.userId ?: profile?.userId
    return ChatterCardChatter(
        login = shownLogin,
        name = profile?.displayName?.takeIf { it.isNotBlank() } ?: card.displayName,
        userId = knownUserId,
        selfLogin = selfLogin,
        badgeUrls = card.badgeUrls,
        badgeLines = rememberChatterBadgeLines(card.badges, card.badgeUrls),
        subscription = remember(card.badges) { chatterSubscription(card.badges) },
        messages = messages,
        mentions = mentions,
        // The row in the chat follows a deletion, so the card shows that copy of the message.
        sourceMessage = card.message?.let { source -> messages.find { it.id == source.id } ?: source },
        othersCard = chatterCardOffersWhisper(
            knownUserId,
            card.login,
            LocalOwnChannelFollow.current?.userId,
            selfLogin,
        ),
        modTools = rememberChatterModTools(knownUserId, shownLogin, card.badges),
    )
}

/** One page of the card; the profile grows and shrinks smoothly as its facts load, and the sheet follows it. */
@Composable
private fun ChatterCardPageContent(
    page: ChatterCardPage,
    chatter: ChatterCardChatter,
    load: ChatterCardLoad,
    scroll: ChatterCardScroll,
    nav: ChatterCardNavigation,
    renderMessage: @Composable (ChatMessage) -> Unit,
    renderLoggedMessage: @Composable (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (page == ChatterCardPage.Profile) Modifier.animateContentSize() else Modifier),
    ) {
        when (page) {
            ChatterCardPage.ModLogs -> ChatterCardModLogsPage(
                tools = chatter.modTools,
                initialTab = nav.modLogsTab,
                renderMessage = renderLoggedMessage,
                onBack = nav::back,
            )
            ChatterCardPage.Labels -> chatter.userId?.let { id ->
                ChatterCardLabelsPage(
                    userId = id,
                    login = chatter.login,
                    displayName = chatter.name,
                    onBack = nav::back,
                )
            }
            ChatterCardPage.Portrait -> ChatterPortraitPageFor(chatter, load, onDismiss, onBack = nav::back)
            // Each page's header stays in place while the rest scrolls under it.
            ChatterCardPage.Messages -> ChatterMessagesPage(
                title = stringResource(R.string.chatter_card_last_messages_count, chatter.messages.size),
                messages = chatter.messages,
                scroll = scroll.messages,
                renderMessage = renderMessage,
                onBack = nav::back,
            )
            ChatterCardPage.Mentions -> ChatterMessagesPage(
                title = chatterMentionsTitle(chatter.mentions.size),
                messages = chatter.mentions,
                scroll = scroll.mentions,
                renderMessage = renderMessage,
                onBack = nav::back,
            )
            ChatterCardPage.Profile -> ChatterProfilePage(chatter, load, scroll, nav, renderMessage, onDismiss)
        }
    }
}

@Composable
internal fun ChatterCardFact(
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

@Composable
internal fun subscriptionLabel(subscription: ChatterSubscription): String = when {
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
internal fun ChatterCardLoadingFact(icon: ImageVector, description: String, modifier: Modifier = Modifier) {
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
internal fun calendarAgeText(age: CalendarAge): String =
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
