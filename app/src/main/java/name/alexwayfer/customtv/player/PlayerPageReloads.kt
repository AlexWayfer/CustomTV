package name.alexwayfer.customtv.player

/**
 * When the player page loads again because the video does not come.
 *
 * The wait starts when the server answers the page and again when the page loads, so a resource
 * that hangs does not hold the page back until its connection times out. A press of the error
 * screen's reload starts the wait too, and the count over. Each delay that passes without video
 * reloads the page once; after the last one the player stays as it is.
 */
internal object PlayerPageReloads {
    private val DELAYS_MS = longArrayOf(10_000, 20_000)

    val limit: Int get() = DELAYS_MS.size

    /** The wait before the next reload, or null when no reloads are left. */
    fun delayMs(attemptsMade: Int): Long? = DELAYS_MS.getOrNull(attemptsMade)

    /** Whether a wait that ran out after the error screen's reload reloads the page. */
    fun reloadsAfterErrorReload(videoShown: Boolean, userPaused: Boolean): Boolean = !videoShown && !userPaused
}
