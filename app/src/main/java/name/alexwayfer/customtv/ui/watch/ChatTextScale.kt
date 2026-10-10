package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.DEFAULT_CHAT_TEXT_SIZE

/** The text size at which each chat size takes its `large` value. */
private const val LARGE_ANCHOR_TEXT_SIZE = 16

/**
 * A chat size for [textSize], on the line through its [normal] value at the default text size
 * and its [large] value at text size 16.
 */
internal fun chatScaled(textSize: Int, normal: Int, large: Int): Float =
    normal + (large - normal) * (textSize - DEFAULT_CHAT_TEXT_SIZE).toFloat() /
        (LARGE_ANCHOR_TEXT_SIZE - DEFAULT_CHAT_TEXT_SIZE)

internal fun chatSp(textSize: Int, normal: Int, large: Int): TextUnit = chatScaled(textSize, normal, large).sp

internal fun chatDp(textSize: Int, normal: Int, large: Int): Dp = chatScaled(textSize, normal, large).dp
