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
        assertEquals(FullscreenChatSwipeOutcome.Hide, outcome(-301f, 0f, chatOnLeft = true))
    }

    @Test
    fun aChatOnTheLeftSwipedRightPastAThirdMovesAcross() {
        assertEquals(FullscreenChatSwipeOutcome.MoveAcross, outcome(301f, 0f, chatOnLeft = true))
    }

    @Test
    fun aChatOnTheRightMirrorsTheDirections() {
        assertEquals(FullscreenChatSwipeOutcome.Hide, outcome(301f, 0f, chatOnLeft = false))
        assertEquals(FullscreenChatSwipeOutcome.MoveAcross, outcome(-301f, 0f, chatOnLeft = false))
    }

    @Test
    fun aShortSwipeSpringsBack() {
        assertEquals(FullscreenChatSwipeOutcome.Stay, outcome(-299f, 0f, chatOnLeft = true))
        assertEquals(FullscreenChatSwipeOutcome.Stay, outcome(0f, 0f, chatOnLeft = true))
    }

    @Test
    fun aFlingCountsInTheDirectionTheChatMoved() {
        assertEquals(FullscreenChatSwipeOutcome.Hide, outcome(-20f, -3201f, chatOnLeft = true))
        assertEquals(FullscreenChatSwipeOutcome.MoveAcross, outcome(20f, 3201f, chatOnLeft = true))
    }

    @Test
    fun aFlingBackTowardWhereTheChatStartedDoesNotCount() {
        assertEquals(FullscreenChatSwipeOutcome.Stay, outcome(-20f, 3201f, chatOnLeft = true))
    }

    @Test
    fun theChatFollowsOutToItsWidthAndAcrossToTheOtherSide() {
        assertEquals(-900f, swipeOffset(-2000f, chatOnLeft = true), 0.001f)
        assertEquals(1350f, swipeOffset(2000f, chatOnLeft = true), 0.001f)
        assertEquals(900f, swipeOffset(2000f, chatOnLeft = false), 0.001f)
        assertEquals(-1350f, swipeOffset(-2000f, chatOnLeft = false), 0.001f)
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
        assertTrue(revealShows(FullscreenChatSide.Right, offsetX = 599f))
        assertFalse(revealShows(FullscreenChatSide.Right, offsetX = 601f))
        assertTrue(revealShows(FullscreenChatSide.Right, offsetX = 880f, velocityX = -3201f))
        assertFalse(revealShows(FullscreenChatSide.Left, offsetX = -880f, velocityX = -3201f))
        assertTrue(revealShows(FullscreenChatSide.Left, offsetX = -599f))
    }

    // A chat 900 px wide, with 1350 px to travel across the screen.
    private fun outcome(offsetX: Float, velocityX: Float, chatOnLeft: Boolean) =
        fullscreenChatSwipeOutcome(offsetX, velocityX, chatOnLeft = chatOnLeft, chatWidthPx = 900)

    private fun swipeOffset(dragX: Float, chatOnLeft: Boolean) =
        fullscreenChatSwipeOffset(dragX, chatOnLeft = chatOnLeft, chatWidthPx = 900, travelPx = 1350)

    private fun revealShows(side: FullscreenChatSide, offsetX: Float, velocityX: Float = 0f) =
        fullscreenChatRevealShows(side, offsetX = offsetX, velocityX = velocityX, chatWidthPx = 900)
}
