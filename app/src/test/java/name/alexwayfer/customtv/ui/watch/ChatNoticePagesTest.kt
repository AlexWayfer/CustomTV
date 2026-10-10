package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatNoticePagesTest {
    @Test
    fun aPinAndARaidAreSeparatePagesWithTheRaidFirst() {
        assertEquals(
            listOf(ChatNoticePage.Raid, ChatNoticePage.Pinned),
            chatNoticePages(hasPinned = true, hasRaid = true, hasCommunityGift = false),
        )
    }

    @Test
    fun aPinAloneIsTheOnlyPage() {
        assertEquals(
            listOf(ChatNoticePage.Pinned),
            chatNoticePages(hasPinned = true, hasRaid = false, hasCommunityGift = false),
        )
    }

    @Test
    fun aRaidAloneIsTheOnlyPage() {
        assertEquals(
            listOf(ChatNoticePage.Raid),
            chatNoticePages(hasPinned = false, hasRaid = true, hasCommunityGift = false),
        )
    }

    @Test
    fun neitherNoticeProducesNoPages() {
        assertEquals(
            emptyList<ChatNoticePage>(),
            chatNoticePages(hasPinned = false, hasRaid = false, hasCommunityGift = false),
        )
    }

    @Test
    fun aCommunityGiftSitsBetweenTheRaidAndPinnedMessage() {
        assertEquals(
            listOf(ChatNoticePage.Raid, ChatNoticePage.CommunityGift, ChatNoticePage.Pinned),
            chatNoticePages(hasPinned = true, hasRaid = true, hasCommunityGift = true),
        )
    }

    @Test
    fun aPollAndAPredictionFollowTheRaidAndPrecedeGiftsAndThePin() {
        assertEquals(
            listOf(
                ChatNoticePage.Raid,
                ChatNoticePage.Poll,
                ChatNoticePage.Prediction,
                ChatNoticePage.CommunityGift,
                ChatNoticePage.Pinned,
            ),
            chatNoticePages(
                hasPinned = true,
                hasRaid = true,
                hasCommunityGift = true,
                hasPoll = true,
                hasPrediction = true,
            ),
        )
    }

    @Test
    fun aPredictionAloneIsTheOnlyPage() {
        assertEquals(
            listOf(ChatNoticePage.Prediction),
            chatNoticePages(hasPinned = false, hasRaid = false, hasCommunityGift = false, hasPrediction = true),
        )
    }

    @Test
    fun itemsAlreadyRunningWhenChatOpensKeepTheDefaultOrder() {
        val arrivals = nextChatNoticeArrivals(
            ChatNoticeArrivals(),
            mapOf(
                ChatNoticePage.Pinned to ChatNoticeItem("pin", live = false),
                ChatNoticePage.Poll to ChatNoticeItem("poll", live = false),
                ChatNoticePage.HypeTrain to ChatNoticeItem("train:2", live = false),
            ),
        )

        assertEquals(
            listOf(ChatNoticePage.HypeTrain, ChatNoticePage.Poll, ChatNoticePage.Pinned),
            orderedChatNoticePages(
                listOf(ChatNoticePage.HypeTrain, ChatNoticePage.Poll, ChatNoticePage.Pinned),
                arrivals,
            ),
        )
    }

    @Test
    fun aLiveGiftDuringARunningTrainComesFirst() {
        val opened = nextChatNoticeArrivals(
            ChatNoticeArrivals(),
            mapOf(ChatNoticePage.HypeTrain to ChatNoticeItem("train:2", live = true)),
        )
        val gifted = nextChatNoticeArrivals(
            opened,
            mapOf(
                ChatNoticePage.HypeTrain to ChatNoticeItem("train:2", live = true),
                ChatNoticePage.CommunityGift to ChatNoticeItem("gift", live = true),
            ),
        )

        assertEquals(
            listOf(ChatNoticePage.CommunityGift, ChatNoticePage.HypeTrain),
            orderedChatNoticePages(listOf(ChatNoticePage.HypeTrain, ChatNoticePage.CommunityGift), gifted),
        )
    }

    @Test
    fun aHypeTrainsNextLevelMovesItFirstAgainButItsProgressDoesNot() {
        val start = nextChatNoticeArrivals(
            ChatNoticeArrivals(),
            mapOf(
                ChatNoticePage.HypeTrain to ChatNoticeItem("train:2", live = true),
                ChatNoticePage.Prediction to ChatNoticeItem("prediction", live = false),
            ),
        )
        val prediction = nextChatNoticeArrivals(
            start,
            mapOf(
                ChatNoticePage.HypeTrain to ChatNoticeItem("train:2", live = true),
                ChatNoticePage.Prediction to ChatNoticeItem("next prediction", live = true),
            ),
        )
        val pages = listOf(ChatNoticePage.HypeTrain, ChatNoticePage.Prediction)
        assertEquals(listOf(ChatNoticePage.Prediction, ChatNoticePage.HypeTrain), orderedChatNoticePages(pages, prediction))

        val sameLevel = nextChatNoticeArrivals(prediction, prediction.byPage.mapValues { ChatNoticeItem(it.value.key, true) })
        assertEquals(prediction, sameLevel)

        val levelUp = nextChatNoticeArrivals(
            prediction,
            mapOf(
                ChatNoticePage.HypeTrain to ChatNoticeItem("train:3", live = true),
                ChatNoticePage.Prediction to ChatNoticeItem("next prediction", live = true),
            ),
        )
        assertEquals(listOf(ChatNoticePage.HypeTrain, ChatNoticePage.Prediction), orderedChatNoticePages(pages, levelUp))
    }

    @Test
    fun anExpandedNoticeKeepsThePagesInPlaceAndANewPageJoinsAtTheEnd() {
        assertEquals(
            listOf(ChatNoticePage.Poll, ChatNoticePage.Pinned, ChatNoticePage.Prediction),
            frozenChatNoticePages(
                shown = listOf(ChatNoticePage.Poll, ChatNoticePage.Raid, ChatNoticePage.Pinned),
                ordered = listOf(ChatNoticePage.Prediction, ChatNoticePage.Poll, ChatNoticePage.Pinned),
            ),
        )
    }

    @Test
    fun theInsetIsTheSharedGapPlusTheDotsOverlappingTheCard() {
        assertEquals(25.dp, chatNoticeDotInset())
        assertEquals(14.dp, chatNoticeDotInset(gapAboveDots = 1.dp))
    }

    @Test
    fun aSinglePageKeepsItsOwnGap() = assertEquals(10.dp, noticeGapAboveInset(10.dp, inset = 0.dp))

    @Test
    fun aGrowingInsetTakesOverTheOwnGapBitByBit() = assertEquals(6.dp, noticeGapAboveInset(10.dp, inset = 4.dp))

    @Test
    fun theFullInsetLeavesNoOwnGapAndNeverANegativeOne() {
        assertEquals(0.dp, noticeGapAboveInset(10.dp, inset = 25.dp))
        assertEquals(0.dp, noticeGapAboveInset(0.dp, inset = 3.dp))
    }
}
