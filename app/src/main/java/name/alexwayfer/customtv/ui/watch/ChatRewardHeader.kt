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
import androidx.compose.ui.unit.em
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.displayNameLoginSuffix
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchText
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
                appendChatTimestamp(headerTimestamp, timestampSize)
                appendRedeemer(message.displayName, message.userLogin, verb, bodySize)
            } else {
                withStyle(SpanStyle(color = TwitchText, fontSize = bodySize)) {
                    append(headerPrefix)
                    append(" ")
                }
            }
            appendRewardTitleAndCost(title, costLabel, bodySize)
        }
    }
    val inlineContent = remember(pointsIconUrl, iconSize) { rewardPointsInlineContent(pointsIconUrl, iconSize) }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        lineHeight = lineHeight,
        modifier = Modifier.padding(bottom = if (hasUserInput) 2.dp else 0.dp),
    )
}

/** "[name] ([login]) [verb] ": who redeemed the reward, the name in bold. */
private fun AnnotatedString.Builder.appendRedeemer(
    displayName: String,
    login: String,
    verb: String,
    bodySize: TextUnit,
) {
    withStyle(SpanStyle(color = TwitchText, fontWeight = FontWeight.Bold, fontSize = bodySize)) {
        append(displayName)
    }
    displayNameLoginSuffix(displayName, login)?.let { suffix ->
        withStyle(SpanStyle(color = TwitchText, fontWeight = FontWeight.Normal, fontSize = bodySize)) {
            append(suffix)
        }
    }
    withStyle(SpanStyle(color = TwitchText, fontSize = bodySize)) {
        append(" ")
        append(verb)
        append(" ")
    }
}

/** The reward's title in bold, then the channel points icon and its cost. */
private fun AnnotatedString.Builder.appendRewardTitleAndCost(title: String, costLabel: String, bodySize: TextUnit) {
    withStyle(SpanStyle(color = TwitchText, fontWeight = FontWeight.Bold, fontSize = bodySize)) {
        append(title)
    }
    append(" ")
    appendInlineContent("points", "pts")
    withStyle(SpanStyle(color = TwitchText, fontWeight = FontWeight.SemiBold, fontSize = bodySize)) {
        append("\u00A0")
        append(costLabel)
    }
}

/** The channel's own channel points icon, or Twitch's. */
private fun rewardPointsInlineContent(pointsIconUrl: String?, iconSize: Dp): Map<String, InlineTextContent> = mapOf(
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
