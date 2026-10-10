package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.components.revealLines
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

@Composable
internal fun StreamInfoPanel(
    channel: String,
    profile: ChannelProfile,
    title: String?,
    tags: List<String>,
    categoryName: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenProfile: () -> Unit,
    meta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: @Composable () -> Unit = {},
    containerColor: Color = TwitchSurface,
) {
    val displayName = profile.displayName
    val transition = updateTransition(expanded, label = "channelHeader")
    // The parts fade at the pace they move, so they are seen sliding in and out rather than popping. The progress is
    // read only in layout and drawing, so a frame of the motion does not recompose the panel.
    val sizeProgress = transition.animateFloat(
        transitionSpec = { tween(HEADER_SIZE_MS, easing = HeaderPanelEasing) },
        label = "headerSize",
    ) { visible -> if (visible) 1f else 0f }
    val showExpandedContent = transition.currentState || transition.targetState
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            // What slides up out of the panel goes under its top edge, not under its padding.
            .clipToBounds()
            .clickable(onClick = onToggle)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (showExpandedContent) {
            ChannelAvatar(
                channel = channel,
                profile = profile,
                size = HeaderAvatarSize,
                modifier = Modifier
                    .headerAvatarReveal { sizeProgress.value }
                    .clickable(role = Role.Button, onClick = onOpenProfile)
                    .padding(end = HeaderAvatarEndPadding),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (showExpandedContent) {
                Column(modifier = Modifier.headerRevealUp { sizeProgress.value }) {
                    // The trailing action takes room from the name and the title; the tags run under it.
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenProfile),
                                color = TwitchText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            title?.let { title ->
                                Text(text = title, color = TwitchText, fontSize = 13.sp)
                            }
                        }
                        trailingAction()
                    }
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            tags.forEach { tag ->
                                Text(
                                    text = tag,
                                    color = TwitchText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 20.sp,
                                    style = TextStyle(
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.None,
                                        ),
                                    ),
                                    modifier = Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(percent = 50),
                                        )
                                        .padding(horizontal = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
            // Baselines line up, so with a category over several lines the meta stays beside its first line.
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                val bottomLabel = categoryName ?: displayName.takeIf { !showExpandedContent }
                // A recording's category changes with its chapters: the old one fades out as the new one
                // fades in, and the row takes the new height at the panel's own pace.
                AnimatedContent(
                    targetState = bottomLabel,
                    modifier = Modifier
                        .weight(1f, fill = bottomLabel != null)
                        .alignByBaseline(),
                    transitionSpec = {
                        (HeaderFadeIn togetherWith HeaderFadeOut).using(
                            SizeTransform { _, _ -> tween(HEADER_SIZE_MS, easing = HeaderEmphasizedDecelerate) },
                        )
                    },
                    label = "headerBottomLabel",
                ) { label ->
                    if (label == null) return@AnimatedContent
                    // Every line stays laid out while the panel moves, and the panel's progress reveals them
                    // from the first line down, so a closing panel cuts them off as it shrinks, not at once.
                    var firstLineBottomPx by remember { mutableIntStateOf(0) }
                    Text(
                        text = label,
                        color = TwitchTextSecondary,
                        fontSize = 12.sp,
                        maxLines = collapsibleTextMaxLines(showExpandedContent),
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { layout -> firstLineBottomPx = layout.getLineBottom(0).roundToInt() },
                        modifier = Modifier
                            .revealLines({ firstLineBottomPx }, { sizeProgress.value })
                            .padding(end = 8.dp),
                    )
                }
                Box(modifier = Modifier.alignByBaseline()) { meta() }
            }
        }
    }
}

@Composable
internal fun ChannelHeader(
    channel: String,
    profile: ChannelProfile,
    expanded: Boolean,
    onToggle: () -> Unit,
    onViewersClick: (() -> Unit)?,
    onOpenProfile: () -> Unit,
    followStatus: ChannelFollowStatus,
    followAction: ChannelFollowAction,
    onOpenSettings: () -> Unit,
    onHeldChange: (Boolean) -> Unit,
    containerColor: Color = TwitchSurface,
    showTags: Boolean = true,
) {
    StreamInfoPanel(
        channel = channel,
        profile = profile,
        title = profile.streamTitle,
        tags = if (showTags) profile.tags else emptyList(),
        categoryName = profile.categoryName,
        expanded = expanded,
        onToggle = onToggle,
        onOpenProfile = onOpenProfile,
        meta = {
            StreamLiveMeta(
                profile.streamStartedAtMillis,
                profile.viewerCount,
                profile.sharedViewerCount,
                onViewersClick = onViewersClick,
            )
        },
        trailingAction = {
            // The heart shows once the follow is known, and the bell beside it only while the channel is followed.
            Row {
                AnimatedVisibility(
                    visible = followStatus != ChannelFollowStatus.Unknown,
                    enter = HeaderFadeIn + expandIn(
                        tween(HEADER_SIZE_MS, easing = HeaderEmphasizedDecelerate),
                        expandFrom = Alignment.TopEnd,
                    ),
                    exit = HeaderFadeOut + shrinkOut(
                        tween(HEADER_SIZE_MS, easing = HeaderEmphasizedAccelerate),
                        shrinkTowards = Alignment.TopEnd,
                    ),
                ) {
                    ChannelFollowButton(followStatus == ChannelFollowStatus.Following, followAction)
                }
                AnimatedVisibility(
                    visible = followStatus == ChannelFollowStatus.Following,
                    enter = HeaderFadeIn + expandIn(
                        tween(HEADER_SIZE_MS, easing = HeaderEmphasizedDecelerate),
                        expandFrom = Alignment.TopEnd,
                    ),
                    exit = HeaderFadeOut + shrinkOut(
                        tween(HEADER_SIZE_MS, easing = HeaderEmphasizedAccelerate),
                        shrinkTowards = Alignment.TopEnd,
                    ),
                ) {
                    ChannelAlertsButton(profile.id, onOpenSettings, onHeldChange)
                }
            }
        },
        containerColor = containerColor,
    )
}

/**
 * The avatar slides up out of its slot and back down as the slot closes and opens, keeping its width while it goes.
 */
private fun Modifier.headerAvatarReveal(progress: () -> Float): Modifier =
    layout { measurable, constraints ->
        val fraction = progress()
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * fraction).roundToInt().coerceAtLeast(0)
        layout((placeable.width * fraction).roundToInt().coerceAtLeast(0), height) {
            placeable.placeRelative(0, height - placeable.height)
        }
    }
    .graphicsLayer { alpha = progress() }

/**
 * Slides up out of its box as the box shrinks, and back down as it grows. The details keep their place and width
 * beside the full avatar, so they neither follow the avatar nor rewrap while it moves.
 */
private fun Modifier.headerRevealUp(progress: () -> Float): Modifier =
    layout { measurable, constraints ->
        val fraction = progress()
        val reservePx = (HeaderAvatarSlotWidth.toPx() * (1f - fraction)).roundToInt()
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = (constraints.minWidth - reservePx).coerceAtLeast(0),
                maxWidth = (constraints.maxWidth - reservePx).coerceAtLeast(0),
            ),
        )
        val height = (placeable.height * fraction).roundToInt().coerceAtLeast(0)
        layout((placeable.width + reservePx).coerceAtMost(constraints.maxWidth), height) {
            placeable.placeRelative(reservePx, height - placeable.height)
        }
    }
    .graphicsLayer { alpha = progress() }

private val HeaderAvatarSize = 40.dp
private val HeaderAvatarEndPadding = 10.dp
private val HeaderAvatarSlotWidth = HeaderAvatarSize + HeaderAvatarEndPadding
