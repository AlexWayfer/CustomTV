package name.alexwayfer.customtv.player

/**
 * Tells which WebView page callbacks belong to the current player load.
 *
 * A load that a newer `loadUrl` aborts still reports `onPageFinished`, so the next finish
 * after such a restart belongs to the old page. A failed load reports its error page's
 * finish as well, and a loaded page may report a second finish.
 */
internal class PlayerPageLoad {
    private var inFlight = false
    private var staleFinishes = 0
    private var responded = false
    private var loaded = false
    private var failed = false

    /** The current load is still waiting for the server to answer the page request. */
    val awaitingResponse: Boolean
        get() = inFlight && !responded && !loaded && !failed

    fun start() {
        if (inFlight) staleFinishes += 1
        inFlight = true
        responded = false
        loaded = false
        failed = false
    }

    /** The server answered and the page started; a later slow download is not a missing answer. */
    fun respond() {
        if (inFlight && !failed) responded = true
    }

    /** True only for the first finish of the current load when it did not fail. */
    fun finish(): Boolean {
        if (staleFinishes > 0) {
            staleFinishes -= 1
            return false
        }
        inFlight = false
        if (loaded || failed) return false
        loaded = true
        return true
    }

    /** True only for the first failure of the current load before it finished. */
    fun fail(): Boolean {
        if (!inFlight || loaded || failed) return false
        failed = true
        return true
    }
}
