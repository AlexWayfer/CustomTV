package name.alexwayfer.customtv.player

// The video moves less than this in a stall: Twitch's player nudges a stuck position by a tenth of a second.
private const val PROGRESS_SECONDS = 1.0
private const val RESUMED_PROGRESS_SECONDS = 0.5
private const val STALL_MS = 10_000L
private const val BEHIND_SECONDS = 20.0
// After a step, a stream that is still behind waits this long for it to work, and one that plays on this long
// without a stall or a lag starts the steps over.
private const val SETTLE_MS = 30_000L
private const val PAGE_RELOAD_LIMIT = 2
// A longer pause between reports says nothing about the video: the page's timers were held, as in the background.
private const val REPORT_GAP_MS = 5_000L

// Twitch's player still means to play the video in these states.
private val WATCHED_STATES = setOf("Playing", "Buffering")

/** What the live player page reports about its video every tick. */
internal data class LiveTick(
    val positionSeconds: Double,
    /** The latency Twitch's player reports, or NaN when it reports none. */
    val latencySeconds: Double,
    /** The state of Twitch's player, such as `Playing`. */
    val state: String,
    val adBreak: Boolean,
    /** The video's buffered ranges, for the log. */
    val buffered: String,
    val waitingForPlaylistWithLowBuffer: Boolean = false,
    val hasPlaybackBuffer: Boolean = false,
    val segmentRetry: LiveSegmentRetry? = null,
)

internal data class LiveSegmentRetry(val id: String?, val canWait: Boolean, val deadline: Boolean = false)

internal enum class LiveStallAction { None, RestartPlayer, ReloadPage, GiveUp }

/** How a report bears on a stall. */
internal enum class LiveStallWatch {
    /** Not a live stream that has played, paused by the viewer, or an ad break: never a stall. */
    Off,

    /** Twitch's player means to play the video. */
    Playing,

    /** Twitch's player stopped: watched during recovery or after a network switch until the video moves on. */
    Stopped,
}

/**
 * Brings back a live stream that Twitch's player keeps reporting as playing while its video stands still or falls
 * far behind the broadcast: it stalls on a gap in its buffer and never reports an error. The first step restarts
 * the player, the next ones reload the page, and then it gives up until the stream plays on again.
 */
internal class LiveStallWatchdog {
    private var anchorSeconds = 0.0
    private var anchorAtMs = 0L
    private var lastTickAtMs: Long? = null
    private var lastPositionSeconds = Double.NaN
    private var stepsTaken = 0
    private var lastStepAtMs: Long? = null
    private var networkWait = false
    @Volatile private var networkGraceUntilMs = 0L
    private var awaitedSegmentRetryId: String? = null

    /** A step was taken and the video has not moved on since, whatever state Twitch's player reports. */
    var recovering = false
        private set

    fun tick(nowMs: Long, watch: LiveStallWatch, tick: LiveTick): LiveStallAction {
        if (tick.segmentRetry?.deadline == true && awaitedSegmentRetryId == null) return LiveStallAction.None
        if (watch == LiveStallWatch.Off) {
            recovering = false
            networkWait = false
        }
        val watching = watch == LiveStallWatch.Playing || watch == LiveStallWatch.Stopped && (recovering || networkWait)
        val positionSeconds = tick.positionSeconds
        follow(nowMs, watching, tick)
        if (!watching || !networkGraceElapsed(nowMs)) return LiveStallAction.None
        val stalled = nowMs - anchorAtMs >= STALL_MS
        val behind = tick.latencySeconds > BEHIND_SECONDS
        return when {
            stalled -> recoverStall(nowMs, tick)
            tick.segmentRetry?.deadline == true -> LiveStallAction.None
            behind -> if (settled(nowMs)) step(nowMs, positionSeconds) else LiveStallAction.None
            else -> {
                if (settled(nowMs)) stepsTaken = 0
                LiveStallAction.None
            }
        }
    }

    /** Called from the WebView bridge as well as the main thread. */
    fun networkGraceElapsed(nowMs: Long): Boolean = nowMs >= networkGraceUntilMs

    /** Lets the new network recover the stream before any step, including when Twitch stops the player. */
    fun networkChanged(nowMs: Long) {
        stepsTaken = 0
        lastStepAtMs = null
        recovering = false
        networkWait = true
        networkGraceUntilMs = nowMs + STALL_MS
        anchorAtMs = nowMs
        lastTickAtMs = nowMs
        awaitedSegmentRetryId = null
    }

