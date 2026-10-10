package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChannelPrediction

/** Watch streaks are a Premium feature: the free build reports no watched minutes. */
@Suppress("unused")
@Composable
internal fun WatchPresenceEffect(channelLogin: String, isLive: Boolean) = Unit

@Suppress("unused")
@Composable
internal fun ChannelPointsChip(channelLogin: String, pointsIconUrl: String?, prediction: ChannelPrediction?, visible: Boolean) = Unit

@Suppress("unused")
@Composable
internal fun rememberChatShareOffer(channelLogin: String, pointsIconUrl: String?): ChatShareOffer? = null
