package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.CategoryViewers
import name.alexwayfer.customtv.ui.rememberMinuteClock
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import name.alexwayfer.customtv.ui.theme.TwitchLive

@Composable
internal fun ChannelSearchLastLive(endedAtMillis: Long) {
    val label = followedLastBroadcastLabel(endedAtMillis, rememberMinuteClock())
    val description = stringResource(R.string.following_last_live, label)
    Text(
        text = label,
        color = TwitchTextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Composable
internal fun ChannelSearchLiveStatus(viewers: Int?) {
    if (viewers != null) {
        CategoryViewers(viewers)
    } else {
        val description = stringResource(R.string.search_history_live)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TwitchLive)
                .clearAndSetSemantics { contentDescription = description },
        )
    }
}
