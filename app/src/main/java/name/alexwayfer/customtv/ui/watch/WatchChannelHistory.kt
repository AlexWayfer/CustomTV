package name.alexwayfer.customtv.ui.watch

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import name.alexwayfer.customtv.ui.components.ChannelPreview

internal const val WATCH_CHANNEL_ROUTE = "watch/{channel}?name={name}&avatar={avatar}"

/** The arguments of [WATCH_CHANNEL_ROUTE]: the login, and the name and avatar that [watchRoute] may leave out. */
internal val watchChannelArguments: List<NamedNavArgument> = listOf(
    navArgument("channel") { type = NavType.StringType },
    navArgument("name") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    },
    navArgument("avatar") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    },
)

/** The route to [channel]'s stream, with the name and avatar the opener shows, which the stream shows until loaded. */
internal fun watchRoute(channel: ChannelPreview): String {
    val query = listOfNotNull(
        channel.displayName?.takeIf { it.isNotBlank() }?.let { "name=" + Uri.encode(it) },
        channel.avatarUrl?.takeIf { it.isNotBlank() }?.let { "avatar=" + Uri.encode(it) },
    )
    val route = "watch/" + Uri.encode(channel.login)
    return if (query.isEmpty()) route else query.joinToString("&", prefix = "$route?")
}

/** The stream's channel as [watchRoute] passed it. */
internal fun watchChannelPreview(arguments: Bundle?): ChannelPreview = ChannelPreview(
    login = arguments?.getString("channel").orEmpty(),
    displayName = arguments?.getString("name"),
    avatarUrl = arguments?.getString("avatar"),
)

internal fun watchBackReturnsToPreviousChannel(previousRoute: String?): Boolean =
    previousRoute == WATCH_CHANNEL_ROUTE

/** Closing the stream closes every channel it led to, such as a raid's target, not only the one on screen. */
internal fun NavController.closeWatchedChannels() {
    popBackStack("home", inclusive = false)
}
