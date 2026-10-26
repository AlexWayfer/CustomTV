package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/** The stream's sheet that opened the settings, which Back from them reopens over the expanded stream. */
internal sealed interface PlayerSheetReturn {
    data object ChatRecap : PlayerSheetReturn

    /** The chatter's card, on its portrait page. */
    data class ChatterPortrait(val card: ChatterCardRequest) : PlayerSheetReturn
}

/** Hands the sheet to reopen to the chat, which opens it once the expanding stream shows its content. */
@Composable
internal fun PlayerSheetReopenEffect(request: OneShotRequest<PlayerSheetReturn>?, chat: WatchChatState) {
    LaunchedEffect(request) {
        when (val sheet = request?.take() ?: return@LaunchedEffect) {
            PlayerSheetReturn.ChatRecap -> chat.recapOpen = OneShotRequest(Unit)
            is PlayerSheetReturn.ChatterPortrait ->
                chat.fieldCardOpen = OneShotRequest(sheet.card.copy(page = ChatterCardPage.Portrait))
        }
    }
}

/**
 * Opens what [request] asks for once the stream shows its content: a sheet opened while the player is minimized
 * would close at once, as every chat sheet does while the content is hidden.
 */
@Composable
internal fun <T : Any> OpenWhenPlayerContentShown(request: OneShotRequest<T>?, open: (T) -> Unit) {
    val hidden = LocalPlayerContentHidden.current
    val latestOpen by rememberUpdatedState(open)
    LaunchedEffect(request, hidden) {
        if (hidden) return@LaunchedEffect
        request?.take()?.let(latestOpen)
    }
}
