package name.alexwayfer.customtv.ui.watch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.LocalGestureHints
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.ChatLink
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

/** Puts a mention of a chatter into the chat's draft; null where the chat has no field to write in. */
internal val LocalOnChatMention = compositionLocalOf<((login: String, displayName: String) -> Unit)?> { null }

@Composable
internal fun ChatMessageCopyPopup(
    onReply: (() -> Unit)?,
    onMention: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val gestureHints = LocalGestureHints.current
    ChatActionPopup(onDismiss) {
        if (onReply != null) {
            ChatMessageActionRow(
                icon = R.drawable.ic_reply_arrow,
                label = stringResource(R.string.chat_reply),
                onClick = {
                    gestureHints?.slowPathUsed(GestureHint.Reply)
                    onReply()
                },
            )
        }
        if (onMention != null) {
            ChatMessageActionRow(
                icon = R.drawable.ic_chat_mention,
                label = stringResource(R.string.chat_mention),
                onClick = onMention,
            )
        }
        if (onCopy != null) {
            ChatMessageActionRow(
                icon = R.drawable.ic_chat_copy,
                label = stringResource(R.string.chat_copy),
                onClick = onCopy,
            )
        }
    }
}

/**
 * Shown on a long press on the link at [start] until [end] of the text laid out in [layout]. It sits by the
 * link's end as the message menu sits by the message's: below its last line, at its end edge. Call it inside
 * a box that covers the text exactly.
 */
@Composable
internal fun ChatLinkMenuPopup(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (start >= end) return
    val density = LocalDensity.current
    val lastLineStart = layout.getLineStart(layout.getLineForOffset(end - 1))
    val bounds = layout.getPathForRange(linkLastLineStart(start, lastLineStart), end).getBounds()
    // A popup places itself by the box it is called in, so this box covers the link's last line.
    Box(
        Modifier
            .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() }),
    ) {
        ChatActionPopup(onDismiss) {
            ChatMessageActionRow(
                icon = R.drawable.ic_open_in_new,
                label = stringResource(R.string.chat_open_link),
                onClick = onOpen,
            )
            ChatMessageActionRow(
                icon = R.drawable.ic_chat_copy,
                label = stringResource(R.string.chat_copy),
                onClick = onCopy,
            )
        }
    }
}

/**
 * Not focusable, so the message field keeps its focus and the keyboard stays up under it. A touch outside still
 * closes it, but Back reaches the app's window, so a handler here takes it.
 */
@Composable
private fun ChatActionPopup(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    BackHandler(onBack = onDismiss)
    Popup(
        popupPositionProvider = remember { ChatCopyPopupPositionProvider() },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, clippingEnabled = true),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TwitchSurfaceAlt,
            shadowElevation = 6.dp,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ChatMessageActionRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = TwitchText,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = TwitchText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Asks before [url] opens outside the app; fades in when a link is chosen and out once it is answered. */
@Composable
internal fun ChatOpenLinkOverlay(
    url: String?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val shownUrl = rememberLastNonNull(url)
    // Registered only while asking, so it is the newest handler and takes Back before the panels under it.
    if (url != null) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible = url != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.54f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.padding(horizontal = 28.dp).blockClicks(),
                color = TwitchSurface,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 12.dp, bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.chat_open_link_title),
                        color = TwitchText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = shownUrl.orEmpty(),
                        color = ChatLink,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp, end = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel), color = TwitchTextSecondary)
                        }
                        TextButton(onClick = { url?.let(onOpen) }) {
                            Text(
                                stringResource(R.string.chat_open_link),
                                color = TwitchPurple,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Where the part of a link on its last line starts: the link's own start when it fits on one line. */
internal fun linkLastLineStart(linkStart: Int, lastLineStart: Int): Int = maxOf(linkStart, lastLineStart)

internal fun Context.copyChatText(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("chat", text))
}

private class ChatCopyPopupPositionProvider : PopupPositionProvider {
    private var placeAbove: Boolean? = null

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = 8
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(0)
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width - margin
        } else {
            anchorBounds.left + margin
        }.coerceIn(margin, maxX)
        val above = anchorBounds.top - popupContentSize.height - margin
        val below = anchorBounds.bottom + margin
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(0)
        val canPlaceBelow = below + popupContentSize.height <= windowSize.height - margin
        val canPlaceAbove = above >= margin
        if (placeAbove == null) placeAbove = !canPlaceBelow && canPlaceAbove
        val y = if (placeAbove == true) above.coerceIn(margin, maxY) else below.coerceIn(margin, maxY)
        return IntOffset(x, y)
    }
}
