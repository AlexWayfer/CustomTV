package name.alexwayfer.customtv.player

/**
 * Follows the app's default network, a VPN included, and tells when a working one replaces the last: the
 * connections Twitch's player had open die with the old network, and its own retries hang for most of a minute.
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

/** Whether a switch of the network reloads the player page: a live stream the viewer has not paused. */
internal fun networkSwitchReloadsPlayer(live: Boolean, userPaused: Boolean): Boolean = live && !userPaused
