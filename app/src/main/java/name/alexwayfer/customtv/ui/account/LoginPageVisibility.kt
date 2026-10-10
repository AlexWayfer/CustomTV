package name.alexwayfer.customtv.ui.account

/**
 * Chrome reports the login tab hidden both when it closes and when the user switches to another app,
 * such as a password manager. The tab counts as closed only once it is hidden and the app itself is in front.
 */
internal class LoginPageVisibility {
    private var shownAt = 0L
    private var loaded = false
    private var hidden = false
    private var closed = false

    fun shown(now: Long) {
        if (shownAt == 0L) shownAt = now
        hidden = false
    }

    fun loaded() {
        loaded = true
    }

    /** Returns true when this hide closes the tab: the app is already in front. */
    fun hidden(now: Long, appInFront: Boolean): Boolean {
        val settled = loaded || (shownAt != 0L && now - shownAt >= SETTLE_MILLIS)
        if (!settled) return false
        hidden = true
        return close(appInFront)
    }

    /** Returns true when the app came to front while the tab is hidden, so the tab was closed. */
    fun appResumed(): Boolean = close(hidden)

    private fun close(tabGone: Boolean): Boolean {
        if (!tabGone || closed) return false
        closed = true
        return true
    }
}

private const val SETTLE_MILLIS = 1_000L
