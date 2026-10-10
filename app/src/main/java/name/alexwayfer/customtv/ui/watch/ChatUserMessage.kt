package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.displayNameLoginSuffix
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.chat.flaggedTermRanges
import name.alexwayfer.customtv.chat.formatTimeoutDuration
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio
import name.alexwayfer.customtv.ui.components.revealLines
import kotlin.math.roundToInt
import name.alexwayfer.customtv.ui.theme.ChatFlaggedTerm
import name.alexwayfer.customtv.ui.theme.ChatLink
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

internal val LocalOnChatLinkClick = compositionLocalOf<(String) -> Unit> { {} }

@Composable
internal fun ChatUserMessage(
    message: ChatMessage,
    readableColors: Boolean,
    timestampText: String?,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
    emoteSize: Dp,
    gifSize: Dp,
    badgeSize: Dp,
    badgeUrls: Map<String, String>,
    meMessageItalic: Boolean,
    modifier: Modifier = Modifier,
    includeAuthor: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    showGifs: Boolean = true,
    enableLinks: Boolean = true,
    onEmoteLongClick: (() -> Unit)? = null,
    foldProgress: (() -> Float)? = null,
) {
    val nickColor = remember(message.color, readableColors) {
        if (readableColors) ReadableChatColor.adjust(message.color, TwitchBg) else message.color
    }
    // Only the colors of this message's nicks: a new chatter elsewhere does not rebuild the text.
    val allNickColors = LocalChatNickColors.current
    val nickColors by remember(message, allNickColors) {
        derivedStateOf { chatNickColorsFor(message, allNickColors.value) }
    }
    val actionItalic = message.isAction && meMessageItalic
    val actionColor = message.isAction && !meMessageItalic
    val bodyColor = if (actionColor) nickColor else TwitchText
    val bodyFontStyle = if (actionItalic) FontStyle.Italic else FontStyle.Normal
    val inlineContent = remember(message) { linkedMapOf<String, InlineTextContent>() }
    val emoteHeightSp = with(LocalDensity.current) { emoteSize.toSp() }
    val emoteAspects = message.parts.mapNotNull { part ->
        if (part is ChatPart.Emote) stackedEmoteSlotAspect(part) else null
    }
    val gifs = message.parts.filterIsInstance<ChatPart.Gif>()
    val deletedNote = when {
        !message.deleted -> null
        message.timeoutSeconds != null -> stringResource(
            R.string.chat_timed_out,
            formatTimeoutDuration(message.timeoutSeconds),
        )
        message.banned -> stringResource(R.string.chat_banned)
        else -> message.deletedBy?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.chat_deleted_by, it) }
            ?: stringResource(R.string.chat_deleted)
    }
    val bodyDecoration = if (message.deleted) TextDecoration.LineThrough else TextDecoration.None
    val onChatLinkClick = rememberUpdatedState(LocalOnChatLinkClick.current)
    val onEmoteLongClickState = rememberUpdatedState(onEmoteLongClick)
    val openChatterCardState = rememberUpdatedState(LocalOpenChatterCard.current)
    val sheetKeyboardState = rememberUpdatedState(LocalSheetKeyboard.current)
    val chatterLabels = LocalChatterLabels.current
    val badgeSlotWidth = with(LocalDensity.current) { (badgeSize + BadgeGap).toSp() }
    val badgeSlotHeight = with(LocalDensity.current) { badgeSize.toSp() }
    var previewEmote by remember { mutableStateOf<ChatPart.Emote?>(null) }
    var previewGif by remember { mutableStateOf<ChatPart.Gif?>(null) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotated = remember(
        message, nickColor, timestampText, textSize, timestampSize, emoteSize, emoteHeightSp,
        badgeSize, badgeSlotWidth, badgeSlotHeight, badgeUrls, chatterLabels, emoteAspects, deletedNote,
        bodyDecoration, actionItalic, actionColor,
        bodyColor, bodyFontStyle, nickColors, readableColors, includeAuthor, enableLinks,
    ) {
        inlineContent.clear()
        val openAuthor = {
            if (message.userLogin.isNotBlank()) {
                openChatterCardState.value(
                    ChatterCardRequest(
                        login = message.userLogin,
                        displayName = message.displayName,
                        userId = message.userId,
                        badges = message.badges,
                        badgeUrls = badgeUrls,
                        message = message,
                    ),
                )
            }
        }
        buildAnnotatedString {
            if (includeAuthor) {
                if (timestampText != null) {
                    withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = timestampSize)) {
                        append(timestampText)
                        append(" ")
                    }
                }
                message.badges.forEachIndexed { index, badge ->
                    val url = badgeUrls[badge.key] ?: return@forEachIndexed
                    val key = "badge_$index"
                    appendInlineContent(key, badge.setId)
                    inlineContent[key] = badgeInlineContent(
                        model = url,
                        contentDescription = badge.setId,
                        badgeSize = badgeSize,
                        slotWidth = badgeSlotWidth,
                        slotHeight = badgeSlotHeight,
                        onClick = openAuthor,
                    )
                }
                appendChatterLabelBadges(
                    message.userId,
                    badgeSize,
                    badgeSlotWidth,
                    badgeSlotHeight,
                    chatterLabels,
                    inlineContent,
                    onClick = openAuthor,
                )
                val authorLinkStyle = SpanStyle(
                    color = nickColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize,
                    textDecoration = TextDecoration.None,
                )
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "author",
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
            val bodyStart = length
            message.parts.forEachIndexed { index, part ->
                when (part) {
                    is ChatPart.Text -> {
                        if (isWhitespaceBetweenEmotes(message.parts, index)) {
                            val nextEmote = message.parts[index + 1] as ChatPart.Emote
                            val gapKey = "emote_gap_$index"
                            appendInlineContent(gapKey, " ")
                            inlineContent[gapKey] = InlineTextContent(
                                Placeholder(
                                    emoteGapWidth(part, nextEmote).em,
                                    emoteHeightSp,
                                    PlaceholderVerticalAlign.TextCenter,
                                ),
                            ) { Box(Modifier.fillMaxSize()) }
                        } else {
                            appendChatTextWithLinks(
                                text = part.text,
                                bodyStyle = SpanStyle(
                                    color = bodyColor,
                                    fontSize = textSize,
                                    fontStyle = bodyFontStyle,
                                    textDecoration = bodyDecoration,
                                ),
                                linkStyle = SpanStyle(
                                    color = ChatLink,
                                    fontSize = textSize,
                                    fontStyle = bodyFontStyle,
                                    textDecoration = if (message.deleted) {
                                        TextDecoration.combine(
                                            listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                                        )
                                    } else {
                                        TextDecoration.Underline
                                    },
                                ),
                                nickColors = nickColors,
                                readableColors = readableColors,
                                enableLinks = enableLinks,
                                onClick = { href -> onChatLinkClick.value(href) },
                            )
                        }
                    }
                    is ChatPart.Emote -> {
                        val emoteKey = "emote_$index"
                        val emoteAspect = resolvedEmoteAspectRatio(part.url, part.aspectRatio)
                        val slotAspect = stackedEmoteSlotAspect(part)
                        appendInlineContent(emoteKey, part.name)
                        inlineContent[emoteKey] = InlineTextContent(
                            Placeholder(
                                emoteHeightSp * slotAspect,
                                emoteHeightSp,
                                PlaceholderVerticalAlign.TextCenter,
                            ),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        hapticFeedbackEnabled = false,
                                        onLongClick = { onEmoteLongClickState.value?.invoke() },
                                        onClick = { sheetKeyboardState.value.open { previewEmote = part } },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                ChatEmoteEffectImage(
                                    url = part.url,
                                    contentDescription = part.name,
                                    size = emoteSize,
                                    aspectRatio = emoteAspect,
                                    effects = part.effects,
                                )
                                part.overlays.forEach { overlay ->
                                    ChatEmoteEffectImage(
                                        url = overlay.url,
                                        contentDescription = null,
                                        size = emoteSize,
                                        aspectRatio = resolvedEmoteAspectRatio(
                                            overlay.url,
                                            overlay.aspectRatio,
                                        ),
                                        effects = overlay.effects,
                                    )
                                }
                            }
                        }
                    }
                    is ChatPart.Gif -> Unit
                }
            }
            val flaggedTerms = message.moderationHold?.flaggedTerms.orEmpty()
            if (flaggedTerms.isNotEmpty()) {
                val body = toAnnotatedString().text
                flaggedTermRanges(body, flaggedTerms, from = bodyStart).forEach { range ->
                    addStyle(SpanStyle(background = ChatFlaggedTerm), range.first, range.last + 1)
                }
            }
            if (deletedNote != null) {
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                    append(" ")
                    append(deletedNote)
                }
            }
        }
    }
    Column(modifier.fillMaxWidth()) {
        Box {
            Text(
                text = annotated,
                inlineContent = inlineContent,
                lineHeight = lineHeight,
                maxLines = maxLines,
                overflow = overflow,
                onTextLayout = { textLayout = it },
                // A folding card lays every line out and cuts them off from the bottom as it closes.
                modifier = foldProgress?.let { progress ->
                    Modifier.revealLines({ textLayout?.getLineBottom(0)?.roundToInt() ?: 0 }, progress)
                } ?: Modifier,
                style = LocalTextStyle.current.copy(
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None,
                    ),
                ),
            )
            Box(Modifier.matchParentSize()) {
                textLayout?.takeIf { it.layoutInput.text == annotated }?.let { layout ->
                    annotated.getLinkAnnotations(0, annotated.length).forEach { range ->
                        val link = range.item as? LinkAnnotation.Clickable
                        if (link?.tag == "author") {
                            ChatAuthorTouchTarget(
                                layout = layout,
                                start = range.start,
                                end = range.end,
                                onClick = { link.linkInteractionListener?.onClick(link) },
                                onLongClick = onEmoteLongClick,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showGifs && gifs.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                gifs.forEach { gif ->
                    SharedEmoteImage(
                        url = gif.url,
                        contentDescription = gif.name,
                        size = gifSize,
                        aspectRatio = resolvedEmoteAspectRatio(gif.url, gif.aspectRatio),
                        modifier = Modifier
                            .padding(start = 8.dp, top = 4.dp)
                            .combinedClickable(
                                hapticFeedbackEnabled = false,
                                onLongClick = { onEmoteLongClickState.value?.invoke() },
                                onClick = { sheetKeyboardState.value.open { previewGif = gif } },
                            ),
                    )
                }
            }
        }
        previewEmote?.let { emote ->
            ChatEmotePreview(emote = emote, onDismiss = {
                previewEmote = null
                sheetKeyboardState.value.onDismiss()
            })
        }
        previewGif?.let { gif ->
            ChatGifPreview(gif = gif, onDismiss = {
                previewGif = null
                sheetKeyboardState.value.onDismiss()
            })
        }
    }
}

