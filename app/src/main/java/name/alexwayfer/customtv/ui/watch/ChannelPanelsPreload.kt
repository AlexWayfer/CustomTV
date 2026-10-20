package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import name.alexwayfer.customtv.data.ChannelPanelsRepository

/**
 * Loads the channel's About panels when its stream opens, unless the run already holds them, so the chat menu
 * opens with its rows taken from the panels already in place instead of expanding them in.
 */
@Composable
internal fun ChannelPanelsPreloadEffect(channelId: String?) {
    LaunchedEffect(channelId) {
        if (channelId != null && ChannelPanelsRepository.cached(channelId) == null) {
            ChannelPanelsRepository.load(channelId)
        }
    }
}
