package name.alexwayfer.customtv.player

import android.os.Handler

/**
 * A pause the app infers when the page says the player stopped though the user did not pause it. It counts only once
 * the player stays stopped past a grace for rebuffering, and not at all while [suppressed].
 */
internal class InferredPause(private val handler: Handler) {
    @Volatile
    var suppressed: Boolean = false
        private set
    private var pending: Runnable? = null

    val isPending: Boolean
        get() = pending != null

    fun suppress(suppress: Boolean) {
        suppressed = suppress
        if (suppress) cancel()
    }

    fun schedule(delayMillis: Long, onPause: () -> Unit) {
        val task = Runnable {
            pending = null
            onPause()
        }
        pending = task
        handler.postDelayed(task, delayMillis)
    }

    fun cancel() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }
}
