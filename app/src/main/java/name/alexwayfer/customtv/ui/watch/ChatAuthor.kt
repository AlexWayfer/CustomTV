package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.chat.ChatBadge

@Composable
internal fun ChatNameWithBadges(
    displayName: String,
    color: Color,
    badges: List<ChatBadge>,
    badgeUrls: Map<String, String>,
    textSize: TextUnit,
    badgeSize: Dp,
    modifier: Modifier = Modifier,
    labelsUserId: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badges.forEach { badge ->
            val url = badgeUrls[badge.key] ?: return@forEach
            AsyncImage(
                model = url,
                contentDescription = badge.setId,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(badgeSize)
                    .ffzModBadgeBackground(url),
                contentScale = ContentScale.Fit,
            )
        }
        ChatterLabelIcons(userId = labelsUserId, badgeSize = badgeSize)
        Text(
            text = displayName,
            color = color,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
