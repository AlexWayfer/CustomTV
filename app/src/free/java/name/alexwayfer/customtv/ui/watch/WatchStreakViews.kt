package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.chat.ChannelPrediction
import name.alexwayfer.customtv.chat.ChatMessage

/** Watch streaks are a Premium feature: the free build reports no watched minutes. */
@Composable
internal fun WatchPresenceEffect(channelLogin: String, isLive: Boolean, chatMessages: StateFlow<List<ChatMessage>>) =
    Unit

/** Stream start alerts are a Premium feature: the free build has none to clear. */
@Composable
internal fun WatchedStartAlertEffect(channelId: String?, isLive: Boolean) = Unit

@Composable
internal fun ChannelPointsChip(
    channelLogin: String,
    pointsIconUrl: String?,
    prediction: ChannelPrediction?,
    visible: Boolean,
) = Unit

@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun rememberChatShareOffer(channelLogin: String, pointsIconUrl: String?): ChatShareOffer? = null
