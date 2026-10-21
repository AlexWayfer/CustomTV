package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatCopyPopupPlacementTest {
    private val window = IntSize(1000, 2000)
    private val popup = IntSize(300, 200)

    private fun place(
        anchor: IntRect,
        windowSize: IntSize = window,
        direction: LayoutDirection = LayoutDirection.Ltr,
        placeAbove: Boolean? = null,
    ) = chatCopyPopupPlacement(anchor, windowSize, direction, popup, placeAbove)

    @Test
    fun roomBelowPlacesPopupBelowAtTheAnchorEnd() {
        val placement = place(IntRect(0, 500, 900, 600))

        assertEquals(ChatCopyPopupPlacement(IntOffset(592, 608), above = false), placement)
    }

    @Test
    fun rightToLeftPlacesPopupAtTheAnchorStart() {
        val placement = place(IntRect(100, 500, 900, 600), direction = LayoutDirection.Rtl)

        assertEquals(IntOffset(108, 608), placement.offset)
    }

    @Test
    fun noRoomBelowPlacesPopupAbove() {
        val placement = place(IntRect(0, 1800, 900, 1900))

        assertEquals(ChatCopyPopupPlacement(IntOffset(592, 1592), above = true), placement)
    }

    @Test
    fun noRoomOnEitherSideStaysBelowAndKeepsInsideTheWindow() {
        val placement = place(IntRect(0, 100, 900, 1900))

        assertEquals(ChatCopyPopupPlacement(IntOffset(592, 1792), above = false), placement)
    }

    @Test
    fun chosenSideIsKeptOnTheNextPass() {
        val placement = place(IntRect(0, 500, 900, 600), placeAbove = true)

        assertEquals(ChatCopyPopupPlacement(IntOffset(592, 292), above = true), placement)
    }

    @Test
    fun zeroWindowPutsPopupAtTheMargin() {
        val placement = place(IntRect(0, 500, 900, 600), windowSize = IntSize.Zero)

        assertEquals(IntOffset(8, 8), placement.offset)
    }

    @Test
    fun windowSmallerThanPopupPutsPopupAtTheMargin() {
        val placement = place(IntRect(0, 50, 250, 100), windowSize = IntSize(250, 150))

        assertEquals(IntOffset(8, 8), placement.offset)
    }

    @Test
    fun anchorPastTheWindowEndKeepsPopupInside() {
        val placement = place(IntRect(0, 500, 1400, 600))

        assertEquals(692, placement.offset.x)
    }
}
