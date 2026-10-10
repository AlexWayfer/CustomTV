package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import name.alexwayfer.customtv.channel.displayNameLoginSuffix
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

@Composable
internal fun ChatRewardRedeemedHeader(
    message: ChatMessage,
    title: String,
    cost: Int,
    hasUserInput: Boolean,
    timestampText: String?,
    pointsIconUrl: String?,
    chatTextSize: Int,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
) {
    val verb = stringResource(R.string.chat_reward_redeemed)
    val headerPrefix = stringResource(R.string.chat_reward_header)
    val costLabel = NumberFormat.getIntegerInstance().format(cost)
    val bodySize = if (hasUserInput) timestampSize else textSize
    val iconSize = chatDp(chatTextSize, 12, 14)
    val headerTimestamp = timestampText.takeIf { !hasUserInput }
    val annotated = remember(
        hasUserInput,
        headerTimestamp,
        message.displayName,
        message.userLogin,
        verb,
        headerPrefix,
        title,
        costLabel,
        bodySize,
        timestampSize,
    ) {
        buildAnnotatedString {
            if (!hasUserInput) {
                if (headerTimestamp != null) {
                    withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = timestampSize)) {
                        append(headerTimestamp)
                        append(" ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = TwitchText,
                        fontWeight = FontWeight.Bold,
                        fontSize = bodySize,
                    ),
                ) {
                    append(message.displayName)
                }
                displayNameLoginSuffix(message.displayName, message.userLogin)?.let { suffix ->
                    withStyle(
                        SpanStyle(
                            color = TwitchText,
                            fontWeight = FontWeight.Normal,
                            fontSize = bodySize,
                        ),
                    ) {
                        append(suffix)
                    }
                }
                withStyle(SpanStyle(color = TwitchText, fontSize = bodySize)) {
                    append(" ")
                    append(verb)
                    append(" ")
                }
            } else {
                withStyle(SpanStyle(color = TwitchText, fontSize = bodySize)) {
                    append(headerPrefix)
                    append(" ")
                }
            }
            withStyle(
                SpanStyle(
                    color = TwitchText,
                    fontWeight = FontWeight.Bold,
                    fontSize = bodySize,
                ),
            ) {
                append(title)
            }
            append(" ")
            appendInlineContent("points", "pts")
            withStyle(
                SpanStyle(
                    color = TwitchText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = bodySize,
                ),
            ) {
                append("\u00A0")
                append(costLabel)
            }
        }
    }
    val inlineContent = remember(pointsIconUrl, iconSize) {
        mapOf(
            "points" to InlineTextContent(
                Placeholder(1.em, 1.em, PlaceholderVerticalAlign.Center),
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
                        tint = TwitchText,
                        modifier = Modifier.size(iconSize),
                    )
                }
            },
        )
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        lineHeight = lineHeight,
        modifier = Modifier.padding(bottom = if (hasUserInput) 2.dp else 0.dp),
    )
}
