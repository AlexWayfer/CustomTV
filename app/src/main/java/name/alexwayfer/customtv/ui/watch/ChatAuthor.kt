package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.shownBadges

@Composable
internal fun ChatNameWithBadges(
    displayName: String,
    color: Color,
    badges: List<ChatBadge>,
    badgeUrls: Map<String, String>,
    textSize: TextUnit,
    badgeSize: Dp,
    modifier: Modifier = Modifier,
    /** The chatter whose labels, third-party badges, and paint show; null shows none of them. */
    userId: String? = null,
    topChatterLogin: String? = null,
) {
    val cosmetics = rememberChatterCosmetics(userId)
    val paint = rememberNamePaintTextStyle(cosmetics.paint, color)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopChatterNameIcon(login = topChatterLogin, badgeSize = badgeSize)
        shownBadges(badges, badgeUrls, cosmetics.badges).forEach { badge ->
            AsyncImage(
                model = badge.url,
                contentDescription = badge.description,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(badgeSize)
                    .ffzModBadgeBackground(badge.url)
                    .badgeBackground(badge.background),
                contentScale = ContentScale.Fit,
            )
        }
        ChatterLabelIcons(userId = userId, badgeSize = badgeSize)
        if (paint == null) {
            Text(
                text = displayName,
                color = color,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            // Material's Text would put the color over the brush. Not filling the row, the paint spans only the name.
            BasicText(
                text = displayName,
                style = LocalTextStyle.current.merge(TextStyle(fontSize = textSize, fontWeight = FontWeight.SemiBold))
                    .merge(TextStyle(brush = paint.brush, shadow = paint.shadow)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
