package name.alexwayfer.customtv.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import name.alexwayfer.customtv.diagnostics.AppLog

/** Gives live stall recovery a new wait when another working network becomes the app's default. */
internal class PlayerNetworkWatch(
    appContext: Context,
    private val handler: Handler,
    private val isLive: () -> Boolean,
    private val onNetworkChanged: () -> Unit,
) {
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun register() {
        val active = connectivity.activeNetwork
        val activeValidated = active?.let { connectivity.getNetworkCapabilities(it) }?.validated() == true
        val tracker = DefaultNetworkTracker(initial = active?.networkHandle?.takeIf { activeValidated })
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (tracker.report(network.networkHandle, capabilities.validated())) switched()
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
        if (callback == null || !networkSwitchWatchesPlayer(isLive(), StreamPlayback.userPaused)) return
        AppLog.i(PLAYER_LOG_TAG, "Network switched, waiting for playback to recover")
        onNetworkChanged()
    }

    private fun NetworkCapabilities.validated() = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
