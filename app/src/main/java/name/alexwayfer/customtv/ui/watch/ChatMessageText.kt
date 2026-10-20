package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import name.alexwayfer.customtv.channel.displayNameLoginSuffix
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.flaggedTermRanges
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio
import name.alexwayfer.customtv.ui.theme.ChatFlaggedTerm
import name.alexwayfer.customtv.ui.theme.ChatLink
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** The sizes of a chat message's text, emotes, and badges, with the slots they take in the text. */
internal data class ChatTextSizes(
    val text: TextUnit,
    val timestamp: TextUnit,
    val emote: Dp,
    val emoteHeight: TextUnit,
    val badge: Dp,
    val badgeSlotWidth: TextUnit,
    val badgeSlotHeight: TextUnit,
)

@Composable
internal fun chatTextSizes(textSize: TextUnit, timestampSize: TextUnit, emoteSize: Dp, badgeSize: Dp): ChatTextSizes =
    with(LocalDensity.current) {
        ChatTextSizes(
            text = textSize,
            timestamp = timestampSize,
            emote = emoteSize,
            emoteHeight = emoteSize.toSp(),
            badge = badgeSize,
            badgeSlotWidth = (badgeSize + BadgeGap).toSp(),
            badgeSlotHeight = badgeSize.toSp(),
        )
    }

/** How a chat message's text looks: its colors and sizes, what its author part shows, and whether links open. */
internal data class ChatMessageLook(
    val nickColor: Color,
    /** A /me message in italics, or in the author's color. */
    val actionItalic: Boolean,
    val actionColor: Boolean,
    val sizes: ChatTextSizes,
    val badgeUrls: Map<String, String>,
    val chatterLabels: Map<String, List<ChatterLabelIcon>>,
    /** The colors of the nicks this message mentions. */
    val nickColors: Map<String, Color>,
    val readableColors: Boolean,
    /** Links open on a tap; otherwise they only look like links, laid out the same, so the text does not reflow. */
    val linksClickable: Boolean,
    val includeAuthor: Boolean,
    val timestampText: String?,
    val topChatter: TopChatterBadge? = null,
) {
    val bodyColor: Color get() = if (actionColor) nickColor else TwitchText
    val bodyFontStyle: FontStyle get() = if (actionItalic) FontStyle.Italic else FontStyle.Normal
}

/** What a chat message's text does when its author, a link, an emote, or a GIF is tapped or held. */
internal class ChatMessageTextActions(
    val openAuthor: () -> Unit,
    val longClick: () -> Unit,
    val openLink: (String) -> Unit,
    val openEmote: (ChatPart.Emote) -> Unit,
    val openGif: (ChatPart.Gif) -> Unit,
    val closePreview: () -> Unit,
)

/** The message's text: its author part, its body, and a note when it was deleted. Fills [inlineContent] anew. */
internal fun chatMessageText(
    message: ChatMessage,
    look: ChatMessageLook,
    deletedNote: String?,
    inlineContent: MutableMap<String, InlineTextContent>,
    actions: ChatMessageTextActions,
): AnnotatedString {
    inlineContent.clear()
    return buildAnnotatedString {
        if (look.includeAuthor) appendChatAuthor(message, look, inlineContent, actions)
        val bodyStart = length
        appendChatBody(message, look, inlineContent, actions)
        val flaggedTerms = message.moderationHold?.flaggedTerms.orEmpty()
        if (flaggedTerms.isNotEmpty()) {
            val body = toAnnotatedString().text
            flaggedTermRanges(body, flaggedTerms, from = bodyStart).forEach { range ->
                addStyle(SpanStyle(background = ChatFlaggedTerm), range.first, range.last + 1)
            }
        }
        if (deletedNote != null) {
            withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = look.sizes.text)) {
                append(" ")
                append(deletedNote)
            }
        }
    }
}

/** The timestamp, the badges, and the author's name, which opens their card. */
private fun AnnotatedString.Builder.appendChatAuthor(
    message: ChatMessage,
    look: ChatMessageLook,
    inlineContent: MutableMap<String, InlineTextContent>,
    actions: ChatMessageTextActions,
) {
    val sizes = look.sizes
    appendChatTimestamp(look.timestampText, sizes.timestamp)
    appendTopChatterBadge(
        look.topChatter,
        sizes,
        inlineContent,
        onClick = actions.openAuthor,
        onLongClick = actions.longClick,
    )
    message.badges.forEachIndexed { index, badge ->
        val url = look.badgeUrls[badge.key] ?: return@forEachIndexed
        val key = "badge_$index"
        appendInlineContent(key, badge.setId)
        inlineContent[key] = badgeInlineContent(
            model = url,
            contentDescription = badge.setId,
            sizes = sizes,
            onClick = actions.openAuthor,
            onLongClick = actions.longClick,
        )
    }
    appendChatterLabelBadges(
        message.userId,
        look,
        inlineContent,
        onClick = actions.openAuthor,
        onLongClick = actions.longClick,
    )
    appendAuthorName(message, look.nickColor, sizes.text, actions.openAuthor)
}

