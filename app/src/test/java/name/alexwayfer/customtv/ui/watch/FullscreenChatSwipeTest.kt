package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenChatSwipeTest {
    @Test
    fun aChatOnTheLeftSwipedLeftPastAThirdHides() {
        assertEquals(FullscreenChatSwipeOutcome.Hide, fullscreenChatSwipeOutcome(-301f, 0f, chatOnLeft = true, chatWidthPx = 900))
    }

    @Test
    fun aChatOnTheLeftSwipedRightPastAThirdMovesAcross() {
        assertEquals(
            FullscreenChatSwipeOutcome.MoveAcross,
            fullscreenChatSwipeOutcome(301f, 0f, chatOnLeft = true, chatWidthPx = 900),
        )
    }

    @Test
    fun aChatOnTheRightMirrorsTheDirections() {
        assertEquals(FullscreenChatSwipeOutcome.Hide, fullscreenChatSwipeOutcome(301f, 0f, chatOnLeft = false, chatWidthPx = 900))
        assertEquals(
            FullscreenChatSwipeOutcome.MoveAcross,
            fullscreenChatSwipeOutcome(-301f, 0f, chatOnLeft = false, chatWidthPx = 900),
        )
    }

    @Test
    fun aShortSwipeSpringsBack() {
        assertEquals(FullscreenChatSwipeOutcome.Stay, fullscreenChatSwipeOutcome(-299f, 0f, chatOnLeft = true, chatWidthPx = 900))
        assertEquals(FullscreenChatSwipeOutcome.Stay, fullscreenChatSwipeOutcome(0f, 0f, chatOnLeft = true, chatWidthPx = 900))
    }

    @Test
    fun aFlingCountsInTheDirectionTheChatMoved() {
        assertEquals(FullscreenChatSwipeOutcome.Hide, fullscreenChatSwipeOutcome(-20f, -3201f, chatOnLeft = true, chatWidthPx = 900))
        assertEquals(
            FullscreenChatSwipeOutcome.MoveAcross,
            fullscreenChatSwipeOutcome(20f, 3201f, chatOnLeft = true, chatWidthPx = 900),
        )
    }

    @Test
    fun aFlingBackTowardWhereTheChatStartedDoesNotCount() {
        assertEquals(FullscreenChatSwipeOutcome.Stay, fullscreenChatSwipeOutcome(-20f, 3201f, chatOnLeft = true, chatWidthPx = 900))
    }

    @Test
    fun theChatFollowsOutToItsWidthAndAcrossToTheOtherSide() {
        assertEquals(-900f, fullscreenChatSwipeOffset(-2000f, chatOnLeft = true, chatWidthPx = 900, travelPx = 1350), 0.001f)
        assertEquals(1350f, fullscreenChatSwipeOffset(2000f, chatOnLeft = true, chatWidthPx = 900, travelPx = 1350), 0.001f)
        assertEquals(900f, fullscreenChatSwipeOffset(2000f, chatOnLeft = false, chatWidthPx = 900, travelPx = 1350), 0.001f)
        assertEquals(-1350f, fullscreenChatSwipeOffset(-2000f, chatOnLeft = false, chatWidthPx = 900, travelPx = 1350), 0.001f)
    }

    @Test
    fun theChatFadesOnlyAsItLeavesPastItsEdge() {
        assertEquals(0.5f, fullscreenChatSwipeAlpha(-450f, chatOnLeft = true, chatWidthPx = 900), 0.001f)
        assertEquals(1f, fullscreenChatSwipeAlpha(450f, chatOnLeft = true, chatWidthPx = 900), 0.001f)
        assertEquals(0f, fullscreenChatSwipeAlpha(900f, chatOnLeft = false, chatWidthPx = 900), 0.001f)
        assertEquals(1f, fullscreenChatSwipeAlpha(450f, chatOnLeft = true, chatWidthPx = 0), 0.001f)
    }

    @Test
    fun aVideoBesideAColumnChatFollowsItHalfwayOutAndAcrossByItsWidth() {
        val column = fullscreenChatBounds(FullscreenChatMode.Column, FullscreenChatSide.Left, 2250)
        assertEquals(-450f, fullscreenPlayerSwipeShiftPx(-900f, column, travelPx = 1350), 0.001f)
        assertEquals(-788f, fullscreenPlayerSwipeShiftPx(1350f, column, travelPx = 1350), 0.001f)
    }

    @Test
    fun aVideoUnderAnOverlaidChatStaysPut() {
        val overlay = fullscreenChatBounds(FullscreenChatMode.Overlay, FullscreenChatSide.Left, 2250)
        assertEquals(0f, fullscreenPlayerSwipeShiftPx(-900f, overlay, travelPx = 1350), 0.001f)
        assertEquals(0f, fullscreenPlayerSwipeShiftPx(-900f, null, travelPx = 1350), 0.001f)
    }

    @Test
    fun aSwipeLeftOnTheVideoBringsTheChatFromTheRightAndASwipeRightFromTheLeft() {
        assertEquals(FullscreenChatSide.Right, fullscreenChatRevealSide(-10f))
        assertEquals(FullscreenChatSide.Left, fullscreenChatRevealSide(10f))
    }

    @Test
    fun theComingChatStartsPastItsEdgeAndFollowsTheFingerIn() {
        assertEquals(900f, fullscreenChatRevealOffset(FullscreenChatSide.Right, 0f, 900), 0.001f)
        assertEquals(600f, fullscreenChatRevealOffset(FullscreenChatSide.Right, -300f, 900), 0.001f)
        assertEquals(0f, fullscreenChatRevealOffset(FullscreenChatSide.Right, -2000f, 900), 0.001f)
        assertEquals(900f, fullscreenChatRevealOffset(FullscreenChatSide.Right, 300f, 900), 0.001f)
        assertEquals(-600f, fullscreenChatRevealOffset(FullscreenChatSide.Left, 300f, 900), 0.001f)
        assertEquals(0f, fullscreenChatRevealOffset(FullscreenChatSide.Left, 2000f, 900), 0.001f)
    }

    @Test
    fun theComingChatStaysOnceAThirdIsInOrItIsFlungIn() {
        assertTrue(fullscreenChatRevealShows(FullscreenChatSide.Right, offsetX = 599f, velocityX = 0f, chatWidthPx = 900))
        assertFalse(fullscreenChatRevealShows(FullscreenChatSide.Right, offsetX = 601f, velocityX = 0f, chatWidthPx = 900))
        assertTrue(fullscreenChatRevealShows(FullscreenChatSide.Right, offsetX = 880f, velocityX = -3201f, chatWidthPx = 900))
        assertFalse(fullscreenChatRevealShows(FullscreenChatSide.Left, offsetX = -880f, velocityX = -3201f, chatWidthPx = 900))
        assertTrue(fullscreenChatRevealShows(FullscreenChatSide.Left, offsetX = -599f, velocityX = 0f, chatWidthPx = 900))
    }
}
