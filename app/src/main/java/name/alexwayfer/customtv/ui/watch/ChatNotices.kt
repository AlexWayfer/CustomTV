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
import name.alexwayfer.customtv.chat.ChannelShoutout
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.chat.PinnedChat
import name.alexwayfer.customtv.chat.chatCardEvents
import name.alexwayfer.customtv.chat.communityGiftPlaqueRemainingMillis
import name.alexwayfer.customtv.chat.latestCommunityGiftMessage
import name.alexwayfer.customtv.chat.shoutoutPlaqueRemainingMillis
import name.alexwayfer.customtv.chat.visibleCommunityGiftMessage
import name.alexwayfer.customtv.chat.visibleShoutout
import name.alexwayfer.customtv.ui.components.rememberLastNonNull

@Composable
internal fun ChatNotices(
    /** Read only to find the latest community gift, so other chat messages do not recompose the notices. */
    messages: () -> List<ChatMessage>,
    pin: PinnedChat?,
    raid: OutgoingRaid?,
    shoutout: ChannelShoutout?,
    shoutoutSourceName: String?,
    shoutoutFollow: ShoutoutFollow,
    events: ChannelEvents,
    pointsIconUrl: String?,
    channelId: String?,
    channelLogin: String,
    selfLogin: String?,
    appearance: ChatAppearance,
    onHidePin: () -> Unit,
    onOpenTwitchChat: () -> Unit,
    onHideEvent: (String) -> Unit,
    onOpenChatterCard: (login: String) -> Unit,
) {
    var hiddenCommunityGiftId by remember { mutableStateOf<String?>(null) }
    val communityGiftMessage = rememberVisibleCommunityGift(messages, hiddenCommunityGiftId)
    var hiddenShoutoutId by remember { mutableStateOf<String?>(null) }
    val visibleShoutout = rememberVisibleShoutout(shoutout, hiddenShoutoutId)
    val cardEvents = chatCardEvents(events)
    val items = chatNoticeItems(raid, cardEvents, visibleShoutout, communityGiftMessage, pin)
    val expandKeys = chatNoticeExpandKeys(cardEvents, pin)
    var expandedKey by remember { mutableStateOf<String?>(null) }
    val anyExpanded = expandedKey != null && expandedKey in expandKeys.values
    val committed = remember { CommittedNoticeOrder() }
    val arrivals = nextChatNoticeArrivals(committed.arrivals, items)
    val present = chatNoticePagesOf(pin, raid, visibleShoutout, communityGiftMessage, cardEvents)
    val ordered = orderedChatNoticePages(present, arrivals)
    // An expanded notice stays where it is: pages move and switch only while every notice is collapsed.
    val pages = if (anyExpanded) frozenChatNoticePages(committed.pages, ordered) else ordered
    SideEffect {
        committed.arrivals = arrivals
        committed.pages = pages
    }
    // The block slides down from the top as its first notice arrives and back up after its last one
    // leaves, still showing that last notice on the way out.
    val snapshot = rememberLastNonNull(
        pages.takeIf { it.isNotEmpty() }?.let {
            ChatNoticeSnapshot(it, pin, raid, visibleShoutout, cardEvents, communityGiftMessage)
        },
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
                shoutout = shown.shoutout?.let { ShownShoutout(it, shoutoutSourceName, shoutoutFollow) },
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
                onHideShoutout = { hiddenShoutoutId = visibleShoutout?.id },
                onOpenChatterCard = onOpenChatterCard,
                dotInset = dotInset,
            )
        }
    }
}

/**
 * The latest community gift while its plaque lasts, unless the viewer hid it. Only the gifts of [messages] are read,
 * so other chat messages do not recompose the notices.
 */
@Composable
private fun rememberVisibleCommunityGift(messages: () -> List<ChatMessage>, hiddenMessageId: String?): ChatMessage? {
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
    return visibleCommunityGiftMessage(
        latest = latestGiftMessage,
        hiddenMessageId = hiddenMessageId,
        nowMillis = communityGiftClockMillis,
    )
}

/** The [latest] shoutout while its plaque lasts, unless the viewer hid it. */
@Composable
private fun rememberVisibleShoutout(latest: ChannelShoutout?, hiddenId: String?): ChannelShoutout? {
    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(latest?.id) {
        val shoutout = latest ?: return@LaunchedEffect
        clockMillis = System.currentTimeMillis()
        val remaining = shoutoutPlaqueRemainingMillis(shoutout, clockMillis)
        if (remaining > 0L) delay(remaining.milliseconds)
        clockMillis = System.currentTimeMillis()
    }
    return visibleShoutout(latest, hiddenId, clockMillis)
}

