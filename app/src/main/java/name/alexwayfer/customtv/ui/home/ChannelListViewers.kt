package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.SharedViewerCount
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

/** A live channel's viewers in a channel list, with the shared audience on a second line under them. */
@Composable
internal fun ChannelListViewers(
    viewerCount: Int,
    sharedViewerCount: Int?,
    modifier: Modifier = Modifier,
) {
    val formattedViewers = NumberFormat.getIntegerInstance().format(viewerCount)
    val viewersDescription = if (sharedViewerCount != null) {
        stringResource(
            R.string.stream_viewers_with_shared,
            formattedViewers,
            NumberFormat.getIntegerInstance().format(sharedViewerCount),
        )
    } else {
        pluralStringResource(R.plurals.stream_viewers, viewerCount, formattedViewers)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics { contentDescription = viewersDescription },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TwitchLive),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = formattedViewers,
                color = TwitchText,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        if (sharedViewerCount != null) {
            SharedViewerCount(sharedViewerCount, TwitchTextSecondary, parenthesized = false)
        }
    }
}
