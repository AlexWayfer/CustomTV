package name.alexwayfer.customtv.links

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import name.alexwayfer.customtv.channel.isValidChannelLogin

/** Where a twitch.tv link leads inside the app. */
internal sealed interface TwitchLinkTarget {
    data class Channel(val login: String) : TwitchLinkTarget

    data class Video(val id: String) : TwitchLinkTarget

    /** A clip by its [slug], which keeps its case: Twitch finds no clip by a lowercased one. */
    data class Clip(val slug: String) : TwitchLinkTarget
}

/**
 * The hosts the manifest asks Android to hand to the app. `m.twitch.tv` stays out: twitch.tv redirects its channel
 * and About pages there, and Chrome would hand that redirect in the app's own Custom Tabs back to the app.
 */
internal val APP_LINK_HOSTS = setOf("twitch.tv", "www.twitch.tv")

/** The host of clip links, `clips.twitch.tv/<slug>`; only the Premium manifest claims it. */
internal const val CLIPS_LINK_HOST = "clips.twitch.tv"

/** The hosts of twitch.tv's web pages, whose paths [twitchLinkTarget] reads. */
private val TWITCH_WEB_HOSTS = APP_LINK_HOSTS + "m.twitch.tv"

/** A page of `clips.twitch.tv` that is not a clip. */
private const val CLIP_EMBED_PATH = "embed"

private val CLIP_SLUG_REGEX = Regex("[A-Za-z0-9_-]+")

/** `/<login>/clip/<slug>` */
private const val CLIP_PATH_PARTS = 3

/** Tabs of a channel's page that the app does not show apart, so the link opens the channel. */
private val CHANNEL_TABS = setOf("about", "schedule", "videos")

/** First path segments of twitch.tv pages that are not channels; Twitch keeps these names out of logins. */
private val RESERVED_PATHS = setOf(
    "activate", "bits", "broadcast", "collections", "dashboard", "directory", "downloads", "drops", "embed", "event",
    "friends", "inventory", "jobs", "login", "logout", "messages", "moderator", "p", "payments", "popout", "prime",
    "products", "search", "settings", "signup", "store", "subs", "subscriptions", "team", "turbo", "user", "videos",
    "wallet",
)

/**
 * What the app opens for a twitch.tv [url]: a channel for `/<login>` and its tabs, a recording for `/videos/<id>`,
 * a clip for `/<login>/clip/<slug>` and `clips.twitch.tv/<slug>`. Null for any other page, such as a category or the
 * settings: those open in the browser. The link filters of [TwitchLinkActivity] in the manifests list the same
 * paths, the clips' only in the Premium one; change both together.
 */
internal fun twitchLinkTarget(url: String): TwitchLinkTarget? {
    val uri = try {
        URI(url.trim())
    } catch (_: URISyntaxException) {
        return null
    }
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "https" && scheme != "http") return null
    val host = uri.host?.lowercase(Locale.ROOT)
    val pathParts = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    if (host == CLIPS_LINK_HOST) return clipsHostTarget(pathParts)
    if (host !in TWITCH_WEB_HOSTS) return null
    val parts = pathParts.map { it.lowercase(Locale.ROOT) }
    val first = parts.firstOrNull() ?: return null
    return when {
        first == "videos" && parts.size == 2 ->
            parts[1].takeIf { id -> id.all { it in '0'..'9' } }?.let(TwitchLinkTarget::Video)
        first in RESERVED_PATHS || !isValidChannelLogin(first) -> null
        parts.size == 1 || (parts.size == 2 && parts[1] in CHANNEL_TABS) -> TwitchLinkTarget.Channel(first)
        parts.size == CLIP_PATH_PARTS && parts[1] == "clip" -> clipTarget(pathParts[2])
        else -> null
    }
}

private fun clipsHostTarget(pathParts: List<String>): TwitchLinkTarget? =
    pathParts.singleOrNull()
        ?.takeIf { !it.equals(CLIP_EMBED_PATH, ignoreCase = true) }
        ?.let(::clipTarget)

private fun clipTarget(slug: String): TwitchLinkTarget? =
    slug.takeIf(CLIP_SLUG_REGEX::matches)?.let(TwitchLinkTarget::Clip)
