package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.RecentChatLoadState
import name.alexwayfer.customtv.ui.LoadingBar
import name.alexwayfer.customtv.ui.rememberLoadingBarState

/** The bar over the chat's top while the messages from before the open load. */
@Composable
internal fun RecentChatLoadingIndicator(state: RecentChatLoadState) {
    LoadingBar(rememberLoadingBarState(state == RecentChatLoadState.Loading))
}
