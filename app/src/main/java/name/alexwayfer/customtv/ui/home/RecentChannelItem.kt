package name.alexwayfer.customtv.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.LocalGestureHints
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

@Composable
internal fun RecentChannelItem(
    channel: String,
    refreshedProfile: ChannelProfile?,
    onWatch: (live: Boolean) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { RecentDeleteWidth.toPx() }
    val offsetX = remember(channel) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val gestureHints = LocalGestureHints.current
    var menuOpen by remember { mutableStateOf(false) }
    val removeLabel = stringResource(R.string.remove_recent)

    fun settle(velocity: Float) {
        val open = if (velocity < -700f) {
            true
        } else if (velocity > 700f) {
            false
        } else {
            offsetX.value < -actionWidthPx * 0.35f
        }
        if (open) gestureHints?.gestureUsed(GestureHint.RemoveRecent)
        scope.launch {
            offsetX.animateTo(
                if (open) -actionWidthPx else 0f,
                tween(180),
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .draggable(
                state = rememberDraggableState { delta ->
                    val next = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                    scope.launch { offsetX.snapTo(next) }
                },
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity -> settle(velocity) },
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(removeLabel) {
                        onRemove()
                        true
                    },
                )
            },
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(RecentDeleteWidth)
                    // Hidden at rest: the anti-aliased rounded corners of the row above would let the red show through.
                    .graphicsLayer { alpha = if (offsetX.value < 0f) 1f else 0f }
                    .background(TwitchLive)
                    .clickable(
                        interactionSource = null,
                        indication = ripple(color = Color.White),
                        onClick = onRemove,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.remove_recent),
                    tint = Color.White,
                )
            }
        }
        RecentChannelRow(
            channel = channel,
            refreshedProfile = refreshedProfile,
            onClick = onWatch,
            onLongClick = { menuOpen = true },
            modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(removeLabel) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    gestureHints?.slowPathUsed(GestureHint.RemoveRecent)
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun RecentChannelRow(
    channel: String,
    refreshedProfile: ChannelProfile?,
    onClick: (live: Boolean) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val profile = refreshedProfile ?: remember(channel) {
        ChannelAvatarRepository.cached(channel) ?: ChannelProfile(
            login = channel.lowercase(),
            displayName = channel,
            avatarUrl = null,
        )
    }
    val displayName = profile.displayName
    val category = profile.categoryName.takeIf { profile.isLive }
    val viewerCount = profile.viewerCount.takeIf { profile.isLive }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TwitchSurface)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(color = Color.White),
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onClick = { onClick(profile.isLive) },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelAvatarWithCollaborators(
            channel = channel,
            profile = profile,
            collaboratorAvatarUrls = if (profile.isLive) profile.collaboratorAvatarUrls else emptyList(),
            ringColor = TwitchSurface,
            size = 40.dp,
            grayscale = !profile.isLive,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            ChannelListTitle(
                displayName = displayName,
                login = channel,
                collaboratorCount = if (profile.isLive) profile.collaborationCount else null,
            )
            if (category != null) {
                Text(
                    text = category,
                    color = TwitchTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (viewerCount != null) {
            ChannelListViewers(
                viewerCount = viewerCount,
                sharedViewerCount = profile.sharedViewerCount,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private val RecentDeleteWidth = 72.dp
