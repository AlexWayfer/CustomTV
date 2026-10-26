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
        watch: LiveStallWatch = LiveStallWatch.Playing,
    ): List<Pair<Long, LiveStallAction>> {
        val actions = mutableListOf<Pair<Long, LiveStallAction>>()
        repeat(seconds) {
            nowMs += 1_000
            position += step
            val action = watchdog.tick(nowMs, watch, position, latency)
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
            actions += watchdog.tick(nowMs, LiveStallWatch.Playing, position, 3.0)
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
        play(60, step = 0.0, watch = LiveStallWatch.Off)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0))
    }

    @Test
    fun aPauseOfTheViewerIsNotBehind() {
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(60, latency = 60.0, watch = LiveStallWatch.Off))
    }

    @Test
    fun aStateTwitchStoppedInWithoutAStepIsLeftAlone() {
        play(5)
        assertEquals(
            emptyList<Pair<Long, LiveStallAction>>(),
            play(60, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aStateTwitchStoppedInAfterARestartStillCountsAsStuck() {
        play(5)
        play(10, step = 0.0)
        assertTrue(watchdog.recovering)
        assertEquals(
            listOf(25_000L to LiveStallAction.ReloadPage),
            play(10, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aVideoThatMovesOnAfterARestartEndsTheRecovery() {
        play(5)
        play(10, step = 0.0)
        play(2)
        assertFalse(watchdog.recovering)
        assertEquals(
            emptyList<Pair<Long, LiveStallAction>>(),
            play(30, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aPauseOfTheViewerEndsTheRecovery() {
        play(5)
        play(10, step = 0.0)
        play(1, step = 0.0, watch = LiveStallWatch.Off)
        assertFalse(watchdog.recovering)
        assertEquals(
            emptyList<Pair<Long, LiveStallAction>>(),
            play(30, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aNewTimelineAfterARestartKeepsTheRecovery() {
        play(5)
        play(10, step = 0.0)
        position = 0.0
        assertEquals(
            listOf(26_000L to LiveStallAction.ReloadPage),
            play(11, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aGapBetweenReportsSaysNothingAboutTheVideo() {
        play(5)
        nowMs += 60_000
        assertEquals(LiveStallAction.None, watchdog.tick(nowMs, LiveStallWatch.Playing, position, 3.0))
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
    fun aConfirmedLiveStreamThatTwitchMeansToPlayIsPlaying() {
        assertEquals(LiveStallWatch.Playing, watch(tick()))
        assertEquals(LiveStallWatch.Playing, watch(tick("Buffering")))
    }

    @Test
    fun aRecordingIsOff() {
        assertEquals(LiveStallWatch.Off, watch(tick(), live = false))
    }

    @Test
    fun aStreamThatHasNotPlayedYetIsOff() {
        assertEquals(LiveStallWatch.Off, watch(tick(), playbackConfirmed = false))
    }

    @Test
    fun aStreamTheViewerPausedIsOff() {
        assertEquals(LiveStallWatch.Off, watch(tick("Idle"), userPaused = true))
    }

    @Test
    fun anAdBreakIsOff() {
        assertEquals(LiveStallWatch.Off, watch(tick(adBreak = true)))
    }

    @Test
    fun aStreamTwitchStoppedIsStopped() {
        assertEquals(LiveStallWatch.Stopped, watch(tick("Ended")))
        assertEquals(LiveStallWatch.Stopped, watch(tick("Idle")))
        assertEquals(LiveStallWatch.Stopped, watch(tick("Ready")))
    }

    private fun watch(
        tick: LiveTick,
        live: Boolean = true,
        playbackConfirmed: Boolean = true,
        userPaused: Boolean = false,
    ) = LiveStallWatchdog.watch(live, playbackConfirmed, userPaused, tick)

    private fun tick(state: String = "Playing", adBreak: Boolean = false) = LiveTick(
        positionSeconds = 100.0,
        latencySeconds = 3.0,
        state = state,
        adBreak = adBreak,
        buffered = "[90.0, 110.0]",
    )
}
