package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * How tall a sheet opened from the chat may be, in pixels: up to the player's bottom edge, so the stream stays in
 * sight. Null where the sheet may take the screen, such as outside a player.
 */
internal val LocalChatSheetMaxHeightPx = compositionLocalOf<() -> Int?> { { null } }

/** The room under the player; none when the player reaches the bottom, as beside the chat in landscape. */
internal fun chatSheetMaxHeightPx(containerHeightPx: Int, playerBottomPx: Int): Int? =
    chatBrowserSheetHeightPx(containerHeightPx, playerBottomPx).takeIf { it > 0 }

/** What the sheet's content may take of [sheetMaxPx] after the drag handle above it and the system bar below it. */
internal fun chatSheetContentMaxHeightPx(sheetMaxPx: Int, dragHandlePx: Int, bottomInsetPx: Int): Int =
    (sheetMaxPx - dragHandlePx - bottomInsetPx).coerceAtLeast(0)

/**
 * The height limit for a chat sheet's content, for `Modifier.heightIn(max = …)`; unspecified where it has none.
 * The limit goes on the content, not on `ModalBottomSheet` itself: the sheet places itself by the height its own
 * modifier allows, so a limit there would lift it off the bottom.
 */
@Composable
internal fun chatSheetContentMaxHeight(sheetInsets: WindowInsets = WindowInsets.safeDrawing): Dp {
    val sheetMaxPx = LocalChatSheetMaxHeightPx.current() ?: return Dp.Unspecified
    val density = LocalDensity.current
    val px = chatSheetContentMaxHeightPx(
        sheetMaxPx = sheetMaxPx,
        dragHandlePx = with(density) { SHEET_DRAG_HANDLE_HEIGHT.roundToPx() },
        // The sheet pads its content by the bottom of its insets.
        bottomInsetPx = sheetInsets.getBottom(density),
    )
    return with(density) { px.toDp() }
}

/** A sheet's top and bottom insets, in pixels. */
internal data class ChatSheetInsetsPx(val top: Int, val bottom: Int)

/**
 * The insets a sheet keeps: the [live] ones while the screen is in front, and the ones it [held] from then while a
 * browser tab lies over it. A tab hides the navigation bar of the screen for a moment as it opens and closes, and the
 * live bottom inset drops to zero, though the bar stays where it was.
 */
internal fun chatSheetInsetsShown(live: ChatSheetInsetsPx, held: ChatSheetInsetsPx?, inFront: Boolean) =
    if (inFront) live else held ?: live

/**
 * The insets of a chat sheet's content, for `ModalBottomSheet(contentWindowInsets = …)` and
 * [chatSheetContentMaxHeight], as the screen's window reports them while it is in front ([chatSheetInsetsShown]).
 * Call it outside `ModalBottomSheet`: the sheet's own window reports no bottom inset for a moment whenever it comes
 * back to the front, as when a browser tab over it closes, and the sheet would jump by the system bar's height and
 * back.
 */
@Composable
internal fun chatSheetWindowInsets(): WindowInsets {
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    val live = ChatSheetInsetsPx(top = insets.getTop(density), bottom = insets.getBottom(density))
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val inFront = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val held = remember { HeldChatSheetInsets() }
    SideEffect { if (inFront) held.inFront = live }
    val shown = chatSheetInsetsShown(live, held.inFront, inFront)
    return WindowInsets(top = shown.top, bottom = shown.bottom)
}

/** The insets the screen had when it was last in front; not state, since it changes only what the next pass reads. */
private class HeldChatSheetInsets {
    var inFront: ChatSheetInsetsPx? = null
}

/** Material's drag handle: 4dp tall with 22dp above and below it. */
private val SHEET_DRAG_HANDLE_HEIGHT = 48.dp
