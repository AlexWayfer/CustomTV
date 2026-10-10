package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerGesturesTest {
    @Test
    fun `dragging expanded player down increases collapse progress`() {
        assertEquals(0f, miniPlayerDragProgress(-100f, 1000f, false), 0.0001f)
        assertEquals(0.5f, miniPlayerDragProgress(275f, 1000f, false), 0.0001f)
        assertEquals(1f, miniPlayerDragProgress(1000f, 1000f, false), 0.0001f)
    }

    @Test
    fun `dragging mini player up decreases collapse progress`() {
        assertEquals(1f, miniPlayerDragProgress(100f, 1000f, true), 0.0001f)
        assertEquals(0.5f, miniPlayerDragProgress(-275f, 1000f, true), 0.0001f)
        assertEquals(0f, miniPlayerDragProgress(-1000f, 1000f, true), 0.0001f)
    }

    @Test
    fun `vertical settle keeps the existing thresholds`() {
        assertTrue(shouldStayMinimized(0.43f, 0f, false))
        assertTrue(shouldStayMinimized(0.1f, 3201f, false))
        assertFalse(shouldStayMinimized(0.42f, 3200f, false))
        assertTrue(shouldStayMinimized(0.56f, -2799f, true))
        assertFalse(shouldStayMinimized(0.55f, 0f, true))
        assertFalse(shouldStayMinimized(0.9f, -2800f, true))
    }

    @Test
    fun `horizontal settle closes only past distance or velocity thresholds`() {
        assertTrue(shouldCloseMiniPlayer(39f, 0f, 100f))
        assertTrue(shouldCloseMiniPlayer(10f, 1101f, 100f))
        assertFalse(shouldCloseMiniPlayer(38f, 1100f, 100f))
        assertFalse(shouldCloseMiniPlayer(10f, -2000f, 100f))
    }

    @Test
    fun `horizontal swipe alpha is clamped`() {
        assertEquals(1f, miniPlayerSwipeAlpha(0f, 100f), 0.0001f)
        assertEquals(0.5f, miniPlayerSwipeAlpha(22.5f, 100f), 0.0001f)
        assertEquals(0f, miniPlayerSwipeAlpha(100f, 100f), 0.0001f)
    }

    @Test
    fun `player bounds preserve expanded and minimized positions`() {
        val expanded = miniPlayerBounds(
            fullWidthPx = 1000f,
            fullHeightPx = 2000f,
            miniWidthPx = 250f,
            miniPaddingPx = 10f,
            statusBarTopPx = 30f,
            navigationBarBottomPx = 50f,
            progress = 0f,
        )
        assertEquals(MiniPlayerBounds(0, 30, 1000, 563), expanded)

        val minimized = miniPlayerBounds(
            fullWidthPx = 1000f,
            fullHeightPx = 2000f,
            miniWidthPx = 250f,
            miniPaddingPx = 10f,
            statusBarTopPx = 30f,
            navigationBarBottomPx = 50f,
            progress = 1f,
        )
        assertEquals(MiniPlayerBounds(740, 1799, 250, 141), minimized)
    }

    @Test
    fun `visual state preserves chrome and controls thresholds`() {
        val expanded = miniPlayerVisualState(0f, 0f, 100f, expanding = false)
        assertEquals(1f, expanded.swipeAlpha, 0.0001f)
        assertEquals(1f, expanded.chromeAlpha, 0.0001f)
        assertEquals(0f, expanded.controlsAlpha, 0.0001f)

        val middle = miniPlayerVisualState(0.5f, 22.5f, 100f, expanding = true)
        assertEquals(0.5f, middle.swipeAlpha, 0.0001f)
        assertEquals(0f, middle.chromeAlpha, 0.0001f)
        assertEquals(0f, middle.controlsAlpha, 0.0001f)

        val minimized = miniPlayerVisualState(1f, 100f, 100f, expanding = false)
        assertEquals(0f, minimized.swipeAlpha, 0.0001f)
        assertEquals(0f, minimized.chromeAlpha, 0.0001f)
        assertEquals(1f, minimized.controlsAlpha, 0.0001f)
    }

    @Test
    fun `a tap on the settled mini player expands it`() {
        assertTrue(playerTapExpands(inPictureInPicture = false, compactSettled = true, progress = 1f))
    }

    @Test
    fun `a tap on a player almost in the corner expands it`() {
        assertTrue(playerTapExpands(inPictureInPicture = false, compactSettled = false, progress = 0.9f))
    }

    @Test
    fun `a tap on the full player goes to the player`() {
        assertFalse(playerTapExpands(inPictureInPicture = false, compactSettled = false, progress = 0f))
    }

    @Test
    fun `a tap in picture-in-picture does not expand`() {
        assertFalse(playerTapExpands(inPictureInPicture = true, compactSettled = false, progress = 0f))
    }

    @Test
    fun `a full player starting into the corner hides the controls at once`() {
        assertTrue(controlsHideAsCollapseStarts(collapsing = true, minimized = false))
    }

    @Test
    fun `a mini player swiped away or a player at rest does not hide them again`() {
        assertFalse(controlsHideAsCollapseStarts(collapsing = true, minimized = true))
        assertFalse(controlsHideAsCollapseStarts(collapsing = false, minimized = false))
    }
}
