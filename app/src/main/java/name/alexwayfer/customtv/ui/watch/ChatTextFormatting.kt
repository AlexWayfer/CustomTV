package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import name.alexwayfer.customtv.chat.ChatNickSpan
import name.alexwayfer.customtv.chat.ChatUrlSpan
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.chat.findChatNicks
import name.alexwayfer.customtv.chat.findChatUrls
import name.alexwayfer.customtv.ui.theme.ChatLink
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** [text] in [bodyStyle], with its links in [linkStyle] and the nicks it mentions in their colors from [look]. */
internal fun AnnotatedString.Builder.appendChatTextWithLinks(
    text: String,
    bodyStyle: SpanStyle,
    linkStyle: SpanStyle,
    look: ChatMessageLook,
    onClick: (String) -> Unit,
) {
    val urls = if (look.enableLinks) findChatUrls(text) else emptyList()
    val nicks = findChatNicks(text, look.nickColors.keys)
    if (urls.isEmpty() && nicks.isEmpty()) {
        withStyle(bodyStyle) { append(keepCommandPrefixTogether(text)) }
        return
    }
    val marks = buildList {
        urls.forEach { add(Triple(it.start, 0, it as Any)) }
        nicks.forEach { add(Triple(it.start, 1, it as Any)) }
    }.sortedWith(compareBy({ it.first }, { it.second }))
    var cursor = 0
    for ((start, _, span) in marks) {
        if (start < cursor) continue
        if (start > cursor) {
            withStyle(bodyStyle) { append(keepCommandPrefixTogether(text.substring(cursor, start))) }
        }
        when (span) {
            is ChatUrlSpan -> {
                appendChatLink(span, linkStyle, onClick)
                cursor = span.endExclusive
            }
            is ChatNickSpan -> {
                withStyle(
                    bodyStyle.copy(
                        color = chatNickColor(look.nickColors[span.key], look.readableColors),
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append(span.raw)
                }
                cursor = span.endExclusive
            }
        }
    }
    if (cursor < text.length) {
        withStyle(bodyStyle) { append(keepCommandPrefixTogether(text.substring(cursor))) }
    }
}

private fun AnnotatedString.Builder.appendChatLink(span: ChatUrlSpan, linkStyle: SpanStyle, onClick: (String) -> Unit) {
    withLink(
        LinkAnnotation.Url(
            span.href,
            TextLinkStyles(
                style = linkStyle,
                focusedStyle = linkStyle,
                hoveredStyle = linkStyle,
                pressedStyle = linkStyle.copy(color = ChatLink.copy(alpha = 0.75f)),
            ),
            linkInteractionListener = { onClick(span.href) },
        ),
    ) {
        append(keepUrlPathTogether(span.raw))
    }
}

/** The time a chat line starts with, in the secondary color and a space after it; nothing without a [timestamp]. */
internal fun AnnotatedString.Builder.appendChatTimestamp(timestamp: String?, size: TextUnit) {
    if (timestamp == null) return
    withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = size)) {
        append(timestamp)
        append(" ")
    }
}

/** The [text] in the secondary color, with the chatter's [name] in it bold in their [nickColor] where it is found. */
internal fun AnnotatedString.Builder.appendWithChatterName(
    text: String,
    name: String,
    nickColor: Color,
    textSize: TextUnit,
) {
    val plain = SpanStyle(color = TwitchTextSecondary, fontSize = textSize)
    val nameStart = text.indexOf(name, ignoreCase = true)
    if (nameStart < 0 || name.isBlank()) {
        withStyle(plain) {
            append(text)
        }
        return
    }
    withStyle(plain) {
        append(text.substring(0, nameStart))
    }
    withStyle(SpanStyle(color = nickColor, fontWeight = FontWeight.Bold, fontSize = textSize)) {
        append(text.substring(nameStart, nameStart + name.length))
    }
    withStyle(plain) {
        append(text.substring(nameStart + name.length))
    }
}

/** A mentioned chatter's chat color, made readable on the dark chat when asked; white for one not seen yet. */
private fun chatNickColor(known: Color?, readableColors: Boolean): Color {
    val color = known?.takeUnless { it == Color.Unspecified } ?: return Color.White
    return if (readableColors) ReadableChatColor.adjust(color, TwitchBg) else color
}

// Word Joiner prevents a break after "!" without changing the copied message text.
internal fun keepCommandPrefixTogether(text: String): String =
    text.replace(COMMAND_PREFIX, "!\u2060")

private val COMMAND_PREFIX = Regex("(?<![\\p{L}\\p{N}_])!(?=[\\p{L}_])")

// Keep the host, path slash and first path character on the same line.
internal fun keepUrlPathTogether(raw: String): String {
    val authorityStart = raw.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: 0
    val slash = raw.indexOf('/', authorityStart)
    if (slash < 0 || slash == raw.lastIndex) return raw
    return raw.substring(0, slash) + "\u2060/\u2060" + raw.substring(slash + 1)
}
