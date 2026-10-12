package name.alexwayfer.customtv.data

internal data class CollaborationChannel(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val viewerCount: Int?,
)
