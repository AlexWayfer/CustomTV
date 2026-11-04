package name.alexwayfer.customtv.player

/**
 * Follows the app's default network, a VPN included, and tells when a working one replaces the last, so recovery
 * can give Twitch's player time to reconnect before restarting it.
 */
internal class DefaultNetworkTracker(initial: Long?) {
    private var last = initial

    /** The default network [network] is reported; whether it is a validated one other than the last. */
    fun report(network: Long, validated: Boolean): Boolean {
        if (!validated || network == last) return false
        last = network
        return true
    }
}

/** Whether a network switch starts watching recovery: a live stream the viewer has not paused. */
internal fun networkSwitchWatchesPlayer(live: Boolean, userPaused: Boolean): Boolean = live && !userPaused
