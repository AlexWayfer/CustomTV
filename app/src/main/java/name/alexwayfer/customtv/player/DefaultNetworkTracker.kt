package name.alexwayfer.customtv.player

/**
 * Follows the app's default network, a VPN included, and tells when a validated network becomes available.
 */
internal class DefaultNetworkTracker(initial: Long?) {
    private var last = initial
    val available: Boolean
        get() = last != null

    /** Whether the default network just became usable or replaced another usable network. */
    fun report(network: Long, validated: Boolean): Boolean {
        if (!validated) {
            last = null
            return false
        }
        if (network == last) return false
        last = network
        return true
    }

    /** A late loss of the old default does not take the new one offline. */
    fun lost(network: Long): Boolean {
        if (last != network) return false
        last = null
        return true
    }
}

/** Whether a network switch starts watching recovery: a live stream the viewer has not paused. */
internal fun networkSwitchWatchesPlayer(live: Boolean, userPaused: Boolean): Boolean = live && !userPaused
