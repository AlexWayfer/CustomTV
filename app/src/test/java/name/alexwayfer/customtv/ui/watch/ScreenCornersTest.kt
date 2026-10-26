package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

/** A phone's corner: 132 px, with the first line of text 29 px down. */
private const val RADIUS = 132
private const val TEXT_TOP = 29

class ScreenCornersTest {
    @Test
    fun `the corner reaches in by its curve at the line's depth`() {
        assertEquals(49, roundedCornerInsetPx(RADIUS, TEXT_TOP, sideInsetPx = 0))
    }

    @Test
    fun `the corner reaches in by its whole radius at the top edge`() {
        assertEquals(RADIUS, roundedCornerInsetPx(RADIUS, depthPx = 0, sideInsetPx = 0))
    }

    @Test
    fun `below the corner the side is straight`() {
        assertEquals(0, roundedCornerInsetPx(RADIUS, depthPx = RADIUS, sideInsetPx = 0))
    }

    @Test
    fun `room already kept from the side counts`() {
        assertEquals(19, roundedCornerInsetPx(RADIUS, TEXT_TOP, sideInsetPx = 30))
    }

    @Test
    fun `a display cutout wider than the corner leaves nothing to add`() {
        assertEquals(0, roundedCornerInsetPx(RADIUS, TEXT_TOP, sideInsetPx = 173))
    }

    @Test
    fun `a square corner needs no room`() {
        assertEquals(0, roundedCornerInsetPx(radiusPx = 0, depthPx = TEXT_TOP, sideInsetPx = 0))
    }
}
