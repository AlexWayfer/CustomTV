package name.alexwayfer.customtv.player

internal sealed interface TwitchPlaybackTarget {
    val key: String

    data class Channel(val login: String) : TwitchPlaybackTarget {
        override val key: String = "channel:${login.lowercase()}"
    }

    /** A recording, with what its media session shows: [title] is already the one on screen, never blank. */
    data class Video(
        val id: String,
        val channel: String,
        val title: String,
        val thumbnailUrl: String?,
        val durationSeconds: Long,
    ) : TwitchPlaybackTarget {
        override val key: String = "video:${id.removePrefix("v")}"
    }
}
