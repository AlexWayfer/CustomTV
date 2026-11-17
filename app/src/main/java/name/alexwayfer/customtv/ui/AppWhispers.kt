package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.ui.watch.ChatterCardRequest
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import name.alexwayfer.customtv.ui.watch.PlayerSheetReturn

/** Expands the already open stream and hands its chat the card requested from the conversation. */
internal fun AppScene.openWhisperChatterCard(): ((ChatterCardRequest) -> Unit)? {
    if (!watching || inPip || shell.player.liveClosedWithPip) return null
    return { card ->
        shell.playerSheetReopen = OneShotRequest(PlayerSheetReturn.WhisperChatter(card))
        shell.player = shell.player.withLiveMinimized(false)
    }
}
