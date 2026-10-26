package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStallWatchdogTest {
    private val watchdog = LiveStallWatchdog()
    private var nowMs = 0L
    private var position = 100.0

    /** Reports once a second for [seconds], moving the video by [step] each time, and lists what was not None. */
    private fun play(
        seconds: Int,
        step: Double = 1.0,
        latency: Double = 3.0,
        watching: Boolean = true,
    ): List<Pair<Long, LiveStallAction>> {
        val actions = mutableListOf<Pair<Long, LiveStallAction>>()
        repeat(seconds) {
            nowMs += 1_000
            position += step
            val action = watchdog.tick(nowMs, watching, position, latency)
            if (action != LiveStallAction.None) actions += nowMs to action
        }
        return actions
    }

    @Test
    fun aStreamThatPlaysOnIsLeftAlone() {
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(120))
    }

    @Test
    fun aVideoStuckForTenSecondsRestartsThePlayer() {
        play(5)
        assertEquals(listOf(15_000L to LiveStallAction.RestartPlayer), play(10, step = 0.0))
    }

    @Test
    fun aVideoStuckForNineSecondsIsLeftAlone() {
        play(5)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0))
    }

    @Test
    fun twitchNudgingThePositionPastAGapIsNoProgress() {
        play(5)
        val actions = mutableListOf<LiveStallAction>()
        repeat(10) { second ->
            nowMs += 1_000
            if (second % 6 == 5) position += 0.1
            actions += watchdog.tick(nowMs, true, position, 3.0)
        }
        assertEquals(LiveStallAction.RestartPlayer, actions.last())
    }

    @Test
    fun aVideoStillStuckAfterTheRestartReloadsThePageTwiceThenGivesUp() {
        play(5)
        val actions = play(60, step = 0.0).map { it.second }
        assertEquals(
            listOf(
                LiveStallAction.RestartPlayer,
                LiveStallAction.ReloadPage,
                LiveStallAction.ReloadPage,
                LiveStallAction.GiveUp,
            ),
            actions,
        )
    }

    @Test
    fun eachStepWaitsTenSecondsOfItsOwn() {
        play(5)
        assertEquals(
            listOf(15_000L, 25_000L, 35_000L, 45_000L),
            play(60, step = 0.0).map { it.first },
        )
    }

    @Test
    fun fallingMoreThanTwentySecondsBehindRestartsThePlayerAtOnce() {
        play(5)
        assertEquals(listOf(6_000L to LiveStallAction.RestartPlayer), play(1, latency = 21.0))
    }

    @Test
    fun twentySecondsBehindIsLeftAlone() {
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(60, latency = 20.0))
    }

    @Test
    fun stillBehindThirtySecondsAfterTheRestartReloadsThePage() {
        play(5)
        play(1, latency = 30.0)
        assertEquals(listOf(36_000L to LiveStallAction.ReloadPage), play(30, latency = 30.0))
    }

    @Test
    fun anUnknownLatencyIsNotBehind() {
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(60, latency = Double.NaN))
    }

    @Test
    fun thirtyCalmSecondsAfterARestartStartTheStepsOver() {
        play(5)
        play(10, step = 0.0)
        play(30)
        assertEquals(LiveStallAction.RestartPlayer, play(10, step = 0.0).single().second)
    }

    @Test
    fun aRestartFollowedByLessThanThirtyCalmSecondsGoesOnToThePageReload() {
        play(5)
        play(10, step = 0.0)
        play(20)
        assertEquals(LiveStallAction.ReloadPage, play(10, step = 0.0).single().second)
    }

    @Test
    fun aPauseOfTheViewerIsNoStall() {
        play(5)
        play(60, step = 0.0, watching = false)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0))
    }

    @Test
    fun aPauseOfTheViewerIsNotBehind() {
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(60, latency = 60.0, watching = false))
    }

    @Test
    fun aGapBetweenReportsSaysNothingAboutTheVideo() {
        play(5)
        nowMs += 60_000
        assertEquals(LiveStallAction.None, watchdog.tick(nowMs, true, position, 3.0))
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0))
    }

    @Test
    fun aNewTimelineAfterARestartCountsAsProgress() {
        play(5)
        play(9, step = 0.0)
        position = 0.0
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0))
    }

    @Test
    fun aResetForgetsTheStepsTaken() {
        play(5)
        play(10, step = 0.0)
        watchdog.reset()
        play(5)
        assertEquals(LiveStallAction.RestartPlayer, play(10, step = 0.0).single().second)
    }

    @Test
    fun aConfirmedLiveStreamThatTwitchMeansToPlayIsWatched() {
        assertTrue(LiveStallWatchdog.watches(live = true, playbackConfirmed = true, userPaused = false, tick()))
        assertTrue(
            LiveStallWatchdog.watches(live = true, playbackConfirmed = true, userPaused = false, tick("Buffering")),
        )
    }

    @Test
    fun aRecordingIsNotWatched() {
        assertFalse(LiveStallWatchdog.watches(live = false, playbackConfirmed = true, userPaused = false, tick()))
    }

    @Test
    fun aStreamThatHasNotPlayedYetIsNotWatched() {
        assertFalse(LiveStallWatchdog.watches(live = true, playbackConfirmed = false, userPaused = false, tick()))
    }

    @Test
    fun aStreamTheViewerPausedIsNotWatched() {
        assertFalse(LiveStallWatchdog.watches(live = true, playbackConfirmed = true, userPaused = true, tick()))
    }

    @Test
    fun anAdBreakIsNotWatched() {
        assertFalse(
            LiveStallWatchdog.watches(
                live = true,
                playbackConfirmed = true,
                userPaused = false,
                tick(adBreak = true),
            ),
        )
    }

    @Test
    fun aStreamTwitchStoppedIsNotWatched() {
        assertFalse(LiveStallWatchdog.watches(live = true, playbackConfirmed = true, userPaused = false, tick("Ended")))
        assertFalse(LiveStallWatchdog.watches(live = true, playbackConfirmed = true, userPaused = false, tick("Idle")))
    }

    private fun tick(state: String = "Playing", adBreak: Boolean = false) = LiveTick(
        positionSeconds = 100.0,
        latencySeconds = 3.0,
        state = state,
        adBreak = adBreak,
        buffered = "[90.0, 110.0]",
    )
}
