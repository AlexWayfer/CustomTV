package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.FollowedChannel

internal data class FollowedSections(
    val live: List<FollowedChannel>,
    val offline: List<FollowedChannel>,
)
