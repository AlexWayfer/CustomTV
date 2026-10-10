package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import name.alexwayfer.customtv.chat.ChatNickSpan
import name.alexwayfer.customtv.chat.ChatUrlSpan
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.chat.findChatNicks
import name.alexwayfer.customtv.chat.findChatUrls
import name.alexwayfer.customtv.ui.theme.ChatLink
import name.alexwayfer.customtv.ui.theme.TwitchBg

internal fun AnnotatedString.Builder.appendChatTextWithLinks(
    text: String,
    bodyStyle: SpanStyle,
    linkStyle: SpanStyle,
    nickColors: Map<String, Color>,
    readableColors: Boolean,
    enableLinks: Boolean = true,
    onClick: (String) -> Unit,
) {
    val urls = if (enableLinks) findChatUrls(text) else emptyList()
    val nicks = findChatNicks(text, nickColors.keys)
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
                cursor = span.endExclusive
            }
            is ChatNickSpan -> {
                val known = nickColors[span.key]?.takeUnless { it == Color.Unspecified }
                val color = when {
                    known != null && readableColors -> ReadableChatColor.adjust(known, TwitchBg)
                    known != null -> known
                    else -> Color.White
                }
                withStyle(
                    bodyStyle.copy(
                        color = color,
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
