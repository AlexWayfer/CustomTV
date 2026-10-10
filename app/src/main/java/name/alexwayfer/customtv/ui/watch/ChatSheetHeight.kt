package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
internal fun chatSheetContentMaxHeight(): Dp {
    val sheetMaxPx = LocalChatSheetMaxHeightPx.current() ?: return Dp.Unspecified
    val density = LocalDensity.current
    val px = chatSheetContentMaxHeightPx(
        sheetMaxPx = sheetMaxPx,
        dragHandlePx = with(density) { SHEET_DRAG_HANDLE_HEIGHT.roundToPx() },
        // The sheet pads its content by the bottom of the safe drawing area.
        bottomInsetPx = WindowInsets.safeDrawing.getBottom(density),
    )
    return with(density) { px.toDp() }
}

/** Material's drag handle: 4dp tall with 22dp above and below it. */
private val SHEET_DRAG_HANDLE_HEIGHT = 48.dp