private fun AnnotatedString.Builder.appendAuthorName(
    message: ChatMessage,
    nickColor: Color,
    textSize: TextUnit,
    openAuthor: () -> Unit,
) {
    val authorLinkStyle = SpanStyle(
        color = nickColor,
        fontWeight = FontWeight.Bold,
        fontSize = textSize,
        textDecoration = TextDecoration.None,
    )
    withLink(
        LinkAnnotation.Clickable(
            tag = CHAT_AUTHOR_LINK_TAG,
            styles = TextLinkStyles(
                style = authorLinkStyle,
                focusedStyle = authorLinkStyle,
                hoveredStyle = authorLinkStyle,
                pressedStyle = authorLinkStyle,
            ),
            linkInteractionListener = { openAuthor() },
        ),
    ) { append(message.displayName) }
    displayNameLoginSuffix(message.displayName, message.userLogin)?.let { suffix ->
        withStyle(
            SpanStyle(
                color = nickColor,
                fontWeight = FontWeight.Normal,
                fontSize = textSize,
            ),
        ) { append(suffix) }
    }
    withStyle(
        SpanStyle(
            color = if (message.isAction) nickColor else Color.White,
            fontSize = textSize,
        ),
    ) { append(if (message.isAction) " " else ": ") }
}

private fun AnnotatedString.Builder.appendChatBody(
    message: ChatMessage,
    look: ChatMessageLook,
    inlineContent: MutableMap<String, InlineTextContent>,
    actions: ChatMessageTextActions,
) {
    val textSize = look.sizes.text
    val bodyStyle = SpanStyle(
        color = look.bodyColor,
        fontSize = textSize,
        fontStyle = look.bodyFontStyle,
        textDecoration = if (message.deleted) TextDecoration.LineThrough else TextDecoration.None,
    )
    val linkStyle = SpanStyle(
        color = ChatLink,
        fontSize = textSize,
        fontStyle = look.bodyFontStyle,
        textDecoration = if (message.deleted) {
            TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        } else {
            TextDecoration.Underline
        },
    )
    message.parts.forEachIndexed { index, part ->
        when (part) {
            is ChatPart.Text -> if (isWhitespaceBetweenEmotes(message.parts, index)) {
                val nextEmote = message.parts[index + 1] as ChatPart.Emote
                val gapKey = "emote_gap_$index"
                appendInlineContent(gapKey, " ")
                inlineContent[gapKey] = InlineTextContent(
                    Placeholder(
                        emoteGapWidth(part, nextEmote).em,
                        look.sizes.emoteHeight,
                        PlaceholderVerticalAlign.TextCenter,
                    ),
                ) { Box(Modifier.fillMaxSize()) }
            } else {
                appendChatTextWithLinks(
                    text = part.text,
                    bodyStyle = bodyStyle,
                    linkStyle = linkStyle,
                    look = look,
                    onClick = actions.openLink,
                )
            }
            is ChatPart.Emote -> appendChatEmote(index, part, look.sizes, inlineContent, actions)
            is ChatPart.Gif -> Unit
        }
    }
}

/** An emote with its overlays: a tap previews it, and holding it selects the message. */
private fun AnnotatedString.Builder.appendChatEmote(
    index: Int,
    part: ChatPart.Emote,
    sizes: ChatTextSizes,
    inlineContent: MutableMap<String, InlineTextContent>,
    actions: ChatMessageTextActions,
) {
    val emoteKey = "emote_$index"
    val emoteAspect = resolvedEmoteAspectRatio(part.url, part.aspectRatio)
    val slotAspect = stackedEmoteSlotAspect(part)
    appendInlineContent(emoteKey, part.name)
    inlineContent[emoteKey] = InlineTextContent(
        Placeholder(
            sizes.emoteHeight * slotAspect,
            sizes.emoteHeight,
            PlaceholderVerticalAlign.TextCenter,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .combinedClickable(
                    hapticFeedbackEnabled = false,
                    onLongClick = actions.longClick,
                    onClick = { actions.openEmote(part) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            ChatEmoteEffectImage(
                url = part.url,
                contentDescription = part.name,
                size = sizes.emote,
                aspectRatio = emoteAspect,
                effects = part.effects,
            )
            part.overlays.forEach { overlay ->
                ChatEmoteEffectImage(
                    url = overlay.url,
                    contentDescription = null,
                    size = sizes.emote,
                    aspectRatio = resolvedEmoteAspectRatio(overlay.url, overlay.aspectRatio),
                    effects = overlay.effects,
                )
            }
        }
    }
}
