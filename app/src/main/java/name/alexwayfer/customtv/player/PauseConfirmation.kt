package name.alexwayfer.customtv.player

/**
 * The page reports playing on every frame, and reports already on their way when the app pauses it still say so.
 * After the app's pause, a playing report is stale until the page has run the pause or reported it; one after that,
 * such as the viewer playing on the page, counts. A stream paused while it was still loading has nothing to pause
 * yet, and Twitch starts it once loaded: that first start is Twitch's own, not the viewer's.
 */
internal class PauseConfirmation {
    private var awaiting = false
    private var pausedWhileLoading = false

    /** The app told the page to pause; [loading]: the stream had not played since the page loaded. */
    fun sent(loading: Boolean) {
        awaiting = true
        pausedWhileLoading = loading
    }

    /** The page ran the pause: its reports from now on are after it. */
    fun ran() {
        awaiting = false
    }

    /** The app told the page to play, or a new page took over: no pause is awaited or held. */
    fun clear() {
        awaiting = false
        pausedWhileLoading = false
    }

    /** Whether a report of [playing] is from before the app's pause; a paused report confirms it. */
    fun stale(playing: Boolean): Boolean {
        if (!playing) awaiting = false
        return playing && awaiting
    }

    /** Whether a report of [playing] is Twitch starting the stream the app paused while it loaded; true once. */
    fun startedOnItsOwn(playing: Boolean): Boolean {
        if (!playing || !pausedWhileLoading) return false
        pausedWhileLoading = false
        return true
    }
}
