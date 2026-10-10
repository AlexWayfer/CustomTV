package name.alexwayfer.customtv.player

/**
 * Moves the player's own controls along with the app's chrome. A tap only reaches the player once
 * it rests at its size (full, mini, or picture-in-picture), so a request made during a drag or an
 * animation waits for that.
 */
class PlayerChrome {
    /** Shows or hides the player's own controls, tapping the player only when they differ. */
    var matchControls: ((visible: Boolean) -> Unit)? = null

    private var settled = false
    private var pending: Boolean? = null

    fun request(visible: Boolean) {
        if (settled) {
            matchControls?.invoke(visible)
        } else {
            pending = visible
        }
    }

    /** The player rests at its size: applies the request that waited for it. */
    fun settle() {
        settled = true
        val visible = pending ?: return
        pending = null
        matchControls?.invoke(visible)
    }

    /** The player minimizes, moves, or animates: requests wait until it settles again. */
    fun unsettle() {
        settled = false
    }

    /** Hides the controls at once, without waiting to settle, as the player starts into the corner. */
    fun hideNow() {
        pending = null
        matchControls?.invoke(false)
    }
}
