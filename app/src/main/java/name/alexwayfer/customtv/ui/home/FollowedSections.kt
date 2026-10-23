package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.FollowedChannel

/** The live channels, the live ones in a hidden category, which wait under a line of their own, and the offline. */
internal data class FollowedSections(
    val live: List<FollowedChannel>,
    val offline: List<FollowedChannel>,
    val hidden: List<FollowedChannel>,
)
