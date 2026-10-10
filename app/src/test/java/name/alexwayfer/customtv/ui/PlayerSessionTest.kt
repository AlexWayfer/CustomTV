package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.ui.account.ProfileVideoPlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSessionTest {
    private val video = ProfileVideoPlayback(
        id = "1",
        channel = "channel",
        title = "Title",
        thumbnailUrl = null,
        startedAtMillis = null,
        durationSeconds = 60L,
        viewCount = 0L,
    )

    @Test
    fun openingAStreamClosesAMinimizedRecording() {
        val watchingRecording = PlayerSession(video = video, videoMinimized = true)

        val opened = watchingRecording.liveOpened()

        assertNull(opened.video)
        assertFalse(opened.videoMinimized)
    }

    @Test
    fun openingAStreamShowsItFullScreenAfterAClosedPip() {
        val opened = PlayerSession(liveMinimized = true, liveClosedWithPip = true).liveOpened()

        assertFalse(opened.liveMinimized)
        assertFalse(opened.liveClosedWithPip)
    }

    @Test
    fun openingARecordingShowsItFullScreenAndTakesTheStreamStateFromTheDecision() {
        val opened = PlayerSession(videoMinimized = true).videoOpened(video, liveMinimized = true)

        assertEquals(video, opened.video)
        assertFalse(opened.videoMinimized)
        assertTrue(opened.liveMinimized)
    }

    @Test
    fun closingARecordingForgetsItsMiniPlayer() {
        val closed = PlayerSession(video = video, videoMinimized = true).videoClosed()

        assertNull(closed.video)
        assertFalse(closed.videoMinimized)
    }

    @Test
    fun closingPipStopsTheStreamAndExpandsItForTheReturn() {
        val closed = PlayerSession(liveMinimized = true).livePipClosed()

        assertTrue(closed.liveClosedWithPip)
        assertFalse(closed.liveMinimized)
        assertFalse(closed.livePipResumed().liveClosedWithPip)
    }

    @Test
    fun expandingPipShowsTheRecordingFullScreen() {
        val expanded = PlayerSession(video = video, videoMinimized = true).pipExpanded(watching = false)

        assertFalse(expanded.videoMinimized)
        assertEquals(video, expanded.video)
    }

    @Test
    fun expandingPipShowsTheStreamFullScreen() {
        assertFalse(PlayerSession(liveMinimized = true).pipExpanded(watching = true).liveMinimized)
    }

    @Test
    fun expandingPipWithoutAStreamKeepsItsState() {
        assertTrue(PlayerSession(liveMinimized = true).pipExpanded(watching = false).liveMinimized)
    }

    @Test
    fun minimizingOnePlayerLeavesTheOtherAlone() {
        val session = PlayerSession(video = video)

        assertEquals(video, session.withLiveMinimized(true).video)
        assertFalse(session.withVideoMinimized(true).liveMinimized)
    }

    @Test
    fun openingWhispersMinimizesTheStream() {
        assertTrue(PlayerSession().whispersOpened(watching = true).liveMinimized)
    }

    @Test
    fun openingWhispersMinimizesTheRecording() {
        val opened = PlayerSession(video = video).whispersOpened(watching = false)

        assertTrue(opened.videoMinimized)
        assertEquals(video, opened.video)
    }

    @Test
    fun returningFromWhispersExpandsTheStream() {
        assertFalse(PlayerSession(liveMinimized = true).returnedFromWhispers(watching = true).liveMinimized)
    }

    @Test
    fun returningFromWhispersExpandsTheRecording() {
        val returned = PlayerSession(video = video, videoMinimized = true).returnedFromWhispers(watching = false)

        assertFalse(returned.videoMinimized)
        assertEquals(video, returned.video)
    }

    @Test
    fun aSectionOpenedFromANotificationMinimizesTheStreamAndTheRecording() {
        assertTrue(PlayerSession().coveringPlayerMinimized(watching = true).liveMinimized)
        assertTrue(PlayerSession(video = video).coveringPlayerMinimized(watching = false).videoMinimized)
        assertEquals(PlayerSession(), PlayerSession().coveringPlayerMinimized(watching = false))
    }

    @Test
    fun openingWhispersWithoutAPlayerKeepsItsState() {
        assertEquals(PlayerSession(), PlayerSession().whispersOpened(watching = false))
    }

    @Test
    fun aFullScreenStreamIsExpandedAgainAfterTheWhispersSection() {
        assertTrue(PlayerSession().fullScreenBeforeWhispers(watching = true))
    }

    @Test
    fun aFullScreenRecordingIsExpandedAgainAfterTheWhispersSection() {
        assertTrue(PlayerSession(video = video).fullScreenBeforeWhispers(watching = false))
    }

    @Test
    fun aMiniPlayerStaysMinimizedAfterTheWhispersSection() {
        assertFalse(PlayerSession(liveMinimized = true).fullScreenBeforeWhispers(watching = true))
        assertFalse(PlayerSession(video = video, videoMinimized = true).fullScreenBeforeWhispers(watching = false))
    }

    @Test
    fun theSleepTimerClosesAMinimizedRecording() {
        val fired = PlayerSession(video = video, videoMinimized = true).sleepTimerFired()

        assertNull(fired.video)
        assertFalse(fired.videoMinimized)
    }

    @Test
    fun theSleepTimerLeavesNoMinimizedStreamBehind() {
        assertFalse(PlayerSession(liveMinimized = true).sleepTimerFired().liveMinimized)
    }

    @Test
    fun withoutAPlayerNothingIsExpandedAfterTheWhispersSection() {
        assertFalse(PlayerSession().fullScreenBeforeWhispers(watching = false))
    }
}
