package name.alexwayfer.customtv.data

/**
 * A Twitch category, such as a game, by the name it shows. [id] opens it, when the answer brought one; [viewerCount] is
 * how many watch it live, and [tags] are Twitch's tags for it, such as RPG, where those are known.
 */
internal data class TwitchCategory(
    val name: String,
    val boxArtUrl: String?,
    val id: String? = null,
    val viewerCount: Int? = null,
    val tags: List<CategoryTag> = emptyList(),
)

/** One of Twitch's tags for categories, by the [id] that filters the categories and the [name] it shows. */
internal data class CategoryTag(val id: String, val name: String)
