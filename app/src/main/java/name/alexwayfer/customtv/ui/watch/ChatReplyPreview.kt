package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatReply
import name.alexwayfer.customtv.ui.components.CardFoldSpring
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.components.revealLines
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

/**
 * The message a chat row replies to, unfolded to all its lines while [expanded]. [onFoldChange] reports while it
 * folds or unfolds, from the composition that starts it, before its first frame changes the row's size.
 */
@Composable
internal fun ChatReplyPreview(
    reply: ChatReply,
    chatTextSize: Int,
    meMessageItalic: Boolean,
    expanded: Boolean,
    onFoldChange: (Boolean) -> Unit,
) {
    val textSize = chatSp(chatTextSize, 12, 14)
    val iconSize = chatDp(chatTextSize, 12, 14)
    val iconHeightPx = with(LocalDensity.current) { iconSize.roundToPx() }
    var textHeightPx by remember { mutableIntStateOf(0) }
    // Every line stays laid out while the preview folds, and the fold cuts them off from the bottom.
    val transition = updateTransition(expanded, label = "replyPreview")
    val progress by transition.animateFloat({ CardFoldSpring }, label = "replyPreviewLines") { if (it) 1f else 0f }
    val allLines = transition.currentState || transition.targetState
    SideEffect { onFoldChange(transition.currentState != transition.targetState) }
    var firstLineBottomPx by remember { mutableIntStateOf(0) }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_reply),
            contentDescription = null,
            tint = TwitchTextSecondary,
            modifier = Modifier
                .size(iconSize)
                .offset { IntOffset(0, ((textHeightPx - iconHeightPx) / 2).coerceAtLeast(0)) },
        )
        Text(
            text = remember(reply, textSize, meMessageItalic) {
                buildAnnotatedString {
                    if (reply.parentBody.isBlank()) {
                        append(reply.parentDisplayName)
                        return@buildAnnotatedString
                    }
                    append(reply.parentDisplayName)
                    if (reply.parentIsAction) {
                        append(" ")
                        if (meMessageItalic) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSize = textSize)) {
                                append(reply.parentBody)
                            }
                        } else {
                            append(reply.parentBody)
                        }
                    } else {
                        append(": ")
                        append(reply.parentBody)
                    }
                }
            },
            color = TwitchTextSecondary,
            fontSize = textSize,
            maxLines = collapsibleTextMaxLines(allLines),
            overflow = if (allLines) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { layout -> firstLineBottomPx = layout.getLineBottom(0).roundToInt() },
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
                .onSizeChanged { textHeightPx = it.height }
                .revealLines({ firstLineBottomPx }, { progress }),
        )
    }
}
