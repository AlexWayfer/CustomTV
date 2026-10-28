package name.alexwayfer.customtv.player

import name.alexwayfer.customtv.data.HTTP_FORBIDDEN
import name.alexwayfer.customtv.data.HTTP_NOT_FOUND
import name.alexwayfer.customtv.data.HTTP_SUCCESS

/** The playlist's answers for a subscriber-only or ended stream, which the player shows itself. */
internal val PLAYER_SHOWN_PLAYLIST_CODES = setOf(HTTP_FORBIDDEN, HTTP_NOT_FOUND)

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
     * Why a page whose wait ran out stays, or null when it is reloaded. Twitch's player being ready is not enough:
     * after a network switch it may sit ready without video until its own error half a minute later. A page that
     * explains the missing video, [withheld] by an offline channel or the audience gate waiting for a tap, stays.
     * So does one the viewer paused with the app's own control, [explicitlyPaused]. A pause the app reads from the
     * page is not one: the page stays hidden until the first frame, so its own pause is out of the viewer's reach,
     * and Twitch's player reports one while it waits for a network that hangs.
     */
    fun noPlaybackReloadSkip(played: Boolean, withheld: Boolean, explicitlyPaused: Boolean): String? = when {
        played -> "it has played"
        withheld -> "the page explains the missing video"
        explicitlyPaused -> "the viewer paused it"
        else -> null
    }

    /**
     * Whether a page without playback whose wait ran out waits once more instead of reloading. A page whose player
     * already has its playlist is on its way: right after a network switch every connection is new and the start
     * takes longer, and a reload then only starts over a moment before the first frame.
     */
    fun waitsOnceMore(playlistLoaded: Boolean, waitedOnceMore: Boolean): Boolean = playlistLoaded && !waitedOnceMore

    /**
     * Whether the playlist's answer with [code] counts as loaded. An offline or subscriber-only answer counts too: the
     * player shows that itself a moment later, and a reload before it only starts the page over.
     */
    fun playlistAnswerCounts(code: Int): Boolean = code in HTTP_SUCCESS || code in PLAYER_SHOWN_PLAYLIST_CODES

    /** The wait once more: as long as the next reload's. */
    fun onceMoreDelayMs(attemptsMade: Int): Long = delayMs(attemptsMade + 1)

    /** Whether a wait that ran out after the error screen's reload reloads the page. */
    fun reloadsAfterErrorReload(videoShown: Boolean, userPaused: Boolean): Boolean = !videoShown && !userPaused
}
