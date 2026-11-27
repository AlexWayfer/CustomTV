package name.alexwayfer.customtv.player

internal enum class PauseReport { Accept, Ignore, Retry }

/**
 * The page reports playing on every frame, and reports already on their way when the app pauses it still say so.
 * After the app's pause, a playing report is stale until the page has run the pause or reported it; one after that,
 * such as the viewer playing on the page, counts. A stream paused while it was still loading has nothing to pause
 * yet, and Twitch starts it once loaded: that first start is Twitch's own, not the viewer's. In chat-only mode only
 * an app play command releases the pause, and a new spell of playback gets one more pause request.
 */
internal class PauseConfirmation {
    private var awaiting = false
    private var pausedWhileLoading = false
    private var holdUntilPlay = false
    private var retriedWhileHeld = false

    /** The app told the page to pause; [loading]: the stream had not played since the page loaded. */
    fun sent(loading: Boolean, holdUntilPlay: Boolean = false) {
        awaiting = true
        pausedWhileLoading = loading
        this.holdUntilPlay = this.holdUntilPlay || holdUntilPlay
        retriedWhileHeld = false
    }

    /** The page ran the pause: its reports from now on are after it. */
    fun ran() {
        awaiting = false
    }

    /** The app told the page to play, or a new page took over: no pause is awaited or held. */
    fun clear() {
        awaiting = false
        pausedWhileLoading = false
        holdUntilPlay = false
        retriedWhileHeld = false
    }

    fun report(playing: Boolean): PauseReport {
        if (!playing) {
            awaiting = false
            retriedWhileHeld = false
            return PauseReport.Accept
        }
        return when {
            awaiting || retriedWhileHeld -> PauseReport.Ignore
            holdUntilPlay || pausedWhileLoading -> {
                awaiting = true
                pausedWhileLoading = false
                retriedWhileHeld = holdUntilPlay
                PauseReport.Retry
            }
            else -> PauseReport.Accept
        }
    }
}
