package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
    val nickColor = rememberActorNickColor(change.actorColor, readableColors)
    val actionText = emoteChangeActionText(change.action)
    val platformLabel = emotePlatformLabel(change.platform)
    val emotePart = emoteChangePart(change)
    val nameLabel = emoteChangeNameLabel(change)
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
            if (logo != null) inlineContent.putEmote("logo", logo, platformLabel, emoteSize, emoteHeightSp)
            appendEmotePlatform(platformLabel, hasLogo = logo != null, textSize)
            append(" ")
            appendChatTimestamp(timestampText, timestampSize)
            withStyle(SpanStyle(color = nickColor, fontWeight = FontWeight.Bold, fontSize = textSize)) {
                append(change.actorName)
            }
            withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                append(" ")
                append(actionText)
            }
            if (emotePart != null) {
                append(" ")
                appendInlineContent("emote", emotePart.name)
                inlineContent.putEmote("emote", emotePart, emotePart.name, emoteSize, emoteHeightSp)
            }
            appendEmoteNameLabel(nameLabel, textSize)
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
            .chatStartStripe(stripeColor)
            .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
    )
}

/** The emote service's logo, put in the inline content under "logo", or its name where it has no logo. */
private fun AnnotatedString.Builder.appendEmotePlatform(platformLabel: String, hasLogo: Boolean, textSize: TextUnit) {
    if (hasLogo) {
        appendInlineContent("logo", platformLabel)
        return
    }
    withStyle(SpanStyle(color = TwitchTextSecondary, fontWeight = FontWeight.Medium, fontSize = textSize)) {
        append(platformLabel)
    }
}

/** The emote's name in parentheses after its picture; nothing for a change without a name. */
private fun AnnotatedString.Builder.appendEmoteNameLabel(nameLabel: String, textSize: TextUnit) {
    if (nameLabel.isBlank()) return
    withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
        append(" (")
        append(nameLabel)
        append(")")
    }
}

/** The actor's chat color, made readable on the dark chat when asked; white for one without a color. */
@Composable
private fun rememberActorNickColor(actorColor: Color, readableColors: Boolean): Color {
    val knownNickColor = actorColor.takeUnless {
        it == Color.Unspecified || it == Color.White
    }
    return remember(knownNickColor, readableColors) {
        val color = knownNickColor ?: Color.White
        if (knownNickColor != null && readableColors) {
            ReadableChatColor.adjust(color, TwitchBg)
        } else {
            color
        }
    }
}

@Composable
private fun emoteChangeActionText(action: EmoteChangeAction): String = when (action) {
    EmoteChangeAction.Added -> stringResource(R.string.chat_emote_added)
    EmoteChangeAction.Removed -> stringResource(R.string.chat_emote_removed)
    EmoteChangeAction.Renamed -> stringResource(R.string.chat_emote_renamed)
}

@Composable
private fun emotePlatformLabel(platform: EmotePlatform): String = when (platform) {
    EmotePlatform.SevenTv -> stringResource(R.string.chat_emote_platform_7tv)
    EmotePlatform.Bttv -> stringResource(R.string.chat_emote_platform_bttv)
    EmotePlatform.Ffz -> stringResource(R.string.chat_emote_platform_ffz)
}

/** The changed emote's picture, named as the change names it; null when the change carries no picture. */
private fun emoteChangePart(change: ChatEmoteChange): ChatPart.Emote? = change.emote?.let { emote ->
    ChatPart.Emote(
        name = change.emoteName.ifBlank { emote.id.orEmpty() },
        url = emote.url,
        aspectRatio = emote.aspectRatio,
    )
}

/** The emote's name, or "old → new" for a rename. */
private fun emoteChangeNameLabel(change: ChatEmoteChange): String = buildString {
    val shownName = change.emoteName.ifBlank { change.emoteId.orEmpty() }
    if (change.previousName != null && change.previousName != shownName) {
        append(change.previousName)
        append(" → ")
        append(shownName)
    } else {
        append(shownName)
    }
}

/** Puts the [emote]'s picture under [key], one line of chat text tall, for the text's inline content. */
private fun MutableMap<String, InlineTextContent>.putEmote(
    key: String,
    emote: ChatPart.Emote,
    description: String,
    size: Dp,
    heightSp: TextUnit,
) {
    val aspect = resolvedEmoteAspectRatio(emote.url, emote.aspectRatio)
    this[key] = InlineTextContent(Placeholder(heightSp * aspect, heightSp, PlaceholderVerticalAlign.TextCenter)) {
        SharedEmoteImage(url = emote.url, contentDescription = description, size = size, aspectRatio = aspect)
    }
}
