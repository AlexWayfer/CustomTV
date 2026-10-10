package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

internal val ChatNoticeDotGap = 12.dp
internal val ChatNoticeDotBottomPadding = 8.dp
internal val ChatNoticeDotTouch = 16.dp
internal val ChatNoticeDotVisual = 6.dp
internal val ChatNoticeCardOuterPadding = 6.dp

/**
 * A panel over the chat list takes the touches on its surface, so a tap on its text or padding
 * does not open the chatter or emote below. It consumes nothing: its buttons and swipes still work.
 */
internal fun Modifier.blockChatTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
}

internal fun chatNoticeDotInset(
    gapAboveDots: Dp = ChatNoticeDotGap,
    dotBottomPadding: Dp = ChatNoticeDotBottomPadding,
    dotTouch: Dp = ChatNoticeDotTouch,
    dotVisual: Dp = ChatNoticeDotVisual,
    cardOuterPadding: Dp = ChatNoticeCardOuterPadding,
): Dp {
    val dotTopFromCardBottom = dotBottomPadding + (dotTouch - dotVisual) / 2 + dotVisual - cardOuterPadding
    return gapAboveDots + dotTopFromCardBottom
}

/**
 * What a notice keeps of its own bottom gap [ownGap] over the dots inset [inset] under it: the part the
 * inset does not already give. As the inset grows in, the card's bottom edge moves smoothly from its
 * own gap to the inset. Never below zero.
 */
internal fun noticeGapAboveInset(ownGap: Dp, inset: Dp): Dp = (ownGap - inset).coerceAtLeast(0.dp)

/** The notice pages in their default order, which also breaks ties between arrivals. */
internal enum class ChatNoticePage {
    Raid,
    HypeTrain,
    Poll,
    Prediction,
    CommunityGift,
    Pinned,
}

/** Timed notices come first: a raid, a hype train, a poll and a prediction, then gifts and the pin. */
internal fun chatNoticePages(
    hasPinned: Boolean,
    hasRaid: Boolean,
    hasCommunityGift: Boolean,
    hasPoll: Boolean = false,
    hasPrediction: Boolean = false,
    hasHypeTrain: Boolean = false,
): List<ChatNoticePage> {
    return buildList {
        if (hasRaid) add(ChatNoticePage.Raid)
        if (hasHypeTrain) add(ChatNoticePage.HypeTrain)
        if (hasPoll) add(ChatNoticePage.Poll)
        if (hasPrediction) add(ChatNoticePage.Prediction)
        if (hasCommunityGift) add(ChatNoticePage.CommunityGift)
        if (hasPinned) add(ChatNoticePage.Pinned)
    }
}

/** What a page shows now: [key] names the item, and [live] says it arrived while chat was open. */
internal data class ChatNoticeItem(val key: String, val live: Boolean)

/** When a page's item arrived: 0 for one already running when chat opened. */
internal data class ChatNoticeArrival(val key: String, val order: Long)

internal data class ChatNoticeArrivals(
    val byPage: Map<ChatNoticePage, ChatNoticeArrival> = emptyMap(),
    val counter: Long = 0L,
)

/**
 * Numbers each page by when its item arrived. A live item with a new key, such as a new poll or a
 * hype train's next level, takes the next number; the same key keeps its number; an item that was
 * already running when chat opened stays at 0.
 */
internal fun nextChatNoticeArrivals(
    previous: ChatNoticeArrivals,
    items: Map<ChatNoticePage, ChatNoticeItem>,
): ChatNoticeArrivals {
    var counter = previous.counter
    val byPage = items.mapValues { (page, item) ->
        val known = previous.byPage[page]
        when {
            known?.key == item.key -> known
            item.live -> ChatNoticeArrival(item.key, ++counter)
            else -> ChatNoticeArrival(item.key, 0L)
        }
    }
    return ChatNoticeArrivals(byPage, counter)
}

/** The newest arrival first; items already running when chat opened keep the default order. */
internal fun orderedChatNoticePages(
    pages: List<ChatNoticePage>,
    arrivals: ChatNoticeArrivals,
): List<ChatNoticePage> = pages.sortedWith(
    compareByDescending<ChatNoticePage> { arrivals.byPage[it]?.order ?: 0L }.thenBy { it.ordinal },
)

/** While a notice is expanded, the pages keep their places and a new page joins at the end. */
internal fun frozenChatNoticePages(
    shown: List<ChatNoticePage>,
    ordered: List<ChatNoticePage>,
): List<ChatNoticePage> = shown.filter { it in ordered } + ordered.filter { it !in shown }
