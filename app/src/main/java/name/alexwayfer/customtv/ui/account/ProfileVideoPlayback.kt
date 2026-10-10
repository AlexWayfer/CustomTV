package name.alexwayfer.customtv.ui.account

internal data class ProfileVideoPlayback(
    val id: String,
    val channel: String,
    val title: String,
    val thumbnailUrl: String?,
    val startedAtMillis: Long?,
    val durationSeconds: Long,
    val viewCount: Long,
)
