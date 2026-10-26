package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.chat.formatTimeoutDuration
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio
import name.alexwayfer.customtv.ui.components.revealLines
import kotlin.math.roundToInt
import name.alexwayfer.customtv.ui.theme.TwitchBg

internal val LocalOnChatLinkClick = compositionLocalOf<(String) -> Unit> { {} }

/** The link whose menu is open, with its range in the message text to place the menu by. */
private data class ChatLinkMenuTarget(val url: String, val start: Int, val end: Int)

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
    /** False where the row's event line already shows the channel a shared message came from. */
    showSharedChatSource: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    showGifs: Boolean = true,
    linksClickable: Boolean = true,
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
    val emoteAspects = message.parts.mapNotNull { part ->
        if (part is ChatPart.Emote) stackedEmoteSlotAspect(part) else null
    }
    val deletedNote = chatDeletedNote(message)
    val preview = remember { ChatPartPreview() }
    val actions = chatMessageTextActions(message, badgeUrls, onEmoteLongClick, preview)
    val cosmetics = rememberChatterCosmetics(message.userId)
    // Where the laid out name lies, which a paint covers; its first frame keeps the name's color.
    var nameBox by remember(message) { mutableStateOf<Rect?>(null) }
    val namePaint = rememberNamePaintSpan(cosmetics.paint?.takeIf { includeAuthor }, nickColor, nameBox)
    val look = ChatMessageLook(
        nickColor = nickColor,
        actionItalic = message.isAction && meMessageItalic,
        actionColor = message.isAction && !meMessageItalic,
        sizes = chatTextSizes(textSize, timestampSize, emoteSize, badgeSize),
        badgeUrls = badgeUrls,
        chatterLabels = LocalChatterLabels.current,
        nickColors = nickColors,
        readableColors = readableColors,
        linksClickable = linksClickable,
        includeAuthor = includeAuthor,
        timestampText = timestampText,
        topChatter = topChatterBadge(message.userLogin),
        sharedChatSource = sharedChatSourceLook(message.sourceRoomId.takeIf { showSharedChatSource }),
        extraBadges = cosmetics.badges,
        namePaint = namePaint,
    )
    val inlineContent = remember(message) { linkedMapOf<String, InlineTextContent>() }
    val annotated = remember(message, look, deletedNote, emoteAspects) {
        chatMessageText(message, look, deletedNote, inlineContent, actions)
    }
    val painted = includeAuthor && cosmetics.paint != null
    Column(modifier.fillMaxWidth()) {
        ChatMessageTextWithLinks(
            text = annotated,
            color = look.bodyColor,
            inlineContent = inlineContent,
            lines = ChatTextLines(lineHeight, maxLines, overflow, foldProgress),
            onSelectMessage = onEmoteLongClick,
            onOpenLink = actions.openLink,
            onLayout = { layout -> if (painted) chatAuthorNameBox(annotated, layout)?.let { nameBox = it } },
        )
        val gifs = message.parts.filterIsInstance<ChatPart.Gif>()
        ChatMessageGifs(gifs, visible = showGifs && gifs.isNotEmpty(), gifSize, actions)
        ChatPartPreviews(preview, onDismiss = actions.closePreview)
    }
}

