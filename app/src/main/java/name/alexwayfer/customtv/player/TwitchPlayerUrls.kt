package name.alexwayfer.customtv.player

import android.net.Uri
import androidx.core.net.toUri

internal fun playerEmbedUrl(target: TwitchPlaybackTarget): String = when (target) {
    is TwitchPlaybackTarget.Channel ->
        "https://player.twitch.tv/?channel=${target.login}&parent=player.twitch.tv&autoplay=true&muted=false"
    is TwitchPlaybackTarget.Video ->
        "https://player.twitch.tv/?video=v${target.id.removePrefix("v")}" +
            "&parent=player.twitch.tv&autoplay=true&muted=false"
}

internal fun isPlayerDocumentUrl(url: String): Boolean {
    if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return false
    val host = url.toUri().host?.lowercase() ?: return url.contains("player.twitch.tv")
    return host == "player.twitch.tv" || host.endsWith(".player.twitch.tv")
}

internal fun shouldBlockPlayerNavigation(uri: Uri, isMainFrame: Boolean): Boolean {
    val url = uri.toString()
    if (url.startsWith("about:") || url.startsWith("data:") || url.startsWith("javascript:")) {
        return false
    }
    val host = uri.host?.lowercase() ?: return isMainFrame
    if (host == "player.twitch.tv" || host.endsWith(".player.twitch.tv")) return false
    if (isMainFrame) return true
    if (host == "www.twitch.tv" || host == "twitch.tv" || host == "m.twitch.tv") {
        val path = uri.path.orEmpty()
        return !(path.startsWith("/embed") || path.startsWith("/player"))
    }
    return false
}
