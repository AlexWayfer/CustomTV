package name.alexwayfer.customtv.player

// The video moves less than this in a stall: Twitch's player nudges a stuck position by a tenth of a second.
private const val PROGRESS_SECONDS = 1.0
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
)

internal enum class LiveStallAction { None, RestartPlayer, ReloadPage, GiveUp }

/**
 * Brings back a live stream that Twitch's player keeps reporting as playing while its video stands still or falls
 * far behind the broadcast: it stalls on a gap in its buffer and never reports an error. The first step restarts
 * the player, the next ones reload the page, and then it gives up until the stream plays on again.
 */
internal class LiveStallWatchdog {
    private var anchorSeconds = 0.0
    private var anchorAtMs = 0L
    private var lastTickAtMs: Long? = null
    private var stepsTaken = 0
    private var lastStepAtMs: Long? = null

    fun tick(nowMs: Long, watching: Boolean, positionSeconds: Double, latencySeconds: Double): LiveStallAction {
        follow(nowMs, watching, positionSeconds)
        if (!watching) return LiveStallAction.None
        val stalled = nowMs - anchorAtMs >= STALL_MS
        val behind = latencySeconds > BEHIND_SECONDS
        return when {
            stalled -> step(nowMs, positionSeconds)
            behind -> if (settled(nowMs)) step(nowMs, positionSeconds) else LiveStallAction.None
            else -> {
                if (settled(nowMs)) stepsTaken = 0
                LiveStallAction.None
            }
        }
    }

    /** A new channel: the steps taken for the last one do not count. */
    fun reset() {
        lastTickAtMs = null
        stepsTaken = 0
        lastStepAtMs = null
    }

    private fun step(nowMs: Long, positionSeconds: Double): LiveStallAction {
        val action = when {
            stepsTaken == 0 -> LiveStallAction.RestartPlayer
            stepsTaken <= PAGE_RELOAD_LIMIT -> LiveStallAction.ReloadPage
            stepsTaken == PAGE_RELOAD_LIMIT + 1 -> LiveStallAction.GiveUp
            else -> return LiveStallAction.None
        }
        stepsTaken += 1
        lastStepAtMs = nowMs
        anchor(nowMs, positionSeconds)
        return action
    }

    /** Moves the anchor along with a video that plays, and to where the video is when nothing is known about it. */
    private fun follow(nowMs: Long, watching: Boolean, positionSeconds: Double) {
        val previousTickAtMs = lastTickAtMs
        lastTickAtMs = nowMs
        val reportGap = previousTickAtMs == null || nowMs - previousTickAtMs > REPORT_GAP_MS
        val progressed = positionSeconds < anchorSeconds || positionSeconds - anchorSeconds >= PROGRESS_SECONDS
        if (!watching || reportGap || progressed) anchor(nowMs, positionSeconds)
    }

    /** No step yet, or the last one is long enough ago to count as done. */
    private fun settled(nowMs: Long): Boolean = lastStepAtMs?.let { nowMs - it >= SETTLE_MS } ?: true

    private fun anchor(nowMs: Long, positionSeconds: Double) {
        anchorSeconds = positionSeconds
        anchorAtMs = nowMs
    }

    companion object {
        /**
         * Whether a stall of this report counts: a live stream that has played, that the viewer has not paused,
         * outside an ad break, while Twitch's player means to play it.
         */
        fun watches(live: Boolean, playbackConfirmed: Boolean, userPaused: Boolean, tick: LiveTick): Boolean =
            live && playbackConfirmed && !userPaused && !tick.adBreak && tick.state in WATCHED_STATES
    }
}
