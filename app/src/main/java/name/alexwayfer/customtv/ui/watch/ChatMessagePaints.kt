package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.NamePaint

/** The mark on a nick that has a paint, holding the nick, for its box to be found in the laid out text. */
internal const val CHAT_NICK_PAINT_TAG = "nick_paint"

/** Where a painted [nick] that starts at [start] in its text lies once the text is laid out. */
internal data class NickPaintBox(val start: Int, val nick: String, val box: Rect)

/** Where a message's painted names lie in its laid out text: the author's, and those of the nicks it names. */
@Stable
internal class ChatPaintBoxes {
    var name by mutableStateOf<Rect?>(null)
    var nicks by mutableStateOf(emptyList<NickPaintBox>())
}

/** The 7TV paints of a message's text: over its author's name, and over each nick it names whose chatter has one. */
internal class ChatMessagePaints(
    val name: SpanStyle?,
    val paintedNicks: Set<String>,
    val nickStyles: Map<Int, SpanStyle>,
    private val boxes: ChatPaintBoxes,
    private val authorPainted: Boolean,
) {
    /** Finds where the painted names lie in [layout] of [text], for each paint to cover only its name. */
    fun record(text: AnnotatedString, layout: TextLayoutResult) {
        if (authorPainted) chatAuthorNameBox(text, layout)?.let { boxes.name = it }
        if (paintedNicks.isNotEmpty()) chatNickPaintBoxes(text, layout)?.let { boxes.nicks = it }
    }
}

/**
 * The paints of [message]: [authorPaint] over the author's name in [nickColor], and the paints of the nicks it names
 * over their [nickColors]. Each name keeps its color until the text is laid out and its box is known.
 */
@Composable
internal fun rememberChatMessagePaints(
    message: ChatMessage,
    authorPaint: NamePaint?,
    nickColor: Color,
    nickColors: Map<String, Color>,
    readableColors: Boolean,
): ChatMessagePaints {
    val boxes = remember(message) { ChatPaintBoxes() }
    val name = rememberNamePaintSpan(authorPaint, nickColor, boxes.name)
    val nickPaints = rememberChatNickPaints(nickColors.keys)
    val nickStyles = if (nickPaints.isEmpty()) {
        emptyMap()
    } else {
        rememberNamePaintSpans(boxedNickPaints(boxes.nicks, nickPaints, nickColors, readableColors))
    }
    return ChatMessagePaints(name, nickPaints.keys, nickStyles, boxes, authorPainted = authorPaint != null)
}

/** The 7TV paints of the [nicks] a message names, by nick; another chatter's change does not recompose the caller. */
@Composable
private fun rememberChatNickPaints(nicks: Set<String>): Map<String, NamePaint> {
    val all = LocalChatCosmetics.current
    val paints by remember(nicks, all) {
        derivedStateOf(structuralEqualityPolicy()) {
            val cosmetics = all.value
            nicks.mapNotNull { nick -> cosmetics.paintForNick(nick)?.let { nick to it } }.toMap()
        }
    }
    return paints
}

/** Where the painted nicks lie in a message's [layout], for each paint to cover only its nick; null while stale. */
private fun chatNickPaintBoxes(text: AnnotatedString, layout: TextLayoutResult): List<NickPaintBox>? {
    if (!layoutTextMatches(layout.layoutInput.text, text)) return null
    val laidOut = layout.layoutInput.text.length
    return text.getStringAnnotations(CHAT_NICK_PAINT_TAG, 0, text.length)
        .filter { it.end > it.start && it.end <= laidOut }
        .map { NickPaintBox(it.start, it.item, layout.getPathForRange(it.start, it.end).getBounds()) }
}

/** The paints of the [boxes] whose nicks have one in [paints], by where each starts, over the nicks' [colors]. */
internal fun boxedNickPaints(
    boxes: List<NickPaintBox>,
    paints: Map<String, NamePaint>,
    colors: Map<String, Color>,
    readableColors: Boolean,
): Map<Int, BoxedNamePaint> = boxes.mapNotNull { box ->
    paints[box.nick]?.let { paint ->
        box.start to BoxedNamePaint(paint, chatNickColor(colors[box.nick], readableColors), box.box)
    }
}.toMap()