/** Why a deleted message is gone: a timeout, a ban, or who deleted it. Null while it stands. */
@Composable
private fun chatDeletedNote(message: ChatMessage): String? = when {
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

/** The message's actions; each reads the latest handler the chat provides when it runs. */
@Composable
private fun chatMessageTextActions(
    message: ChatMessage,
    badgeUrls: Map<String, String>,
    onSelectMessage: (() -> Unit)?,
    preview: ChatPartPreview,
): ChatMessageTextActions {
    val onChatLinkClick = rememberUpdatedState(LocalOnChatLinkClick.current)
    val onSelectMessageState = rememberUpdatedState(onSelectMessage)
    val openChatterCardState = rememberUpdatedState(LocalOpenChatterCard.current)
    val sheetKeyboardState = rememberUpdatedState(LocalSheetKeyboard.current)
    return ChatMessageTextActions(
        openAuthor = {
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
        },
        longClick = { onSelectMessageState.value?.invoke() },
        openLink = { href -> onChatLinkClick.value(href) },
        openEmote = { emote -> sheetKeyboardState.value.open { preview.emote = emote } },
        openGif = { gif -> sheetKeyboardState.value.open { preview.gif = gif } },
        closePreview = { sheetKeyboardState.value.onDismiss() },
    )
}

/** How the message's lines are laid out; a folding card cuts them off from the bottom by [foldProgress]. */
private data class ChatTextLines(
    val lineHeight: TextUnit,
    val maxLines: Int,
    val overflow: TextOverflow,
    val foldProgress: (() -> Float)?,
)

/** The message's text, with touch targets over its author and links and the menu of a held link. */
@Composable
private fun ChatMessageTextWithLinks(
    text: AnnotatedString,
    /** The color of what has no span of its own: an ellipsis that takes an emote's place in cut-off lines. */
    color: Color,
    inlineContent: Map<String, InlineTextContent>,
    lines: ChatTextLines,
    onSelectMessage: (() -> Unit)?,
    onOpenLink: (String) -> Unit,
    onLayout: (TextLayoutResult) -> Unit,
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var menuLink by remember { mutableStateOf<ChatLinkMenuTarget?>(null) }
    val context = LocalContext.current
    Box {
        Text(
            text = text,
            color = color,
            inlineContent = inlineContent,
            lineHeight = lines.lineHeight,
            maxLines = lines.maxLines,
            overflow = lines.overflow,
            onTextLayout = {
                textLayout = it
                onLayout(it)
            },
            // A folding card lays every line out and cuts them off from the bottom as it closes.
            modifier = lines.foldProgress?.let { progress ->
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
            textLayout?.takeIf { layoutTextMatches(it.layoutInput.text, text) }?.let { layout ->
                ChatLinkTouchTargets(text, layout, onSelectMessage, onLinkMenu = { menuLink = it })
                menuLink?.let { target ->
                    ChatLinkMenuPopup(
                        layout = layout,
                        start = target.start,
                        end = target.end,
                        onOpen = {
                            menuLink = null
                            onOpenLink(target.url)
                        },
                        onCopy = {
                            context.copyChatText(target.url)
                            menuLink = null
                        },
                        onDismiss = { menuLink = null },
                    )
                }
            }
        }
    }
}

/** A touch target over each name and link: holding a link opens its menu, holding a name selects the message. */
@Composable
private fun ChatLinkTouchTargets(
    text: AnnotatedString,
    layout: TextLayoutResult,
    onSelectMessage: (() -> Unit)?,
    onLinkMenu: (ChatLinkMenuTarget) -> Unit,
) {
    text.getLinkAnnotations(0, text.length).forEach { range ->
        val link = range.item
        val longPress = chatLinkLongPress(link) ?: return@forEach
        ChatAuthorTouchTarget(
            layout = layout,
            start = range.start,
            end = range.end,
            onClick = { link.linkInteractionListener?.onClick(link) },
            onLongClick = when (longPress) {
                ChatLinkLongPress.SelectMessage -> onSelectMessage
                is ChatLinkLongPress.LinkMenu -> {
                    { onLinkMenu(ChatLinkMenuTarget(longPress.url, range.start, range.end)) }
                }
            },
        )
    }
}

/** The message's GIFs under its text: a tap previews one, and holding it selects the message. */
@Composable
private fun ColumnScope.ChatMessageGifs(
    gifs: List<ChatPart.Gif>,
    visible: Boolean,
    gifSize: Dp,
    actions: ChatMessageTextActions,
) {
    AnimatedVisibility(
        visible = visible,
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
                            onLongClick = actions.longClick,
                            onClick = { actions.openGif(gif) },
                        ),
                )
            }
        }
    }
}

/** The emote or GIF of the message shown large. */
@Stable
private class ChatPartPreview {
    var emote by mutableStateOf<ChatPart.Emote?>(null)
    var gif by mutableStateOf<ChatPart.Gif?>(null)
}

@Composable
private fun ChatPartPreviews(preview: ChatPartPreview, onDismiss: () -> Unit) {
    preview.emote?.let { emote ->
        ChatEmotePreview(emote = emote, onDismiss = {
            preview.emote = null
            onDismiss()
        })
    }
    preview.gif?.let { gif ->
        ChatGifPreview(gif = gif, onDismiss = {
            preview.gif = null
            onDismiss()
        })
    }
}

