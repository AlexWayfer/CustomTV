package name.alexwayfer.customtv.ui.account

/**
 * A recording the player opens, or with [clipSlug] a clip. A clip's [startedAtMillis] is when its moment went out
 * live, which places its replayed chat, [categoryName] is its category, and [curatorName] who cut it; a recording
 * reads its categories from its chapters.
 */
internal data class ProfileVideoPlayback(
    val id: String,
    val channel: String,
    val title: String,
    val thumbnailUrl: String?,
    val startedAtMillis: Long?,
    val durationSeconds: Long,
    val viewCount: Long,
    val clipSlug: String? = null,
    val categoryName: String? = null,
    val curatorName: String? = null,
)
