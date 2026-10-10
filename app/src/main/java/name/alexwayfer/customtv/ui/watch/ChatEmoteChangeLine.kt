package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatEmoteChange
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.EmoteChangeAction
import name.alexwayfer.customtv.chat.EmotePlatform
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatEmoteChangeLine(
    message: ChatMessage,
    change: ChatEmoteChange,
    readableColors: Boolean,
    stripeColor: Color,
    showTimestamps: Boolean,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
    emoteSize: Dp,
    logo: ChatPart.Emote?,
) {
    val context = LocalContext.current
    val timestampText = remember(message.timestampMillis, showTimestamps) {
        if (showTimestamps) formatChatTimestamp(context, message.timestampMillis) else null
    }
    val knownNickColor = change.actorColor.takeUnless {
        it == Color.Unspecified || it == Color.White
    }
    val nickColor = remember(knownNickColor, readableColors) {
        val color = knownNickColor ?: Color.White
        if (knownNickColor != null && readableColors) {
            ReadableChatColor.adjust(color, TwitchBg)
        } else {
            color
        }
    }
    val actionText = when (change.action) {
        EmoteChangeAction.Added -> stringResource(R.string.chat_emote_added)
        EmoteChangeAction.Removed -> stringResource(R.string.chat_emote_removed)
        EmoteChangeAction.Renamed -> stringResource(R.string.chat_emote_renamed)
    }
    val platformLabel = when (change.platform) {
        EmotePlatform.SevenTv -> stringResource(R.string.chat_emote_platform_7tv)
        EmotePlatform.Bttv -> stringResource(R.string.chat_emote_platform_bttv)
        EmotePlatform.Ffz -> stringResource(R.string.chat_emote_platform_ffz)
    }
    val emotePart = change.emote?.let { emote ->
        ChatPart.Emote(
            name = change.emoteName.ifBlank { emote.id.orEmpty() },
            url = emote.url,
            aspectRatio = emote.aspectRatio,
        )
    }
    val nameLabel = buildString {
        val shownName = change.emoteName.ifBlank { change.emoteId.orEmpty() }
        if (change.previousName != null && change.previousName != shownName) {
            append(change.previousName)
            append(" → ")
            append(shownName)
        } else {
            append(shownName)
        }
    }
    val emoteHeightSp = with(LocalDensity.current) { emoteSize.toSp() }
    val inlineContent = remember { linkedMapOf<String, InlineTextContent>() }
    val annotated = remember(
        logo,
        platformLabel,
        timestampText,
        nickColor,
        change.actorName,
        actionText,
        emotePart,
        nameLabel,
        textSize,
        timestampSize,
        emoteSize,
        emoteHeightSp,
    ) {
        inlineContent.clear()
        buildAnnotatedString {
            if (logo != null) {
                val logoAspect = resolvedEmoteAspectRatio(logo.url, logo.aspectRatio)
                appendInlineContent("logo", platformLabel)
                inlineContent["logo"] = InlineTextContent(
                    Placeholder(
                        emoteHeightSp * logoAspect,
                        emoteHeightSp,
                        PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    SharedEmoteImage(
                        url = logo.url,
                        contentDescription = platformLabel,
                        size = emoteSize,
                        aspectRatio = logoAspect,
                    )
                }
            } else {
                withStyle(
                    SpanStyle(
                        color = TwitchTextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = textSize,
                    ),
                ) { append(platformLabel) }
            }
            append(" ")
            if (timestampText != null) {
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = timestampSize)) {
                    append(timestampText)
                    append(" ")
                }
            }
            withStyle(
                SpanStyle(color = nickColor, fontWeight = FontWeight.Bold, fontSize = textSize),
            ) { append(change.actorName) }
            withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                append(" ")
                append(actionText)
            }
            if (emotePart != null) {
                append(" ")
                val emoteAspect = resolvedEmoteAspectRatio(emotePart.url, emotePart.aspectRatio)
                appendInlineContent("emote", emotePart.name)
                inlineContent["emote"] = InlineTextContent(
                    Placeholder(
                        emoteHeightSp * emoteAspect,
                        emoteHeightSp,
                        PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    SharedEmoteImage(
                        url = emotePart.url,
                        contentDescription = emotePart.name,
                        size = emoteSize,
                        aspectRatio = emoteAspect,
                    )
                }
            }
            if (nameLabel.isNotBlank()) {
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                    append(" (")
                    append(nameLabel)
                    append(")")
                }
            }
        }
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        color = TwitchTextSecondary,
        fontSize = textSize,
        lineHeight = lineHeight,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stripe = 3.dp.toPx()
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - stripe else 0f
                drawRect(
                    color = stripeColor,
                    topLeft = Offset(x, 0f),
                    size = Size(stripe, size.height),
                )
            }
            .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
    )
}