/** The pages there is something for, before they are put in order. */
private fun chatNoticePagesOf(
    pin: PinnedChat?,
    raid: OutgoingRaid?,
    shoutout: ChannelShoutout?,
    communityGiftMessage: ChatMessage?,
    events: ChannelEvents,
): List<ChatNoticePage> = chatNoticePages(
    buildSet {
        if (pin != null) add(ChatNoticePage.Pinned)
        if (raid != null) add(ChatNoticePage.Raid)
        if (shoutout != null) add(ChatNoticePage.Shoutout)
        if (communityGiftMessage != null) add(ChatNoticePage.CommunityGift)
        if (events.poll != null) add(ChatNoticePage.Poll)
        if (events.prediction != null) add(ChatNoticePage.Prediction)
        if (events.hypeTrain != null) add(ChatNoticePage.HypeTrain)
    },
)

/** What each page shows, so the order knows what is new: a hype train's next level counts as new. */
private fun chatNoticeItems(
    raid: OutgoingRaid?,
    events: ChannelEvents,
    shoutout: ChannelShoutout?,
    communityGiftMessage: ChatMessage?,
    pin: PinnedChat?,
): Map<ChatNoticePage, ChatNoticeItem> = buildMap {
    raid?.let { put(ChatNoticePage.Raid, ChatNoticeItem(it.id, live = true)) }
    shoutout?.let { put(ChatNoticePage.Shoutout, ChatNoticeItem(it.id, live = true)) }
    events.hypeTrain?.let {
        put(ChatNoticePage.HypeTrain, ChatNoticeItem("${it.id}:${it.level}", it.id in events.liveIds))
    }
    events.poll?.let { put(ChatNoticePage.Poll, ChatNoticeItem(it.id, it.id in events.liveIds)) }
    events.prediction?.let { put(ChatNoticePage.Prediction, ChatNoticeItem(it.id, it.id in events.liveIds)) }
    communityGiftMessage?.let { put(ChatNoticePage.CommunityGift, ChatNoticeItem(it.id, live = true)) }
    pin?.let { put(ChatNoticePage.Pinned, ChatNoticeItem(it.pinId, it.live)) }
}

/** Which item is expanded, by page and id; a new poll or pin on the same page starts collapsed. */
private fun chatNoticeExpandKeys(events: ChannelEvents, pin: PinnedChat?): Map<ChatNoticePage, String> = buildMap {
    events.hypeTrain?.let { put(ChatNoticePage.HypeTrain, "hype:${it.id}") }
    events.poll?.let { put(ChatNoticePage.Poll, "poll:${it.id}") }
    events.prediction?.let { put(ChatNoticePage.Prediction, "prediction:${it.id}") }
    pin?.let { put(ChatNoticePage.Pinned, "pin:${it.pinId}") }
}

/** What the notices showed last, so the block can slide away with it after it is gone. */
private data class ChatNoticeSnapshot(
    val pages: List<ChatNoticePage>,
    val pin: PinnedChat?,
    val raid: OutgoingRaid?,
    val shoutout: ChannelShoutout?,
    val events: ChannelEvents,
    val communityGiftMessage: ChatMessage?,
)

/** A shoutout page's plaque with who gave it and the heart of its channel. */
private class ShownShoutout(val shoutout: ChannelShoutout, val sourceName: String?, val follow: ShoutoutFollow)

/** The shoutout's plaque, whose tap opens the card of the channel it names. */
@Composable
private fun ShoutoutPage(
    shown: ShownShoutout,
    appearance: ChatAppearance,
    onOpenChatterCard: (login: String) -> Unit,
    onHide: () -> Unit,
    dotInset: Dp,
) {
    ChatShoutoutBanner(
        shoutout = shown.shoutout,
        sourceName = shown.sourceName,
        follow = shown.follow,
        chatTextSize = appearance.textSize,
        onOpenCard = { onOpenChatterCard(shown.shoutout.targetLogin) },
        onHide = onHide,
        bottomInset = dotInset,
    )
}

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
    shoutout: ShownShoutout?,
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
    onHideShoutout: () -> Unit,
    onOpenChatterCard: (login: String) -> Unit,
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
        ChatNoticePage.Pinned -> pin?.let {
            ChatPinnedBanner(
                pin = it,
                channelLogin = channelLogin,
                appearance = appearance,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                onHide = onHidePin,
                bottomInset = dotInset,
            )
        }
        ChatNoticePage.Raid -> raid?.let { ChatRaidNotice(it, chatTextSize = appearance.textSize, dotInset = dotInset) }
        ChatNoticePage.Shoutout -> shoutout?.let {
            ShoutoutPage(it, appearance, onOpenChatterCard, onHideShoutout, dotInset)
        }
        ChatNoticePage.CommunityGift -> communityGiftMessage?.let {
            ChatCommunityGiftBanner(it, appearance.textSize, onHide = onHideCommunityGift, bottomInset = dotInset)
        }
    }
}
