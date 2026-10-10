package name.alexwayfer.customtv.ui.home

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.FollowedChannel

/** Watch streaks are a Premium feature: the free build reads none for the followed list. */
@Suppress("unused")
@Composable
internal fun FollowedWatchStreaksEffect(channels: List<FollowedChannel>, fromCache: Boolean) = Unit

@Suppress("unused")
@Composable
internal fun FollowedWatchStreakLine(channelId: String, isLive: Boolean) = Unit
