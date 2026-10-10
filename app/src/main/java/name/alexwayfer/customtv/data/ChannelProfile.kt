package name.alexwayfer.customtv.data

data class ChannelProfile(
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val id: String? = null,
    val streamTitle: String? = null,
    val categoryName: String? = null,
    val streamStartedAtMillis: Long? = null,
    val currentStreamVideoId: String? = null,
    val viewerCount: Int? = null,
    val sharedViewerCount: Int? = null,
    val collaborationCount: Int? = null,
    /** Avatars of the other channels in the collaboration, in Twitch order. */
    val collaboratorAvatarUrls: List<String> = emptyList(),
    val isLive: Boolean = false,
    val highlightColorHex: String? = null,
    val primaryColorHex: String? = null,
    val highlightRewardCost: Int? = null,
    val channelPointsIconUrl: String? = null,
    val tags: List<String> = emptyList(),
    /** Null until the channel's own profile request brings them; not saved with the profile. */
    val chatRules: List<String>? = null,
) {
    companion object {
        const val DEFAULT_HIGHLIGHT_COST = 100
    }
}
