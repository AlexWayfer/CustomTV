package name.alexwayfer.customtv.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import name.alexwayfer.customtv.diagnostics.AppLog

/** Tracks the default network's availability and gives live playback time to recover when it changes. */
internal class PlayerNetworkWatch(
    appContext: Context,
    private val handler: Handler,
    private val isLive: () -> Boolean,
    private val onNetworkChanged: () -> Unit,
    private val onNetworkAvailabilityChanged: (Boolean) -> Unit,
) {
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun register() {
        val active = connectivity.activeNetwork
        val activeValidated = active?.let { connectivity.getNetworkCapabilities(it) }?.validated() == true
        onNetworkAvailabilityChanged(activeValidated)
        val tracker = DefaultNetworkTracker(initial = active?.networkHandle?.takeIf { activeValidated })
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (callback !== this) return
                val wasAvailable = tracker.available
                if (tracker.report(network.networkHandle, capabilities.validated())) switched()
                if (tracker.available != wasAvailable) onNetworkAvailabilityChanged(tracker.available)
            }

            override fun onLost(network: Network) {
                if (callback === this && tracker.lost(network.networkHandle)) onNetworkAvailabilityChanged(false)
            }
        }
        connectivity.registerDefaultNetworkCallback(networkCallback, handler)
        callback = networkCallback
    }

    fun release() {
        val networkCallback = callback ?: return
        callback = null
        connectivity.unregisterNetworkCallback(networkCallback)
    }

    private fun switched() {
        if (!networkSwitchWatchesPlayer(isLive(), StreamPlayback.userPaused)) return
        AppLog.i(PLAYER_LOG_TAG, "Network switched, waiting for playback to recover")
        onNetworkChanged()
    }

    private fun NetworkCapabilities.validated() = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
