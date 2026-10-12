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

    /** [chatTailGrowthToFollow] for a chat at the live edge, still, caught up, whose tail grew by 120 px. */
    private fun growth(
        stickToBottom: Boolean = true,
        scrolling: Boolean = false,
        tailCaughtUp: Boolean = true,
        pinnedOverflow: Int = 0,
        overflow: Int = 120,
    ) = chatTailGrowthToFollow(stickToBottom, scrolling, tailCaughtUp, pinnedOverflow, overflow)

    @Test
    fun aPreviewThatGrowsTheLiveEdgeIsFollowedAndATallArrivalIsNot() {
        assertEquals(120, growth())
        assertEquals(0, growth(overflow = 1))
        assertEquals(0, growth(pinnedOverflow = 800, overflow = 920))
        assertEquals(0, growth(stickToBottom = false))
        assertEquals(0, growth(scrolling = true))
        assertEquals(0, growth(tailCaughtUp = false))
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
