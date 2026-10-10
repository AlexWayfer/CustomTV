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
            if (timestampText != null) {
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = timestampSize)) {
                    append(timestampText)
                    append(" ")
                }
            }
            appendInlineContent("streak", "streak")
            val nameStart = body.indexOf(name, ignoreCase = true)
            withStyle(
                SpanStyle(
                    color = TwitchTextSecondary,
                    fontSize = textSize,
                ),
            ) {
                append(" ")
                if (nameStart < 0 || name.isBlank()) {
                    append(body)
                } else {
                    append(body.substring(0, nameStart))
                }
            }
            if (nameStart >= 0 && name.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = textSize,
                    ),
                ) {
                    append(body.substring(nameStart, nameStart + name.length))
                }
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                    append(body.substring(nameStart + name.length))
                }
            }
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
        buildMap {
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
            if (pointsLabel != null) {
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
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        lineHeight = lineHeight,
        modifier = Modifier.padding(bottom = if (message.parts.isNotEmpty()) 2.dp else 0.dp),
    )
}
