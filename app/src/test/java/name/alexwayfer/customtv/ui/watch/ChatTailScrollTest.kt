package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTailScrollTest {
    @Test
    fun historyInsertedBeforeWelcomePushingItBelowTheViewportJumpsBackToIt() {
        assertTrue(
            chatTailPushedOutOfView(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                lastVisibleIndex = 12,
                totalItems = 22,
            ),
        )
    }

    @Test
    fun aVisibleTailOrAnEmptyListDoesNotJump() {
        assertFalse(
            chatTailPushedOutOfView(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                lastVisibleIndex = 21,
                totalItems = 22,
            ),
        )
        assertFalse(
            chatTailPushedOutOfView(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                lastVisibleIndex = -1,
                totalItems = 0,
            ),
        )
    }

    @Test
    fun aHiddenTailIsLeftAloneWhenScrolledUpScrollingOrStillRevealing() {
        assertFalse(
            chatTailPushedOutOfView(
                stickToBottom = false,
                scrolling = false,
                tailCaughtUp = true,
                lastVisibleIndex = 12,
                totalItems = 22,
            ),
        )
        assertFalse(
            chatTailPushedOutOfView(
                stickToBottom = true,
                scrolling = true,
                tailCaughtUp = true,
                lastVisibleIndex = 12,
                totalItems = 22,
            ),
        )
        assertFalse(
            chatTailPushedOutOfView(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = false,
                lastVisibleIndex = 12,
                totalItems = 22,
            ),
        )
    }

    @Test
    fun aTailThatSticksOutPastTheViewportReportsThatOverflow() {
        assertEquals(120, chatTailOverflow(lastVisibleIndex = 4, totalItems = 5, itemEnd = 900, viewportEnd = 780))
        assertEquals(0, chatTailOverflow(lastVisibleIndex = 4, totalItems = 5, itemEnd = 780, viewportEnd = 780))
        assertEquals(0, chatTailOverflow(lastVisibleIndex = 4, totalItems = 5, itemEnd = 700, viewportEnd = 780))
        assertEquals(0, chatTailOverflow(lastVisibleIndex = 3, totalItems = 5, itemEnd = 900, viewportEnd = 780))
    }

    @Test
    fun aPreviewThatGrowsTheLiveEdgeIsFollowedAndATallArrivalIsNot() {
        assertEquals(
            120,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                pinnedOverflow = 0,
                overflow = 120,
            ),
        )
        assertEquals(
            0,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                pinnedOverflow = 0,
                overflow = 1,
            ),
        )
        assertEquals(
            0,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                pinnedOverflow = 800,
                overflow = 920,
            ),
        )
        assertEquals(
            0,
            chatTailGrowthToFollow(
                stickToBottom = false,
                scrolling = false,
                tailCaughtUp = true,
                pinnedOverflow = 0,
                overflow = 120,
            ),
        )
        assertEquals(
            0,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = true,
                tailCaughtUp = true,
                pinnedOverflow = 0,
                overflow = 120,
            ),
        )
        assertEquals(
            0,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = false,
                pinnedOverflow = 0,
                overflow = 120,
            ),
        )
    }

    @Test
    fun anAnimatedGrowthThatPassesAPixelPastTheEdgeIsStillFollowed() {
        assertEquals(
            24,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                pinnedOverflow = 1,
                overflow = 24,
            ),
        )
        assertEquals(
            0,
            chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = false,
                tailCaughtUp = true,
                pinnedOverflow = 2,
                overflow = 24,
            ),
        )
    }
}
