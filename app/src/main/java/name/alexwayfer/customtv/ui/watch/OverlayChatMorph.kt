package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import name.alexwayfer.customtv.ui.theme.TwitchBg

/**
 * How compact the full overlaid chat looks as it rises out of the corner or sinks back into it ([progress], from 0
 * to 1, read when drawn), and which of its messages the compact chat shows, oldest first: those take its bubbles,
 * the rest fade.
 */
internal class OverlayChatRowMorph(
    val progress: () -> Float,
    val compactIds: List<String>,
    val fadedId: String?,
)

/**
 * Turns a row of the full chat into a bubble of the compact one along [morph]'s progress: the rounded see-through
 * ground, the faded top of the uppermost one, and the gap above each, which lifts the rows instead of laying them out
 * again, so neither the text nor the list moves under it. A row the compact chat leaves out fades away above them.
 * Without a morph the row stays as it is.
 */
internal fun Modifier.overlayChatRowMorph(morph: OverlayChatRowMorph?, id: String): Modifier {
    if (morph == null) return this
    val index = morph.compactIds.indexOf(id)
    val count = morph.compactIds.size
    if (index < 0) {
        return graphicsLayer {
            val progress = morph.progress().coerceIn(0f, 1f)
            alpha = 1f - progress
            translationY = -overlayChatRowLiftPx(count, CompactChatBubbleGap.toPx(), progress)
        }
    }
    val faded = id == morph.fadedId
    return graphicsLayer {
        translationY = -overlayChatRowLiftPx(count - index - 1, CompactChatBubbleGap.toPx(), morph.progress())
    }
        .fadedTop {
            if (faded) 1f - (1f - COMPACT_CHAT_FADED_TOP_ALPHA) * morph.progress().coerceIn(0f, 1f) else 1f
        }
        .graphicsLayer {
            val progress = morph.progress().coerceIn(0f, 1f)
            clip = progress > 0f
            shape = RoundedCornerShape(CompactChatBubbleCorner * progress)
        }
        .drawBehind {
            drawRect(TwitchBg.copy(alpha = FULLSCREEN_OVERLAY_CHAT_ALPHA * morph.progress().coerceIn(0f, 1f)))
        }
}

/**
 * How far a row rises as the chat turns compact: one bubble gap of [gapPx] for each compact row under it
 * ([gapsBelow]), [progress] of the way.
 */
internal fun overlayChatRowLiftPx(gapsBelow: Int, gapPx: Float, progress: Float): Float =
    gapsBelow.coerceAtLeast(0) * gapPx * progress.coerceIn(0f, 1f)

/**
 * How tall the compact chat will be, from the full chat's rows by each row's key and height: the rows it will show
 * and a gap of [gapPx] above each. The full chat sinks to it. Zero when the compact chat will show nothing; null when
 * its rows are off screen.
 */
internal fun compactRowsHeightPx(rows: List<Pair<Any, Int>>, compactIds: List<String>, gapPx: Int): Int? {
    if (compactIds.isEmpty()) return 0
    val shown = rows.filter { (key, _) -> key is String && key in compactIds }
    if (shown.isEmpty()) return null
    return shown.sumOf { (_, size) -> size.coerceAtLeast(0) + gapPx.coerceAtLeast(0) }
}

/**
 * How compact the full overlaid chat looks, from 0 to 1, as it rises out of the corner or sinks back into it: the
 * opposite of [expandProgress]. A chat that is not laid over the video stays as it is.
 */
internal fun overlayChatMorphProgress(overlaid: Boolean, expandProgress: Float): Float =
    if (overlaid) (1f - expandProgress).coerceIn(0f, 1f) else 0f
