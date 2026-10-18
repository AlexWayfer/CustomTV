package name.alexwayfer.customtv.player

internal sealed interface TwitchPlaybackTarget {
    val key: String

    data class Channel(val login: String) : TwitchPlaybackTarget {
        override val key: String = "channel:${login.lowercase()}"
    }

    /**
     * A recording, or with [clipSlug] a clip, with what its media session shows: [title] is already the one on
     * screen, never blank. Both play from start to end and seek alike; only the page that plays them differs.
     */
    data class Video(
        val id: String,
        val channel: String,
        val title: String,
        val thumbnailUrl: String?,
        val durationSeconds: Long,
        val clipSlug: String? = null,
    ) : TwitchPlaybackTarget {
        override val key: String = clipSlug?.let { "clip:$it" } ?: "video:${id.removePrefix("v")}"
    }
}
