package name.alexwayfer.customtv.player

/** An offline page cannot start a new stream out of sight; it reloads when the viewer returns. */
internal fun suspendOfflinePlayerPage(background: Boolean, offline: Boolean, suspended: Boolean = false): Boolean =
    background && (offline || suspended)

/** Keeps suspension separate from the channel's status, which may change while its page is unloaded. */
internal class OfflinePlayerPage(private val unload: () -> Unit, private val reload: () -> Unit) {
    var suspended: Boolean = false
        private set
    private var background = false

    fun update(background: Boolean = this.background, offline: Boolean) {
        this.background = background
        val next = suspendOfflinePlayerPage(background, offline, suspended)
        if (next == suspended) return
        suspended = next
        if (next) unload() else reload()
    }
}
