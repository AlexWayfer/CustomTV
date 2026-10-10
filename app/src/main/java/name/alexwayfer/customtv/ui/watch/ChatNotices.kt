package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.chat.ChannelEvents
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.chat.PinnedChat
import name.alexwayfer.customtv.chat.communityGiftPlaqueRemainingMillis
import name.alexwayfer.customtv.chat.latestCommunityGiftMessage
import name.alexwayfer.customtv.chat.visibleCommunityGiftMessage
import name.alexwayfer.customtv.ui.components.rememberLastNonNull

@Composable
internal fun ChatNotices(
    /** Read only to find the latest community gift, so other chat messages do not recompose the notices. */
    messages: () -> List<ChatMessage>,
    pin: PinnedChat?,
    raid: OutgoingRaid?,
    events: ChannelEvents,
    pointsIconUrl: String?,
    channelId: String?,
    channelLogin: String,
    selfLogin: String?,
    appearance: ChatAppearance,
    onHidePin: () -> Unit,
    onOpenTwitchChat: () -> Unit,
    onHideEvent: (String) -> Unit,
) {
    var hiddenCommunityGiftId by remember { mutableStateOf<String?>(null) }
    var communityGiftClockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val currentMessages by rememberUpdatedState(messages)
    val latestGiftMessage by remember { derivedStateOf { latestCommunityGiftMessage(currentMessages()) } }
    LaunchedEffect(latestGiftMessage?.id) {
        val message = latestGiftMessage ?: return@LaunchedEffect
        communityGiftClockMillis = System.currentTimeMillis()
        val remaining = communityGiftPlaqueRemainingMillis(message, communityGiftClockMillis)
        if (remaining > 0L) delay(remaining.milliseconds)
        communityGiftClockMillis = System.currentTimeMillis()
    }
    val communityGiftMessage = visibleCommunityGiftMessage(
        latest = latestGiftMessage,
        hiddenMessageId = hiddenCommunityGiftId,
        nowMillis = communityGiftClockMillis,
    )
    // What each page shows, so the order knows what is new: a hype train's next level counts as new.
    val items = buildMap {
        raid?.let { put(ChatNoticePage.Raid, ChatNoticeItem(it.id, live = true)) }
        events.hypeTrain?.let {
            put(ChatNoticePage.HypeTrain, ChatNoticeItem("${it.id}:${it.level}", it.id in events.liveIds))
        }
        events.poll?.let { put(ChatNoticePage.Poll, ChatNoticeItem(it.id, it.id in events.liveIds)) }
        events.prediction?.let { put(ChatNoticePage.Prediction, ChatNoticeItem(it.id, it.id in events.liveIds)) }
        communityGiftMessage?.let { put(ChatNoticePage.CommunityGift, ChatNoticeItem(it.id, live = true)) }
        pin?.let { put(ChatNoticePage.Pinned, ChatNoticeItem(it.pinId, it.live)) }
    }
    // Which item is expanded, by page and id; a new poll or pin on the same page starts collapsed.
    val expandKeys = buildMap {
        events.hypeTrain?.let { put(ChatNoticePage.HypeTrain, "hype:${it.id}") }
        events.poll?.let { put(ChatNoticePage.Poll, "poll:${it.id}") }
        events.prediction?.let { put(ChatNoticePage.Prediction, "prediction:${it.id}") }
        pin?.let { put(ChatNoticePage.Pinned, "pin:${it.pinId}") }
    }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    val anyExpanded = expandedKey != null && expandedKey in expandKeys.values
    val committed = remember { CommittedNoticeOrder() }
    val arrivals = nextChatNoticeArrivals(committed.arrivals, items)
    val ordered = orderedChatNoticePages(
        pages = chatNoticePages(
            hasPinned = pin != null,
            hasRaid = raid != null,
            hasCommunityGift = communityGiftMessage != null,
            hasPoll = events.poll != null,
            hasPrediction = events.prediction != null,
            hasHypeTrain = events.hypeTrain != null,
        ),
        arrivals = arrivals,
    )
    // An expanded notice stays where it is: pages move and switch only while every notice is collapsed.
    val pages = if (anyExpanded) frozenChatNoticePages(committed.pages, ordered) else ordered
    SideEffect {
        committed.arrivals = arrivals
        committed.pages = pages
    }
    // The block slides down from the top as its first notice arrives and back up after its last one
    // leaves, still showing that last notice on the way out.
    val snapshot = rememberLastNonNull(
        pages.takeIf { it.isNotEmpty() }?.let { ChatNoticeSnapshot(it, pin, raid, events, communityGiftMessage) },
    )
    AnimatedVisibility(
        visible = pages.isNotEmpty(),
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        val shown = snapshot ?: return@AnimatedVisibility
        ChatNoticeCarousel(
            pages = shown.pages,
            firstArrivalOrder = arrivals.byPage[shown.pages.first()]?.order,
            anyExpanded = anyExpanded,
        ) { page, dotInset ->
            ChatNoticePageContent(
                page = page,
                pin = shown.pin,
                raid = shown.raid,
                events = shown.events,
                pointsIconUrl = pointsIconUrl,
                communityGiftMessage = shown.communityGiftMessage,
                channelId = channelId,
                channelLogin = channelLogin,
                selfLogin = selfLogin,
                appearance = appearance,
                expanded = expandKeys[page] != null && expandKeys[page] == expandedKey,
                onExpandedChange = { expand -> expandedKey = if (expand) expandKeys[page] else null },
                onHidePin = onHidePin,
                onOpenTwitchChat = onOpenTwitchChat,
                onHideEvent = onHideEvent,
                onHideCommunityGift = { hiddenCommunityGiftId = communityGiftMessage?.id },
                dotInset = dotInset,
            )
        }
    }
}

