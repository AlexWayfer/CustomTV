package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatShoutoutNotice
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** Stands in the line's text for the channel's name, which is put back as a link that opens the channel's card. */
private const val TARGET_MARK = "\u0000"

/** A shoutout's line: a megaphone and who gave a shoutout to which channel, whose name opens the channel's card. */
@Composable
internal fun ChatShoutoutLine(
    notice: ChatShoutoutNotice,
    timestampText: String?,
    textSize: TextUnit,
    lineHeight: TextUnit,
    badgeUrls: Map<String, String>,
) {
    val openChatterCard by rememberUpdatedState(LocalOpenChatterCard.current)
    val currentBadgeUrls by rememberUpdatedState(badgeUrls)
    val template = notice.sourceName?.let { stringResource(R.string.chat_shoutout_description, it, TARGET_MARK) }
        ?: stringResource(R.string.chat_shoutout_description_anonymous, TARGET_MARK)
    val annotated = remember(template, notice, timestampText) {
        val nameStyle = SpanStyle(
            color = TwitchText,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.None,
        )
        val link = LinkAnnotation.Clickable(
            tag = notice.targetLogin,
            styles = TextLinkStyles(
                style = nameStyle,
                focusedStyle = nameStyle,
                hoveredStyle = nameStyle,
                pressedStyle = nameStyle,
            ),
            linkInteractionListener = {
                openChatterCard(
                    ChatterCardRequest(
                        login = notice.targetLogin,
                        displayName = notice.targetName,
                        userId = notice.targetId,
                        badges = emptyList(),
                        badgeUrls = currentBadgeUrls,
                        badgesKnown = false,
                    ),
                )
            },
        )
        buildAnnotatedString {
            timestampText?.let { append("$it ") }
            val parts = template.split(TARGET_MARK, limit = 2)
            append(parts[0])
            withLink(link) { append(notice.targetName) }
            parts.getOrNull(1)?.let(::append)
        }
    }
    val iconSize = with(LocalDensity.current) { textSize.toDp() }
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_megaphone),
            contentDescription = null,
            tint = TwitchTextSecondary,
            modifier = Modifier
                .padding(top = 2.dp, end = 4.dp)
                .size(iconSize),
        )
        Text(
            text = annotated,
            color = TwitchTextSecondary,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            lineHeight = lineHeight,
        )
    }
}
