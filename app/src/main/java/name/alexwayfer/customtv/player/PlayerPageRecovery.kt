package name.alexwayfer.customtv.player

import android.os.Handler
import android.os.SystemClock
import name.alexwayfer.customtv.diagnostics.AppLog

private val PLAYER_RELOAD_DELAYS_MS = longArrayOf(1_000, 4_000, 12_000)
private const val PAGE_RESPONSE_TIMEOUT_MS = 10_000L

/**
 * Brings a stuck player page back: a page that does not answer or does not start, Twitch's player error, and the
 * error screen's own reload. Each kind of retry counts its attempts until the next load or playback.
 */
internal class PlayerPageRecovery(
    private val handler: Handler,
    private val isReleased: () -> Boolean,
    isSurfaceRevealed: () -> Boolean,
    reloadPlayer: () -> Unit,
    private val reloadPage: () -> Unit,
    private val concealSurface: () -> Unit,
) {
    val pageLoad = PlayerPageLoad()
    /** The loaded channel or recording has played. */
    var playbackConfirmed = false
        private set
    private var ready = false
    private var loadStartedAt = 0L
    private var automaticReloadAttempts = 0
    private var playerReloadPending = false
    private var startupReloadAttempts = 0
    private val playerReload = Runnable {
        playerReloadPending = false
        if (isReleased() || StreamPlayback.isExplicitlyPaused) return@Runnable
        reloadPlayer()
    }
    private val pageResponseWatchdog = Runnable {
        if (pageLoad.awaitingResponse) pageFailed("no response")
    }
    private val startupWatchdog = Runnable {
        val started = playbackConfirmed || ready
        if (isReleased() || StreamPlayback.userPaused || started) return@Runnable
        startupReloadAttempts += 1
        AppLog.w(
            PLAYER_LOG_TAG,
            "Not ready after $sinceLoadMs ms, reloading the page (attempt $startupReloadAttempts)",
        )
        reloadPage()
    }
    private val errorReloadWatchdog = Runnable {
        if (isReleased()) return@Runnable
        if (!PlayerPageReloads.reloadsAfterErrorReload(isSurfaceRevealed(), StreamPlayback.userPaused)) {
            return@Runnable
        }
        startupReloadAttempts += 1
        AppLog.w(
            PLAYER_LOG_TAG,
            "No video after the error screen's reload, reloading the page (attempt $startupReloadAttempts)",
        )
        reloadPage()
    }

    /** A page starts loading; a reload of the same target keeps counting its startup attempts. */
    fun start(resetStartupReloads: Boolean) {
        playbackConfirmed = false
        ready = false
        if (resetStartupReloads) startupReloadAttempts = 0
        loadStartedAt = SystemClock.elapsedRealtime()
        resetAutomaticReloads()
        handler.removeCallbacks(startupWatchdog)
        handler.removeCallbacks(errorReloadWatchdog)
        pageLoad.start()
        handler.removeCallbacks(pageResponseWatchdog)
        handler.postDelayed(pageResponseWatchdog, PAGE_RESPONSE_TIMEOUT_MS)
    }

    val sinceLoadMs: Long
        get() = SystemClock.elapsedRealtime() - loadStartedAt

    fun resetAutomaticReloads() {
        automaticReloadAttempts = 0
        playerReloadPending = false
        handler.removeCallbacks(playerReload)
    }

    /** The video shows, so the error screen's reload worked. */
    fun cancelErrorReload() {
        handler.removeCallbacks(errorReloadWatchdog)
    }

    /** Whether this playing report is the first since the load, which it then records. */
    fun confirmPlayback(): Boolean {
        if (playbackConfirmed) return false
        playbackConfirmed = true
        AppLog.i(PLAYER_LOG_TAG, "Playing after $sinceLoadMs ms")
        return true
    }

    /** The server answered the page; a resource that hangs from here on must not keep the player from starting. */
    fun pageResponded() {
        if (pageLoad.respond() && !ready && !playbackConfirmed) scheduleStartupWatchdog()
    }

    fun pageLoaded() {
        if (!pageLoad.finish()) return
        AppLog.i(PLAYER_LOG_TAG, "Page loaded after $sinceLoadMs ms")
        if (!ready && !playbackConfirmed) scheduleStartupWatchdog()
    }

    fun pageFailed(reason: String) {
        if (isReleased() || !pageLoad.fail()) return
        AppLog.w(PLAYER_LOG_TAG, "Page failed after $sinceLoadMs ms: $reason")
        if (StreamPlayback.userPaused) return
        if (startupReloadAttempts >= PlayerPageReloads.limit) {
            AppLog.w(PLAYER_LOG_TAG, "No page reloads left")
            return
        }
        startupReloadAttempts += 1
        AppLog.i(PLAYER_LOG_TAG, "Reloading the page (attempt $startupReloadAttempts)")
        reloadPage()
    }

    fun playerReady() {
        if (!ready) AppLog.i(PLAYER_LOG_TAG, "Ready after $sinceLoadMs ms")
        ready = true
        handler.removeCallbacks(startupWatchdog)
    }

    fun playerError() {
        if (isReleased() || StreamPlayback.isExplicitlyPaused || playerReloadPending) return
        val delay = PLAYER_RELOAD_DELAYS_MS.getOrNull(automaticReloadAttempts)
        if (delay == null) {
            AppLog.w(PLAYER_LOG_TAG, "Player error, no automatic reloads left")
            return
        }
        concealSurface()
        automaticReloadAttempts += 1
        playerReloadPending = true
        AppLog.w(PLAYER_LOG_TAG, "Player error, reload $automaticReloadAttempts in $delay ms")
        handler.postDelayed(playerReload, delay)
    }

    fun errorScreenReload() {
        if (isReleased()) return
        AppLog.i(PLAYER_LOG_TAG, "Error screen reload pressed")
        concealSurface()
        // A player that keeps loading without a network shows no error again: reload the page as at start.
        startupReloadAttempts = 0
        handler.removeCallbacks(errorReloadWatchdog)
        PlayerPageReloads.delayMs(startupReloadAttempts)?.let { handler.postDelayed(errorReloadWatchdog, it) }
    }

    private fun scheduleStartupWatchdog() {
        handler.removeCallbacks(startupWatchdog)
        val delay = PlayerPageReloads.delayMs(startupReloadAttempts) ?: return
        handler.postDelayed(startupWatchdog, delay)
    }
}
