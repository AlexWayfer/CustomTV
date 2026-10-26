package name.alexwayfer.customtv.player

/**
 * When the player page loads again because the video does not come.
 *
 * The wait starts when the server answers the page and again when the page loads, so a resource
 * that hangs does not hold the page back until its connection times out. A press of the error
 * screen's reload starts the wait too, and the count over. Each delay that passes without video
 * reloads the page once, and the last delay repeats: right after a network switch the page may
 * hang for most of a minute on a network that already works, and only a later load gets through.
 */
internal object PlayerPageReloads {
    private val DELAYS_MS = longArrayOf(10_000, 20_000)
    // A load that failed starts the next one this long after its own start, so one that fails at once does not spin.
    private const val FAILED_RELOAD_SPACING_MS = 10_000L

    /** The wait before the next reload. */
    fun delayMs(attemptsMade: Int): Long = DELAYS_MS[attemptsMade.coerceIn(0, DELAYS_MS.lastIndex)]

    /** The wait before reloading a page whose load failed [sinceLoadMs] after it started. */
    fun failedReloadDelayMs(sinceLoadMs: Long): Long = (FAILED_RELOAD_SPACING_MS - sinceLoadMs).coerceAtLeast(0)

    /**
     * Whether a page whose wait ran out is reloaded. Twitch's player being ready is not enough: after a network
     * switch it may sit ready without video until its own error half a minute later. A page that explains the
     * missing video, [withheld] by an offline channel or the audience gate waiting for a tap, stays.
     */
    fun reloadsWithoutPlayback(played: Boolean, withheld: Boolean, userPaused: Boolean): Boolean =
        !played && !withheld && !userPaused

    /** Whether a wait that ran out after the error screen's reload reloads the page. */
    fun reloadsAfterErrorReload(videoShown: Boolean, userPaused: Boolean): Boolean = !videoShown && !userPaused
}
