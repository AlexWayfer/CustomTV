package name.alexwayfer.customtv.ui.home

internal enum class HomeChannelTarget {
    Stream,
    Profile,
}

/**
 * A channel picked on Home — from the follows, the recents, a search suggestion, or a typed name —
 * opens the stream when it is live and its profile when it is offline. The profile's Chat tab
 * still opens the stream.
 */
internal fun homeChannelTarget(live: Boolean): HomeChannelTarget =
    if (live) HomeChannelTarget.Stream else HomeChannelTarget.Profile
