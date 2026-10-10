package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFullscreenRulesTest {
    @Test
    fun `an expanded player turned to landscape fills the screen`() {
        assertTrue(playerFullscreen(expanded = true, displayLandscape = true))
    }

    @Test
    fun `full screen waits in portrait until the screen turns`() {
        assertFalse(playerFullscreen(expanded = true, displayLandscape = false))
    }

    @Test
    fun `a minimized player is never full screen`() {
        assertFalse(playerFullscreen(expanded = false, displayLandscape = true))
    }

    @Test
    fun `leaving full screen in landscape holds portrait`() {
        assertEquals(FullscreenLock.Portrait, lockAfterFullscreenExit(displayLandscape = true))
    }

    @Test
    fun `leaving full screen before the rotation lands releases the hold`() {
        assertEquals(FullscreenLock.None, lockAfterFullscreenExit(displayLandscape = false))
    }

    @Test
    fun `minimizing releases only a landscape hold`() {
        assertEquals(FullscreenLock.None, lockAfterMinimize(FullscreenLock.Landscape))
        assertEquals(FullscreenLock.Portrait, lockAfterMinimize(FullscreenLock.Portrait))
        assertEquals(FullscreenLock.None, lockAfterMinimize(FullscreenLock.None))
    }

    @Test
    fun `a portrait hold ends once the device stands upright`() {
        assertEquals(FullscreenLock.None, lockAfterDeviceTurn(FullscreenLock.Portrait, DeviceTurn.Portrait) { false })
        assertEquals(
            FullscreenLock.Portrait,
            lockAfterDeviceTurn(FullscreenLock.Portrait, DeviceTurn.Landscape) { true },
        )
        assertEquals(FullscreenLock.Portrait, lockAfterDeviceTurn(FullscreenLock.Portrait, null) { true })
    }

    @Test
    fun `a landscape hold ends once the device lies sideways with auto-rotate on`() {
        assertEquals(FullscreenLock.None, lockAfterDeviceTurn(FullscreenLock.Landscape, DeviceTurn.Landscape) { true })
        assertEquals(
            FullscreenLock.Landscape,
            lockAfterDeviceTurn(FullscreenLock.Landscape, DeviceTurn.Portrait) { true },
        )
    }

    @Test
    fun `a landscape hold stays with auto-rotate off`() {
        assertEquals(
            FullscreenLock.Landscape,
            lockAfterDeviceTurn(FullscreenLock.Landscape, DeviceTurn.Landscape) { false },
        )
    }

    @Test
    fun `device degrees near each axis read as that turn`() {
        assertEquals(DeviceTurn.Portrait, deviceTurn(0))
        assertEquals(DeviceTurn.Portrait, deviceTurn(30))
        assertEquals(DeviceTurn.Portrait, deviceTurn(330))
        assertEquals(DeviceTurn.Portrait, deviceTurn(180))
        assertEquals(DeviceTurn.Landscape, deviceTurn(90))
        assertEquals(DeviceTurn.Landscape, deviceTurn(60))
        assertEquals(DeviceTurn.Landscape, deviceTurn(270))
        assertEquals(DeviceTurn.Landscape, deviceTurn(300))
    }

    @Test
    fun `any drag in full screen leaves it`() {
        assertEquals(FullscreenDrag.Exit, fullscreenDragGesture(active = true, canEnter = false, offsetY = 10f))
        assertEquals(FullscreenDrag.Exit, fullscreenDragGesture(active = true, canEnter = true, offsetY = -10f))
    }

    @Test
    fun `a drag up on the expanded player enters full screen and a drag down minimizes`() {
        assertEquals(FullscreenDrag.Enter, fullscreenDragGesture(active = false, canEnter = true, offsetY = -10f))
        assertEquals(FullscreenDrag.Other, fullscreenDragGesture(active = false, canEnter = true, offsetY = 10f))
    }

    @Test
    fun `a drag up on the mini player stays with the expand gesture`() {
        assertEquals(FullscreenDrag.Other, fullscreenDragGesture(active = false, canEnter = false, offsetY = -10f))
    }

    @Test
    fun `the player follows the finger at half pace and only in the gesture direction`() {
        assertEquals(50f, fullscreenDragOffset(FullscreenDrag.Exit, 100f), 0.001f)
        assertEquals(0f, fullscreenDragOffset(FullscreenDrag.Exit, -100f), 0.001f)
        assertEquals(-50f, fullscreenDragOffset(FullscreenDrag.Enter, -100f), 0.001f)
        assertEquals(0f, fullscreenDragOffset(FullscreenDrag.Enter, 100f), 0.001f)
        assertEquals(0f, fullscreenDragOffset(FullscreenDrag.Other, 100f), 0.001f)
    }

    @Test
    fun `a drag down leaves full screen where the same drag would minimize the player`() {
        assertTrue(fullscreenDragToggles(FullscreenDrag.Exit, 240f, 0f, 1000f))
        assertTrue(fullscreenDragToggles(FullscreenDrag.Exit, 20f, 3201f, 1000f))
        assertFalse(fullscreenDragToggles(FullscreenDrag.Exit, 220f, 0f, 1000f))
        assertFalse(fullscreenDragToggles(FullscreenDrag.Other, 300f, 0f, 1000f))
    }

    @Test
    fun `a drag up enters full screen at half the minimize distance`() {
        assertTrue(fullscreenDragToggles(FullscreenDrag.Enter, -120f, 0f, 1000f))
        assertTrue(fullscreenDragToggles(FullscreenDrag.Enter, -20f, -3201f, 1000f))
        assertFalse(fullscreenDragToggles(FullscreenDrag.Enter, -110f, 0f, 1000f))
    }

    @Test
    fun `the player shrinks dragged down and reaches its full change at the threshold`() {
        assertEquals(1f, fullscreenDragScale(0f, 1000f), 0.0001f)
        assertEquals(0.95f, fullscreenDragScale(57.75f, 1000f), 0.0001f)
        assertEquals(0.9f, fullscreenDragScale(115.5f, 1000f), 0.0001f)
        assertEquals(0.9f, fullscreenDragScale(2000f, 1000f), 0.0001f)
    }

    @Test
    fun `the player grows dragged up and reaches its full change at the threshold`() {
        assertEquals(1.05f, fullscreenDragScale(-28.875f, 1000f), 0.0001f)
        assertEquals(1.1f, fullscreenDragScale(-57.75f, 1000f), 0.0001f)
        assertEquals(1.1f, fullscreenDragScale(-2000f, 1000f), 0.0001f)
    }

    @Test
    fun `a flat or halfway tilted device reads as no turn`() {
        assertNull(deviceTurn(-1))
        assertNull(deviceTurn(45))
        assertNull(deviceTurn(315))
    }
}
