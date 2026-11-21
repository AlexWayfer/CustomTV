package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatNoticePagesTest {
    @Test
    fun theMenuDotShowsUnreadReceiptsAndRewardsThatStillNeedClaiming() {
        val claim = ChatDropsNotice("claim") { _, _, _, _ -> }
        val received = ChatDropsNotices(hasUnreadReceipt = true)
        val both = received.copy(claim = claim)
        assertFalse(ChatDropsNotices().showsMenuDot)
        assertTrue(received.showsMenuDot)
        assertTrue(both.copy(hasUnreadReceipt = false).showsMenuDot)
    }

    @Test
    fun dropsJoinTheCarouselBehindAnActivePoll() {
        val notices = mapOf(ChatNoticePage.Poll to ChatNoticeItem("live poll", live = true))
        val items = notices.withDropsNotices(campaignId = "campaign", claimId = null)
        val arrivals = nextChatNoticeArrivals(ChatNoticeArrivals(), items)

        assertEquals(
            listOf(ChatNoticePage.Poll, ChatNoticePage.Drops),
            orderedChatNoticePages(chatNoticePages(items.keys), arrivals),
        )
    }

    @Test
    fun aReadyDropTakesTheFirstPageWhileTheCampaignRemainsAvailable() {
        val poll = mapOf(ChatNoticePage.Poll to ChatNoticeItem("live poll", live = true))
        val before = nextChatNoticeArrivals(ChatNoticeArrivals(), poll)
        val items = poll.withDropsNotices(campaignId = "campaign", claimId = "reward")
        val arrivals = nextChatNoticeArrivals(before, items)

        assertEquals(
            listOf(ChatNoticePage.DropsClaim, ChatNoticePage.Poll, ChatNoticePage.Drops),
            orderedChatNoticePages(chatNoticePages(items.keys), arrivals),
        )
        assertEquals(arrivals, nextChatNoticeArrivals(arrivals, items))
    }

    @Test
    fun aReceivedDropIsATemporaryCarouselPageAlongsideClaimableDrops() {
        val base = mapOf(ChatNoticePage.Pinned to ChatNoticeItem("pin", live = true))
            .withDropsNotices(campaignId = "campaign", claimId = "claim")
        val before = nextChatNoticeArrivals(ChatNoticeArrivals(), base)
        val withReceipt = base.withDropsNotices(campaignId = "campaign", claimId = "claim", receivedId = "received")
        val after = nextChatNoticeArrivals(before, withReceipt)

        assertEquals(
            listOf(
                ChatNoticePage.DropsReceived,
                ChatNoticePage.DropsClaim,
                ChatNoticePage.Drops,
                ChatNoticePage.Pinned,
            ),
            orderedChatNoticePages(chatNoticePages(withReceipt.keys), after),
        )
        assertEquals(
            listOf(ChatNoticePage.DropsClaim, ChatNoticePage.Drops, ChatNoticePage.Pinned),
            orderedChatNoticePages(chatNoticePages(base.keys), nextChatNoticeArrivals(after, base)),
        )
    }

    @Test
    fun aPinAndARaidAreSeparatePagesWithTheRaidFirst() {
        assertEquals(
            listOf(ChatNoticePage.Raid, ChatNoticePage.Pinned),
            chatNoticePages(setOf(ChatNoticePage.Pinned, ChatNoticePage.Raid)),
        )
    }

    @Test
    fun dropsPagesPrecedeThePinInTheDefaultOrder() {
        val present = setOf(ChatNoticePage.Pinned, ChatNoticePage.Drops, ChatNoticePage.DropsClaim)
        assertEquals(
            listOf(ChatNoticePage.Drops, ChatNoticePage.DropsClaim, ChatNoticePage.Pinned),
            chatNoticePages(present),
        )
    }

    @Test
    fun aNewPinStaysBehindBothDropsPages() {
        val poll = ChatNoticePage.Poll to ChatNoticeItem("poll", live = true)
        val before = nextChatNoticeArrivals(
            ChatNoticeArrivals(),
            mapOf(poll).withDropsNotices(campaignId = "campaign", claimId = "reward"),
        )
        val items = mapOf(poll, ChatNoticePage.Pinned to ChatNoticeItem("pin", live = true))
            .withDropsNotices(campaignId = "campaign", claimId = "reward")
        val arrivals = nextChatNoticeArrivals(before, items)

        assertEquals(
            listOf(ChatNoticePage.DropsClaim, ChatNoticePage.Poll, ChatNoticePage.Drops, ChatNoticePage.Pinned),
            orderedChatNoticePages(chatNoticePages(items.keys), arrivals),
        )
        val withoutDrops = chatNoticePages(setOf(ChatNoticePage.Poll, ChatNoticePage.Pinned))
        assertEquals(listOf(ChatNoticePage.Pinned, ChatNoticePage.Poll), orderedChatNoticePages(withoutDrops, arrivals))
    }

    @Test
    fun aPinAloneIsTheOnlyPage() {
        assertEquals(
            listOf(ChatNoticePage.Pinned),
            chatNoticePages(setOf(ChatNoticePage.Pinned)),
        )
    }

    @Test
    fun aRaidAloneIsTheOnlyPage() {
        assertEquals(
            listOf(ChatNoticePage.Raid),
            chatNoticePages(setOf(ChatNoticePage.Raid)),
        )
    }

    @Test
    fun neitherNoticeProducesNoPages() {
        assertEquals(
            emptyList<ChatNoticePage>(),
            chatNoticePages(emptySet()),
        )
    }

    @Test
    fun aCommunityGiftSitsBetweenTheRaidAndPinnedMessage() {
        assertEquals(
            listOf(ChatNoticePage.Raid, ChatNoticePage.CommunityGift, ChatNoticePage.Pinned),
            chatNoticePages(setOf(ChatNoticePage.Pinned, ChatNoticePage.CommunityGift, ChatNoticePage.Raid)),
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
                setOf(
                    ChatNoticePage.Pinned,
                    ChatNoticePage.Raid,
                    ChatNoticePage.CommunityGift,
                    ChatNoticePage.Poll,
                    ChatNoticePage.Prediction,
                ),
            ),
        )
    }

    @Test
    fun aShoutoutFollowsThePredictionAndPrecedesGifts() {
        assertEquals(
            listOf(ChatNoticePage.Prediction, ChatNoticePage.Shoutout, ChatNoticePage.CommunityGift),
            chatNoticePages(setOf(ChatNoticePage.CommunityGift, ChatNoticePage.Shoutout, ChatNoticePage.Prediction)),
        )
    }

    @Test
    fun aPredictionAloneIsTheOnlyPage() {
        assertEquals(
            listOf(ChatNoticePage.Prediction),
            chatNoticePages(setOf(ChatNoticePage.Prediction)),
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
        assertEquals(
            listOf(ChatNoticePage.Prediction, ChatNoticePage.HypeTrain),
            orderedChatNoticePages(pages, prediction),
        )

        val sameItems = prediction.byPage.mapValues { ChatNoticeItem(it.value.key, true) }
        val sameLevel = nextChatNoticeArrivals(prediction, sameItems)
        assertEquals(prediction, sameLevel)

        val levelUp = nextChatNoticeArrivals(
            prediction,
            mapOf(
                ChatNoticePage.HypeTrain to ChatNoticeItem("train:3", live = true),
                ChatNoticePage.Prediction to ChatNoticeItem("next prediction", live = true),
            ),
        )
        assertEquals(
            listOf(ChatNoticePage.HypeTrain, ChatNoticePage.Prediction),
            orderedChatNoticePages(pages, levelUp),
        )
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
    fun aScrollSettlesOnTheNearestPageWithinTheRow() {
        assertEquals(0, chatNoticePageAt(scrollPx = 40, pageWidthPx = 100, lastIndex = 2))
        assertEquals(1, chatNoticePageAt(scrollPx = 60, pageWidthPx = 100, lastIndex = 2))
        assertEquals(2, chatNoticePageAt(scrollPx = 900, pageWidthPx = 100, lastIndex = 2))
    }

    @Test
    fun aRowWithoutWidthShowsTheFirstPage() = assertEquals(0, chatNoticePageAt(250, pageWidthPx = 0, lastIndex = 2))

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