    /** A new channel: the steps taken for the last one do not count. */
    fun reset() {
        lastTickAtMs = null
        stepsTaken = 0
        lastStepAtMs = null
        recovering = false
        networkWait = false
        networkGraceUntilMs = 0L
        awaitedSegmentRetryId = null
    }

    private fun recoverStall(nowMs: Long, tick: LiveTick): LiveStallAction {
        val restarting = stepsTaken == 0 && !networkWait && !tick.waitingForPlaylistWithLowBuffer
        if (restarting && shouldWaitForRetry(tick)) return LiveStallAction.None
        return step(nowMs, tick.positionSeconds, tick.waitingForPlaylistWithLowBuffer)
    }

    private fun shouldWaitForRetry(tick: LiveTick): Boolean {
        val retry = tick.segmentRetry ?: return false
        val retryId = retry.id ?: return false
        val awaited = awaitedSegmentRetryId
        if (awaited != null) return awaited == retryId
        if (!retry.canWait) return false
        awaitedSegmentRetryId = retryId
        return true
    }

    private fun step(
        nowMs: Long,
        positionSeconds: Double,
        waitingForPlaylistWithLowBuffer: Boolean = false,
    ): LiveStallAction {
        // Keep the regular restart after a network switch. Otherwise a stuck playlist can reuse the same connection
        // on a player restart, so use the first page reload from the existing budget.
        if (stepsTaken == 0 && waitingForPlaylistWithLowBuffer && !networkWait) stepsTaken += 1
        val action = when {
            stepsTaken == 0 -> LiveStallAction.RestartPlayer
            stepsTaken <= PAGE_RELOAD_LIMIT -> LiveStallAction.ReloadPage
            stepsTaken == PAGE_RELOAD_LIMIT + 1 -> LiveStallAction.GiveUp
            else -> return LiveStallAction.None
        }
        stepsTaken += 1
        lastStepAtMs = nowMs
        recovering = action != LiveStallAction.GiveUp
        networkWait = false
        anchor(nowMs, positionSeconds)
        return action
    }

    /**
     * Moves the anchor along with a video that plays, and to where the video is when nothing is known about it. A new
     * timeline, which starts lower, moves the anchor too, but only a video that moves on ends a recovery.
     */
    private fun follow(nowMs: Long, watching: Boolean, tick: LiveTick) {
        val previousTickAtMs = lastTickAtMs
        lastTickAtMs = nowMs
        val positionSeconds = tick.positionSeconds
        val recentProgress = positionSeconds - lastPositionSeconds
        lastPositionSeconds = positionSeconds
        val reportGap = previousTickAtMs == null || nowMs - previousTickAtMs > REPORT_GAP_MS
        // A resumed video can advance less than one second before the next tick. Tiny seeks still count as a stall.
        val movedOn = positionSeconds - anchorSeconds >= PROGRESS_SECONDS ||
            (!reportGap && tick.hasPlaybackBuffer && recentProgress >= RESUMED_PROGRESS_SECONDS)
        if (movedOn) {
            recovering = false
            networkWait = false
        }
        val elsewhere = movedOn || positionSeconds < anchorSeconds
        if (!watching || reportGap || elsewhere) anchor(nowMs, positionSeconds)
    }

    /** No step yet, or the last one is long enough ago to count as done. */
    private fun settled(nowMs: Long): Boolean = lastStepAtMs?.let { nowMs - it >= SETTLE_MS } ?: true

    private fun anchor(nowMs: Long, positionSeconds: Double) {
        anchorSeconds = positionSeconds
        anchorAtMs = nowMs
        awaitedSegmentRetryId = null
    }

    companion object {
        /**
         * How a stall of this report counts: a live stream that has played, that the viewer has not paused, outside an
         * ad break, is watched while Twitch's player means to play it, and after a step whatever state it reports.
         */
        fun watch(live: Boolean, playbackConfirmed: Boolean, userPaused: Boolean, tick: LiveTick): LiveStallWatch =
            when {
                !live || !playbackConfirmed -> LiveStallWatch.Off
                userPaused || tick.adBreak -> LiveStallWatch.Off
                tick.state in WATCHED_STATES -> LiveStallWatch.Playing
                else -> LiveStallWatch.Stopped
            }
    }
}
