package name.alexwayfer.customtv.player

import android.net.Uri
import androidx.core.net.toUri

internal fun playerEmbedUrl(target: TwitchPlaybackTarget): String = when (target) {
    is TwitchPlaybackTarget.Channel ->
        "https://player.twitch.tv/?channel=${target.login}&parent=player.twitch.tv&autoplay=true&muted=false"
    // Clips have their own embed page; player.twitch.tv takes no clip parameter.
    is TwitchPlaybackTarget.Video -> if (target.clipSlug != null) {
        "https://clips.twitch.tv/embed?clip=${target.clipSlug}&parent=player.twitch.tv&autoplay=true&muted=false"
    } else {
        "https://player.twitch.tv/?video=v${target.id.removePrefix("v")}" +
            "&parent=player.twitch.tv&autoplay=true&muted=false"
    }
}

/** The hosts of Twitch's embedded players: streams and recordings, and clips. */
internal fun isPlayerHost(host: String): Boolean {
    val lower = host.lowercase()
    return lower == "player.twitch.tv" || lower.endsWith(".player.twitch.tv") || lower == "clips.twitch.tv"
}

internal fun isPlayerDocumentUrl(url: String): Boolean {
    if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return false
    val host = url.toUri().host ?: return url.contains("player.twitch.tv") || url.contains("clips.twitch.tv")
    return isPlayerHost(host)
}

internal fun shouldBlockPlayerNavigation(uri: Uri, isMainFrame: Boolean): Boolean {
    val url = uri.toString()
    if (url.startsWith("about:") || url.startsWith("data:") || url.startsWith("javascript:")) {
        return false
    }
    val host = uri.host?.lowercase() ?: return isMainFrame
    if (isPlayerHost(host)) return false
    if (isMainFrame) return true
    if (host == "www.twitch.tv" || host == "twitch.tv" || host == "m.twitch.tv") {
        val path = uri.path.orEmpty()
        return !(path.startsWith("/embed") || path.startsWith("/player"))
    }
    return false
}
