package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.player.PlayerPlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TRAVEL = 400f
private const val DELTA = 0.0001f

class StreamHidingTest {
    @Test
    fun `a drag up on the shown stream hides it by the share of the travel`() {
        assertEquals(0.25f, streamHideDragProgress(startHidden = false, offsetY = -100f, travelPx = TRAVEL), DELTA)
    }

    @Test
    fun `a drag down on the shown stream leaves it shown`() {
        assertEquals(0f, streamHideDragProgress(startHidden = false, offsetY = 100f, travelPx = TRAVEL), DELTA)
    }

    @Test
    fun `a drag up past the travel hides the stream no further than whole`() {
        assertEquals(1f, streamHideDragProgress(startHidden = false, offsetY = -900f, travelPx = TRAVEL), DELTA)
    }

    @Test
    fun `a drag down on the hidden stream brings it back by the share of the travel`() {
        assertEquals(0.75f, streamHideDragProgress(startHidden = true, offsetY = 100f, travelPx = TRAVEL), DELTA)
    }

    @Test
    fun `a drag up on the hidden stream keeps it hidden`() {
        assertEquals(1f, streamHideDragProgress(startHidden = true, offsetY = -100f, travelPx = TRAVEL), DELTA)
    }

    @Test
    fun `no travel yet does not divide by zero`() {
        assertEquals(1f, streamHideDragProgress(startHidden = false, offsetY = -10f, travelPx = 0f), DELTA)
    }

    @Test
    fun `the shown stream hides past the minimize gesture's share`() {
        assertTrue(streamHiddenAfterDrag(startHidden = false, progress = 0.5f, velocityY = 0f))
    }

    @Test
    fun `the shown stream stays shown short of that share`() {
        assertFalse(streamHiddenAfterDrag(startHidden = false, progress = 0.3f, velocityY = 0f))
    }

    @Test
    fun `a short fast fling up hides the shown stream`() {
        assertTrue(streamHiddenAfterDrag(startHidden = false, progress = 0.1f, velocityY = -4000f))
    }

    @Test
    fun `a short slow drag up leaves the stream shown`() {
        assertFalse(streamHiddenAfterDrag(startHidden = false, progress = 0.1f, velocityY = -1000f))
    }

    @Test
    fun `a fast fling down does not hide the shown stream`() {
        assertFalse(streamHiddenAfterDrag(startHidden = false, progress = 0f, velocityY = 4000f))
    }

    @Test
    fun `the hidden stream comes back past the share`() {
        assertFalse(streamHiddenAfterDrag(startHidden = true, progress = 0.5f, velocityY = 0f))
    }

    @Test
    fun `the hidden stream stays hidden short of the share`() {
        assertTrue(streamHiddenAfterDrag(startHidden = true, progress = 0.7f, velocityY = 0f))
    }

    @Test
    fun `a short fast fling down brings the hidden stream back`() {
        assertFalse(streamHiddenAfterDrag(startHidden = true, progress = 0.9f, velocityY = 4000f))
    }

    @Test
    fun `a fast fling up keeps the hidden stream hidden`() {
        assertTrue(streamHiddenAfterDrag(startHidden = true, progress = 1f, velocityY = -4000f))
    }

    @Test
    fun `a hidden stream stays hidden while paused`() {
        assertTrue(streamStaysHidden(hidden = true, playback = PlayerPlayback.Paused))
    }

    @Test
    fun `a hidden stream shows once it plays`() {
        assertFalse(streamStaysHidden(hidden = true, playback = PlayerPlayback.Playing))
    }

    @Test
    fun `a hidden stream shows once it starts playing`() {
        assertFalse(streamStaysHidden(hidden = true, playback = PlayerPlayback.Starting))
    }

    @Test
    fun `a shown stream stays shown when paused`() {
        assertFalse(streamStaysHidden(hidden = false, playback = PlayerPlayback.Paused))
    }

    @Test
    fun `the room is whole with nothing hidden`() {
        assertEquals(180, streamRoomHeightPx(180, 0f))
    }

    @Test
    fun `the room closes by the hidden share`() {
        assertEquals(45, streamRoomHeightPx(180, 0.75f))
    }

    @Test
    fun `the room is gone with the stream hidden`() {
        assertEquals(0, streamRoomHeightPx(180, 1f))
    }

    @Test
    fun `the room never goes below zero`() {
        assertEquals(0, streamRoomHeightPx(180, 1.5f))
    }

