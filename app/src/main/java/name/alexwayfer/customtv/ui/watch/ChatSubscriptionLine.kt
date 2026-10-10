package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatSubscriptionLine(
    message: ChatMessage,
    text: String,
    timestampText: String?,
    nickColor: Color,
    chatTextSize: Int,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
) {
    val iconSize = chatDp(chatTextSize, 15, 18)
    val density = LocalDensity.current
    val iconWidth = with(density) { (iconSize + 5.dp).toSp() }
    val iconHeight = with(density) { iconSize.toSp() }
    val annotated = remember(
        message.displayName,
        text,
        timestampText,
        nickColor,
        textSize,
        timestampSize,
    ) {
        buildAnnotatedString {
            appendInlineContent("subscription", "sub")
            if (timestampText != null) {
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = timestampSize)) {
                    append(timestampText)
                    append(" ")
                }
            }
            val nameStart = text.indexOf(message.displayName, ignoreCase = true)
            if (nameStart < 0 || message.displayName.isBlank()) {
                withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                    append(text)
                }
                return@buildAnnotatedString
            }
            withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                append(text.substring(0, nameStart))
            }
            withStyle(
                SpanStyle(
                    color = nickColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize,
                ),
            ) {
                append(text.substring(nameStart, nameStart + message.displayName.length))
            }
            withStyle(SpanStyle(color = TwitchTextSecondary, fontSize = textSize)) {
                append(text.substring(nameStart + message.displayName.length))
            }
        }
    }
    Text(
        text = annotated,
        lineHeight = lineHeight,
        inlineContent = mapOf(
            "subscription" to InlineTextContent(
                Placeholder(iconWidth, iconHeight, PlaceholderVerticalAlign.TextCenter),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Icon(
                        painter = if (message.primeSubscription) painterResource(R.drawable.ic_chat_prime_crown)
                        else rememberVectorPainter(Icons.Filled.Star),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize),
                    )
                }
            },
        ),
    )
}
