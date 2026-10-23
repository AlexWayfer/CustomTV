package name.alexwayfer.customtv.ui.watch

import androidx.navigation.NavController

internal const val WATCH_CHANNEL_ROUTE = "watch/{channel}"

internal fun watchBackReturnsToPreviousChannel(previousRoute: String?): Boolean =
    previousRoute == WATCH_CHANNEL_ROUTE

/** Closing the stream closes every channel it led to, such as a raid's target, not only the one on screen. */
internal fun NavController.closeWatchedChannels() {
    popBackStack("home", inclusive = false)
}
