package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.displayNameLabel
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatWatchStreak
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchWatchStreak
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

internal fun watchStreakTimestampOnUserMessage(hasUserText: Boolean): Boolean = hasUserText

@Composable
internal fun ChatWatchStreakLine(
    message: ChatMessage,
    streak: ChatWatchStreak,
    timestampText: String?,
    pointsIconUrl: String?,
    chatTextSize: Int,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
) {
    val name = displayNameLabel(message.displayName, message.userLogin)
    val body = pluralStringResource(
        R.plurals.chat_watch_streak,
        streak.consecutiveStreams,
        name,
        streak.consecutiveStreams,
    )
    val pointsLabel = streak.points?.let { points ->
        "+${NumberFormat.getIntegerInstance().format(points)}"
    }
    val iconSize = chatDp(chatTextSize, 12, 14)
    val annotated = remember(timestampText, name, body, pointsLabel, textSize, timestampSize) {
        buildAnnotatedString {
            appendChatTimestamp(timestampText, timestampSize)
            appendInlineContent("streak", "streak")
            withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) { append(" ") }
            appendWithChatterName(body, name, Color.White, textSize)
            if (pointsLabel != null) {
                append(" ")
                appendInlineContent("points", "pts")
                withStyle(
                    SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize,
                    ),
                ) {
                    append("\u00A0")
                    append(pointsLabel)
                }
            }
        }
    }
    val inlineContent = remember(pointsIconUrl, iconSize, pointsLabel) {
        watchStreakInlineContent(pointsIconUrl, iconSize, withPoints = pointsLabel != null)
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        lineHeight = lineHeight,
        modifier = Modifier.padding(bottom = if (message.parts.isNotEmpty()) 2.dp else 0.dp),
    )
}

/** The streak's flame, and the channel points icon before the bonus [withPoints]: the channel's own or Twitch's. */
private fun watchStreakInlineContent(
    pointsIconUrl: String?,
    iconSize: Dp,
    withPoints: Boolean,
): Map<String, InlineTextContent> = buildMap {
    put(
        "streak",
        InlineTextContent(
            Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextBottom),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chat_watch_streak),
                contentDescription = null,
                tint = TwitchWatchStreak,
                modifier = Modifier.size(iconSize),
            )
        },
    )
    if (withPoints) {
        put(
            "points",
            InlineTextContent(
                Placeholder(1.em, 1.em, PlaceholderVerticalAlign.AboveBaseline),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (!pointsIconUrl.isNullOrBlank()) {
                        SharedEmoteImage(
                            url = pointsIconUrl,
                            contentDescription = null,
                            size = iconSize,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_chat_channel_points),
                            contentDescription = null,
                            tint = TwitchTextSecondary,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }
            },
        )
    }
}
