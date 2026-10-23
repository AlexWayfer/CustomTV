package name.alexwayfer.customtv.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.components.CategoryHideMenu
import name.alexwayfer.customtv.ui.components.categoryMenuLabel
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** A live channel's row, whose long press offers to hide its category, or to show it again. */
@Composable
internal fun FollowedLiveItem(channel: FollowedChannel, onWatch: () -> Unit, modifier: Modifier = Modifier) {
    val category = followedCategory(channel)
    val menuLabel = categoryMenuLabel(category)
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier) {
        FollowedLiveChannelRow(
            channel = channel,
            onWatch = onWatch,
            onLongClick = menuLabel?.let { { menuOpen = true } },
            onLongClickLabel = menuLabel,
        )
        CategoryHideMenu(category, expanded = menuOpen, onDismiss = { menuOpen = false })
    }
}

/** The category a live channel streams in, when the follows list named it by ID. */
internal fun followedCategory(channel: FollowedChannel): TwitchCategory? {
    val id = channel.categoryId ?: return null
    val name = channel.categoryName ?: return null
    return TwitchCategory(name = name, boxArtUrl = null, id = id)
}

/**
 * The line under the live channels that says how many stream in a hidden category; a tap shows them under it, and
 * another hides them again.
 */
@Composable
internal fun FollowedHiddenLine(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "hidden streams arrow")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = null, indication = ripple(color = Color.White), onClick = onToggle)
            .semantics {
                if (expanded) {
                    collapse { onToggle(); true }
                } else {
                    expand { onToggle(); true }
                }
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.followed_hidden_streams, count, count),
            color = TwitchTextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = TwitchTextSecondary,
            modifier = Modifier.rotate(turn),
        )
    }
}
