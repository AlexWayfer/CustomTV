package name.alexwayfer.customtv.ui.account

import name.alexwayfer.customtv.data.ChannelProfile

internal sealed interface ProfileHomePrimary {
    data object Live : ProfileHomePrimary
    data class PastBroadcast(val video: TwitchVideo) : ProfileHomePrimary
    data class Suggested(val channel: ChannelProfile) : ProfileHomePrimary
    data object None : ProfileHomePrimary
}

/**
 * A live stream comes first; otherwise the recording the build offers; otherwise the first
 * suggested channel that is live. Offline suggestions show nothing.
 */
internal fun profileHomePrimary(
    profile: ChannelProfile,
    pastBroadcast: TwitchVideo?,
    suggestedChannels: List<ChannelProfile>,
): ProfileHomePrimary {
    val suggested = suggestedChannels.firstOrNull { it.isLive }
    return when {
        profile.isLive -> ProfileHomePrimary.Live
        pastBroadcast != null -> ProfileHomePrimary.PastBroadcast(pastBroadcast)
        suggested != null -> ProfileHomePrimary.Suggested(suggested)
        else -> ProfileHomePrimary.None
    }
}