    @Test
    fun `the room never grows past its height`() {
        assertEquals(180, streamRoomHeightPx(180, -0.5f))
    }

    @Test
    fun `a room of no height stays at zero`() {
        assertEquals(0, streamRoomHeightPx(0, 0.5f))
    }

    @Test
    fun `the video is as hidden as the stream on the full player`() {
        assertEquals(0.6f, hiddenVideoFraction(hidden = 0.6f, collapse = 0f), DELTA)
    }

    @Test
    fun `the video comes back into sight on the way to the corner`() {
        assertEquals(0.5f, hiddenVideoFraction(hidden = 1f, collapse = 0.5f), DELTA)
    }

    @Test
    fun `the mini player shows the whole video`() {
        assertEquals(0f, hiddenVideoFraction(hidden = 1f, collapse = 1f), DELTA)
    }

    @Test
    fun `in portrait the room closes by the hidden share`() {
        assertEquals(0.3f, streamRoomClosedFraction(hidden = 0.3f, overScreen = false), DELTA)
    }

    @Test
    fun `in landscape the room stays closed while the stream comes over the chat`() {
        assertEquals(1f, streamRoomClosedFraction(hidden = 0.3f, overScreen = true), DELTA)
    }

    @Test
    fun `in landscape the room is open with the stream all in sight`() {
        assertEquals(0f, streamRoomClosedFraction(hidden = 0f, overScreen = true), DELTA)
    }

    @Test
    fun `in landscape the partly hidden full player covers the screen`() {
        assertTrue(hiddenStreamCoversScreen(overScreen = true, partlyHidden = true, collapse = 0f))
    }

    @Test
    fun `in landscape the stream all in sight keeps the player's own place`() {
        assertFalse(hiddenStreamCoversScreen(overScreen = true, partlyHidden = false, collapse = 0f))
    }

    @Test
    fun `in landscape the hidden player on its way to the corner keeps the player's own place`() {
        assertFalse(hiddenStreamCoversScreen(overScreen = true, partlyHidden = true, collapse = 0.2f))
    }

    @Test
    fun `in portrait the hidden player keeps its own place`() {
        assertFalse(hiddenStreamCoversScreen(overScreen = false, partlyHidden = true, collapse = 0f))
    }

    @Test
    fun `in landscape the content goes down under the video by its share in sight`() {
        assertEquals(270f, contentShift(hidden = 0.25f, overScreen = true, roomPx = 0), DELTA)
    }

    @Test
    fun `in landscape the content goes down under the video that keeps room for the stream info below it`() {
        assertEquals(210f, contentShift(hidden = 0.25f, overScreen = true, roomPx = 80), DELTA)
    }

    @Test
    fun `in landscape the content is in place with the stream all hidden`() {
        assertEquals(0f, contentShift(hidden = 1f, overScreen = true, roomPx = 80), DELTA)
    }

    @Test
    fun `in landscape the content stays in place with the stream all in sight`() {
        assertEquals(0f, contentShift(hidden = 0f, overScreen = true, roomPx = 80), DELTA)
    }

    @Test
    fun `in portrait the content does not shift, the room moves it`() {
        assertEquals(0f, contentShift(hidden = 0.25f, overScreen = false, roomPx = 0), DELTA)
    }

    @Test
    fun `a room taller than the screen does not shift the content up`() {
        assertEquals(0f, contentShift(hidden = 0.25f, overScreen = true, roomPx = 500), DELTA)
    }

    @Test
    fun `the video over the content leaves the stream info's room under it`() {
        assertEquals(280, streamCoverHeightPx(heightPx = 360, roomPx = 80))
    }

    @Test
    fun `the video over the content never goes below zero`() {
        assertEquals(0, streamCoverHeightPx(heightPx = 60, roomPx = 80))
    }

    /** [hiddenStreamContentShiftPx] on a screen 360 px tall. */
    private fun contentShift(hidden: Float, overScreen: Boolean, roomPx: Int) =
        hiddenStreamContentShiftPx(heightPx = 360f, hidden = hidden, overScreen = overScreen, roomPx = roomPx)

    @Test
    fun `in portrait a drag travels the full width player's height`() {
        assertEquals(225f, streamHideTravelPx(widthPx = 400, heightPx = 800, overScreen = false), DELTA)
    }

    @Test
    fun `in landscape a drag travels the screen's height`() {
        assertEquals(360f, streamHideTravelPx(widthPx = 800, heightPx = 360, overScreen = true), DELTA)
    }
}
