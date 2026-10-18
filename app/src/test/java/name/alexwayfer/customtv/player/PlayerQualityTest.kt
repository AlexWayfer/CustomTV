package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerQualityTest {
    @Test
    fun `the portrait player plays the viewer's own quality`() {
        assertEquals(PlayerQualityMode.User, playerQualityMode(zoomed = false, fullscreen = false))
    }

    @Test
    fun `full screen lets the player choose`() {
        assertEquals(PlayerQualityMode.Auto, playerQualityMode(zoomed = false, fullscreen = true))
    }

    @Test
    fun `a zoomed picture plays the best quality, in full screen too`() {
        assertEquals(PlayerQualityMode.Best, playerQualityMode(zoomed = true, fullscreen = false))
        assertEquals(PlayerQualityMode.Best, playerQualityMode(zoomed = true, fullscreen = true))
    }
}
