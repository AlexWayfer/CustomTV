package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import name.alexwayfer.customtv.chat.ChannelPrediction

/** Ending or deleting a prediction needs the experimental login, a Premium feature: the free card has no menu. */
@Composable
internal fun PredictionModMenu(prediction: ChannelPrediction, channelId: String?, visible: Boolean, iconSize: Dp) = Unit
