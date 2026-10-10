package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.AutoModNotice
import name.alexwayfer.customtv.ui.theme.AutoModShield
import name.alexwayfer.customtv.ui.theme.ChatLink
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchText

/** What AutoMod tells the user about their own held message, under a red stripe as Twitch shows it. */
@Composable
internal fun ChatAutoModNoticeLine(
    notice: AutoModNotice,
    iconSize: Dp,
    textSize: TextUnit,
    lineHeight: TextUnit,
) {
    val text = stringResource(
        when (notice) {
            AutoModNotice.Checking -> R.string.chat_automod_checking
            AutoModNotice.Allowed -> R.string.chat_automod_allowed
            AutoModNotice.Removed -> R.string.chat_automod_removed
        },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stripe = 3.dp.toPx()
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - stripe else 0f
                drawRect(color = TwitchLive, topLeft = Offset(x, 0f), size = Size(stripe, size.height))
            }
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_moderation_shield),
                contentDescription = null,
                tint = AutoModShield,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(iconSize),
            )
            Text(
                text = "${stringResource(R.string.chat_automod)}:",
                color = ChatLink,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                lineHeight = lineHeight,
            )
        }
        Text(text = text, color = TwitchText, fontSize = textSize, lineHeight = lineHeight)
    }
}