/** What the notices showed last, so the block can slide away with it after it is gone. */
private data class ChatNoticeSnapshot(
    val pages: List<ChatNoticePage>,
    val pin: PinnedChat?,
    val raid: OutgoingRaid?,
    val events: ChannelEvents,
    val communityGiftMessage: ChatMessage?,
)

/** The arrivals and page order of the last committed frame, which the next frame builds on. */
private class CommittedNoticeOrder {
    var arrivals = ChatNoticeArrivals()
    var pages = emptyList<ChatNoticePage>()
}

@Composable
private fun ChatNoticePageContent(
    page: ChatNoticePage,
    pin: PinnedChat?,
    raid: OutgoingRaid?,
    events: ChannelEvents,
    pointsIconUrl: String?,
    communityGiftMessage: ChatMessage?,
    channelId: String?,
    channelLogin: String,
    selfLogin: String?,
    appearance: ChatAppearance,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onHidePin: () -> Unit,
    onOpenTwitchChat: () -> Unit,
    onHideEvent: (String) -> Unit,
    onHideCommunityGift: () -> Unit,
    dotInset: Dp,
) {
    when (page) {
        ChatNoticePage.HypeTrain -> events.hypeTrain?.let { train ->
            ChatHypeTrainBanner(
                train = train,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                chatTextSize = appearance.textSize,
                onHide = { onHideEvent(train.id) },
                bottomInset = dotInset,
            )
        }
        ChatNoticePage.Poll -> events.poll?.let { poll ->
            ChatPollBanner(
                poll = poll,
                channelId = channelId,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                chatTextSize = appearance.textSize,
                onOpenTwitchChat = onOpenTwitchChat,
                onHide = { onHideEvent(poll.id) },
                bottomInset = dotInset,
            )
        }
        ChatNoticePage.Prediction -> events.prediction?.let { prediction ->
            ChatPredictionBanner(
                prediction = prediction,
                channelId = channelId,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                channelLogin = channelLogin,
                selfLogin = selfLogin,
                pointsIconUrl = pointsIconUrl,
                chatTextSize = appearance.textSize,
                onOpenTwitchChat = onOpenTwitchChat,
                onHide = { onHideEvent(prediction.id) },
                bottomInset = dotInset,
            )
        }
        ChatNoticePage.Pinned -> {
            if (pin != null) {
                ChatPinnedBanner(
                    pin = pin,
                    channelLogin = channelLogin,
                    appearance = appearance,
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    onHide = onHidePin,
                    bottomInset = dotInset,
                )
            }
        }
        ChatNoticePage.Raid -> {
            if (raid != null) {
                ChatRaidNotice(raid = raid, chatTextSize = appearance.textSize, dotInset = dotInset)
            }
        }
        ChatNoticePage.CommunityGift -> {
            if (communityGiftMessage != null) {
                ChatCommunityGiftBanner(
                    message = communityGiftMessage,
                    chatTextSize = appearance.textSize,
                    onHide = onHideCommunityGift,
                    bottomInset = dotInset,
                )
            }
        }
    }
}
