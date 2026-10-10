package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import kotlin.math.abs
import kotlin.math.min

private val EmotePlaqueColor = TwitchPurple.copy(alpha = 0.45f)
private val EmotePlaqueRadius = 4.dp
private val EmotePlaquePadding = 2.dp

// One step of letter spacing is one plaque padding.
private val EmoteGapStep = EmotePlaquePadding.value.sp

/**
 * Widens the spaces around the draft's emote codes with letter spacing. It styles the shown text
 * and never edits it, so copy and paste see the real text.
 *
 * Keep one instance per field: a new instance makes the field rebuild its text layout. Read
 * changing emote lists from snapshot state inside [isEmoteCode] instead.
 */
internal class ChatDraftEmoteGaps(private val isEmoteCode: (String) -> Boolean) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence()
        for ((index, plaques) in chatDraftEmoteGaps(text, chatDraftEmoteRanges(text, isEmoteCode))) {
            val steps = chatDraftEmoteGapSteps(plaques, lastCharacter = index == text.length - 1)
            addStyle(SpanStyle(letterSpacing = EmoteGapStep * steps), index, index + 1)
        }
    }
}

/**
 * Draws a rounded plaque behind every emote code of the draft. Put it on the box around the
 * field's inner text, which does not scroll: the single-line text inside moves by [scrollState].
 */
internal fun Modifier.chatDraftEmotePlaques(
    isEmoteCode: (String) -> Boolean,
    textLayout: () -> TextLayoutResult?,
    scrollState: ScrollState,
): Modifier = drawBehind {
    val layout = textLayout() ?: return@drawBehind
    val text = layout.layoutInput.text
    val ranges = chatDraftEmoteRanges(text, isEmoteCode)
    if (ranges.isEmpty()) return@drawBehind
    val gaps = chatDraftEmoteGaps(text, ranges)
    val padding = EmotePlaquePadding.toPx()
    val radius = CornerRadius(EmotePlaqueRadius.toPx())
    clipRect(left = -padding, right = size.width + padding) {
        translate(left = -scrollState.value.toFloat()) {
            for (range in ranges) {
                val line = layout.getLineForOffset(range.start)
                // The layout reports the word's start half the letter spacing of the space in front of
                // it earlier than Android draws the first letter; without this the left side is wider.
                val spacingBefore = EmoteGapStep.toPx() * chatDraftEmoteGapStepsBefore(range, gaps)
                val start = layout.getHorizontalPosition(range.start, usePrimaryDirection = true) +
                    spacingBefore / 2
                val end = layout.getHorizontalPosition(range.end, usePrimaryDirection = true)
                val top = layout.getLineTop(line)
                drawRoundRect(
                    color = EmotePlaqueColor,
                    topLeft = Offset(min(start, end) - padding, top),
                    size = Size(abs(end - start) + padding * 2, layout.getLineBottom(line) - top),
                    cornerRadius = radius,
                )
            }
        }
    }
}
