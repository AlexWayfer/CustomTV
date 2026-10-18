package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A portrait player, as wide as the video. */
private val portrait = PlayerZoomFrame(width = 1600f, height = 900f)

/** A full screen player on a 20:9 screen, with black bars beside the video. */
private val landscape = PlayerZoomFrame(width = 2000f, height = 900f)

private val center = ZoomPoint(0f, 0f)

class PlayerZoomTest {
    @Test
    fun `a pinch at the center scales by how far the fingers spread`() {
        val zoom = pinchedPlayerZoom(PlayerZoomTransform(), PlayerPinch(100f, 250f, center, center), portrait)

        assertEquals(PlayerZoomTransform(scale = 2.5f), zoom)
    }

    @Test
    fun `the picture's point under the fingers stays under them`() {
        val focus = ZoomPoint(400f, 200f)

        val zoom = pinchedPlayerZoom(PlayerZoomTransform(), PlayerPinch(100f, 200f, focus, focus), portrait)

        assertEquals(PlayerZoomTransform(scale = 2f, x = -400f, y = -200f), zoom)
        // The point 400 and 200 pixels from the center now shows 800 and 400 from it, moved back by the zoom.
        assertEquals(focus, playerZoomPoint(1200f, 650f, zoom, portrait))
    }

    @Test
    fun `fingers that move together move the picture along`() {
        val zoom = pinchedPlayerZoom(
            PlayerZoomTransform(scale = 2f),
            PlayerPinch(100f, 100f, center, ZoomPoint(50f, -30f)),
            portrait,
        )

        assertEquals(PlayerZoomTransform(scale = 2f, x = 50f, y = -30f), zoom)
    }

    @Test
    fun `a pinch stops at the largest zoom`() {
        val zoom = pinchedPlayerZoom(PlayerZoomTransform(scale = 3f), PlayerPinch(100f, 300f, center, center), portrait)

        assertEquals(PLAYER_ZOOM_MAX, zoom.scale)
    }

    @Test
    fun `a pinch in stops at the whole picture`() {
        val zoom = pinchedPlayerZoom(
            PlayerZoomTransform(scale = 2f, x = 300f, y = 100f),
            PlayerPinch(400f, 100f, center, center),
            portrait,
        )

        assertEquals(PlayerZoomTransform(), zoom)
        assertFalse(zoom.zoomed)
    }

    @Test
    fun `the zoomed video keeps covering the player`() {
        val zoom = pannedPlayerZoom(PlayerZoomTransform(scale = 2f), dx = 5000f, dy = -5000f, frame = portrait)

        assertEquals(PlayerZoomTransform(scale = 2f, x = 800f, y = -450f), zoom)
    }

    @Test
    fun `a video narrower than the player stays in the middle across, and moves only up and down`() {
        // 1600 wide video zoomed 1.2 times is 1920, short of the 2000 wide player.
        val zoom = pannedPlayerZoom(PlayerZoomTransform(scale = 1.2f), dx = 300f, dy = 300f, frame = landscape)

        assertEquals(0f, zoom.x)
        assertEquals(90f, zoom.y, 0.01f)
    }

    @Test
    fun `the black bars beside the video do not come into a zoomed full screen`() {
        // 1600 wide video zoomed 2 times is 3200: 600 pixels past each side of the 2000 wide player.
        val zoom = pannedPlayerZoom(PlayerZoomTransform(scale = 2f), dx = 1000f, dy = 0f, frame = landscape)

        assertEquals(600f, zoom.x)
    }

    @Test
    fun `an empty player keeps the zoom in the middle`() {
        val zoom = pannedPlayerZoom(PlayerZoomTransform(scale = 2f), dx = 100f, dy = 100f, PlayerZoomFrame(0f, 0f))

        assertEquals(PlayerZoomTransform(scale = 2f), zoom)
    }

    @Test
    fun `a barely zoomed picture goes back to the whole one`() {
        assertEquals(PlayerZoomTransform(), settledPlayerZoom(PlayerZoomTransform(scale = 1.05f, x = 20f)))
    }

    @Test
    fun `a zoomed picture stays where the fingers left it`() {
        val zoom = PlayerZoomTransform(scale = PLAYER_ZOOM_SNAP, x = 20f)

        assertEquals(zoom, settledPlayerZoom(zoom))
        assertTrue(zoom.zoomed)
    }
}
