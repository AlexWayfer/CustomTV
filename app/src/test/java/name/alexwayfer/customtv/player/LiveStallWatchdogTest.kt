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
        waitingForPlaylistWithLowBuffer: Boolean = false,
    ): List<Pair<Long, LiveStallAction>> {
        val actions = mutableListOf<Pair<Long, LiveStallAction>>()
        repeat(seconds) {
            nowMs += 1_000
            position += step
            val report = tick(
                latency = latency,
                waitingForPlaylistWithLowBuffer = waitingForPlaylistWithLowBuffer,
            )
            val action = watchdog.tick(nowMs, watch, report)
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
    fun onlyTheFirstSegmentRetryCanDeferRestartDuringAStall() {
        play(5)
        play(9, step = 0.0)
        assertEquals(LiveStallAction.None, tickWithRetry(15_000L, "first"))
        assertEquals(LiveStallAction.None, tickWithRetry(16_000L, "first"))
        assertEquals(LiveStallAction.RestartPlayer, tickWithRetry(16_950L, "second", deadline = true))
    }

    @Test
    fun playbackCanResumeDuringTheSegmentRetryWindow() {
        play(5)
        play(9, step = 0.0)
        assertEquals(LiveStallAction.None, tickWithRetry(15_000L, "first"))
        nowMs = 15_000L
        assertEquals(LiveStallAction.None, tickWithBuffer(step = 0.7659))
        assertEquals(LiveStallAction.None, tickWithRetry(16_950L, null, deadline = true))
    }

    @Test
    fun aRetryDeadlineWithoutADeferredRestartDoesNotChangeTheStallTiming() {
        play(5)
        play(9, step = 0.0)
        assertEquals(LiveStallAction.None, tickWithRetry(15_000L, null, deadline = true))
        assertEquals(listOf(15_000L to LiveStallAction.RestartPlayer), play(1, step = 0.0))
    }

    private fun tickWithRetry(atMs: Long, retryId: String?, deadline: Boolean = false): LiveStallAction =
        watchdog.tick(
            atMs,
            LiveStallWatch.Playing,
            tick().copy(segmentRetry = LiveSegmentRetry(retryId, canWait = retryId != null, deadline = deadline)),
        )

    @Test
    fun aVideoStuckForNineSecondsIsLeftAlone() {
        play(5)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0))
    }

    @Test
    fun playbackResumingWithBufferBeforeTheDeadlineStartsTheStallTimerOver() {
        play(5)
        play(9, step = 0.0)
        assertEquals(LiveStallAction.None, tickWithBuffer(step = 0.7659))
        assertTrue(play(9, step = 0.0).isEmpty())
        assertEquals(listOf(25_000L to LiveStallAction.RestartPlayer), play(1, step = 0.0))
    }

    @Test
    fun aSmallNudgeWithBufferDoesNotRestartTheStallTimer() {
        play(5)
        play(9, step = 0.0)
        assertEquals(LiveStallAction.RestartPlayer, tickWithBuffer(step = 0.1))
    }

    @Test
    fun fillingTheBufferWithoutPositionProgressDoesNotRestartTheStallTimer() {
        play(5)
        play(9, step = 0.0)
        assertEquals(LiveStallAction.RestartPlayer, tickWithBuffer(step = 0.0))
    }

    @Test
    fun subsecondMovementWithoutBufferDoesNotRestartTheStallTimer() {
        play(5)
        play(9, step = 0.0)
        assertEquals(listOf(15_000L to LiveStallAction.RestartPlayer), play(1, step = 0.7659))
    }

    private fun tickWithBuffer(step: Double): LiveStallAction {
        nowMs += 1_000
        position += step
        return watchdog.tick(nowMs, LiveStallWatch.Playing, tick(latency = 12.82, hasPlaybackBuffer = true))
    }

    @Test
    fun aStallWithAnUnansweredPlaylistAndLowBufferUsesTheFirstOfTwoPageReloads() {
        play(5)
        assertTrue(play(9, step = 0.0, waitingForPlaylistWithLowBuffer = true).isEmpty())
        assertEquals(
            listOf(
                15_000L to LiveStallAction.ReloadPage,
                25_000L to LiveStallAction.ReloadPage,
                35_000L to LiveStallAction.GiveUp,
            ),
            play(21, step = 0.0, waitingForPlaylistWithLowBuffer = true),
        )
    }

    @Test
    fun aNetworkSwitchKeepsTheNormalFirstStepEvenWhenThePlaylistHasNotAnswered() {
        play(5)
        watchdog.networkChanged(nowMs)
        val actions = play(10, step = 0.0, waitingForPlaylistWithLowBuffer = true)
        assertEquals(listOf(15_000L to LiveStallAction.RestartPlayer), actions)
    }

    @Test
    fun playlistRetryWaitsOutTheExistingNetworkGrace() {
        assertTrue(watchdog.networkGraceElapsed(nowMs))
        watchdog.networkChanged(5_000L)
        assertFalse(watchdog.networkGraceElapsed(14_999L))
        assertTrue(watchdog.networkGraceElapsed(15_000L))
    }

    @Test
    fun aSlowPlaylistDoesNotTurnALatencyRecoveryIntoAPageReload() {
        play(5)
        val actions = play(1, latency = 21.0, waitingForPlaylistWithLowBuffer = true)
        assertEquals(listOf(6_000L to LiveStallAction.RestartPlayer), actions)
    }

    @Test
    fun aViewerPauseOrAdBreakCannotUseAnUnansweredPlaylistToReload() {
        play(5)
        val actions = play(20, step = 0.0, watch = LiveStallWatch.Off, waitingForPlaylistWithLowBuffer = true)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun twitchNudgingThePositionPastAGapIsNoProgress() {
        play(5)
        val actions = mutableListOf<LiveStallAction>()
        repeat(10) { second ->
            nowMs += 1_000
            if (second % 6 == 5) position += 0.1
            actions += watchdog.tick(nowMs, LiveStallWatch.Playing, tick())
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
    fun aNetworkSwitchGivesAStoppedPlayerTenSecondsBeforeRestarting() {
        play(5)
        watchdog.networkChanged(nowMs)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0, watch = LiveStallWatch.Stopped))
        assertEquals(
            listOf(15_000L to LiveStallAction.RestartPlayer),
            play(1, step = 0.0, watch = LiveStallWatch.Stopped),
        )
        assertEquals(
            listOf(25_000L to LiveStallAction.ReloadPage),
            play(10, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aNetworkSwitchGivesABufferingPlayerTimeEvenWhenItIsFarBehind() {
        play(5)
        watchdog.networkChanged(nowMs)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0, latency = 30.0))
        assertEquals(listOf(15_000L to LiveStallAction.RestartPlayer), play(1, step = 0.0, latency = 30.0))
    }

    @Test
    fun aPlayerThatResumesAfterANetworkSwitchNeedsNoRecovery() {
        play(5)
        watchdog.networkChanged(nowMs)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(120))
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(30, step = 0.0, watch = LiveStallWatch.Stopped))
    }

    @Test
    fun anotherNetworkSwitchStartsTheWaitOver() {
        play(5)
        watchdog.networkChanged(nowMs)
        play(9, step = 0.0, watch = LiveStallWatch.Stopped)
        watchdog.networkChanged(nowMs)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(9, step = 0.0, watch = LiveStallWatch.Stopped))
        assertEquals(
            listOf(24_000L to LiveStallAction.RestartPlayer),
            play(1, step = 0.0, watch = LiveStallWatch.Stopped),
        )
    }

    @Test
    fun aViewerPauseCancelsTheWaitAfterANetworkSwitch() {
        play(5)
        watchdog.networkChanged(nowMs)
        play(1, step = 0.0, watch = LiveStallWatch.Off)
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(30, step = 0.0, watch = LiveStallWatch.Stopped))
    }

    @Test
    fun aNewChannelCancelsTheWaitAfterANetworkSwitch() {
        play(5)
        watchdog.networkChanged(nowMs)
        watchdog.reset()
        assertEquals(emptyList<Pair<Long, LiveStallAction>>(), play(30, step = 0.0, watch = LiveStallWatch.Stopped))
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
        assertEquals(LiveStallAction.None, watchdog.tick(nowMs, LiveStallWatch.Playing, tick()))
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

    private fun tick(
        state: String = "Playing",
        adBreak: Boolean = false,
        latency: Double = 3.0,
        waitingForPlaylistWithLowBuffer: Boolean = false,
        hasPlaybackBuffer: Boolean = false,
    ) = LiveTick(
        positionSeconds = position,
        latencySeconds = latency,
        state = state,
        adBreak = adBreak,
        buffered = "[90.0, 110.0]",
        waitingForPlaylistWithLowBuffer = waitingForPlaylistWithLowBuffer,
        hasPlaybackBuffer = hasPlaybackBuffer,
    )
}
