package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.data.showsFollowButton
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.components.StreamTagPill
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.components.revealLines
import name.alexwayfer.customtv.ui.theme.ChatLink
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
    category: TwitchCategory?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenProfile: () -> Unit,
    /** Opens the category in Explore from the panel shown whole; null where Explore is missing. */
    onOpenCategory: ((TwitchCategory) -> Unit)?,
    meta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: @Composable () -> Unit = {},
    subtitle: String? = null,
    /** Room the content keeps beyond its own padding, such as from the screen's rounded corners. */
    contentPadding: PaddingValues = PaddingValues(),
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
            .background(TwitchSurface)
            // What slides up out of the panel goes under its top edge, not under its padding.
            .clipToBounds()
            .clickable(onClick = onToggle)
            .padding(contentPadding)
            .padding(start = 12.dp, end = 12.dp, top = StreamInfoTopPadding, bottom = 12.dp),
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
                        HeaderNameAndTitle(displayName, title, onOpenProfile, Modifier.weight(1f))
                        trailingAction()
                    }
                    subtitle?.let { line -> HeaderSubtitle(line) }
                    if (tags.isNotEmpty()) StreamTags(tags)
                }
            }
            // Baselines line up, so with a category over several lines the meta stays beside its first line.
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                HeaderBottomLabel(
                    label = category?.name ?: displayName.takeIf { !showExpandedContent },
                    expanded = showExpandedContent,
                    progress = { sizeProgress.value },
                    onClick = categoryClick(category, expanded, onOpenCategory),
                )
                Box(modifier = Modifier.alignByBaseline()) { meta() }
            }
        }
    }
}

/** A line about what plays, such as who cut a clip, under the title of the open panel. */
@Composable
private fun HeaderSubtitle(line: String) {
    Text(
        text = line,
        modifier = Modifier.padding(top = 2.dp),
        color = TwitchTextSecondary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A tap on the category line opens it in Explore; without a click of its own the tap goes to the panel. */
private fun categoryClick(
    category: TwitchCategory?,
    expanded: Boolean,
    onOpenCategory: ((TwitchCategory) -> Unit)?,
): (() -> Unit)? {
    val openable = panelCategoryToOpen(category, expanded) ?: return null
    if (onOpenCategory == null) return null
    return { onOpenCategory(openable) }
}

@Composable
private fun HeaderNameAndTitle(
    displayName: String,
    title: String?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = displayName,
            modifier = Modifier.clickable(role = Role.Button, onClick = onOpenProfile),
            color = TwitchText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        title?.let { streamTitle ->
            Text(text = streamTitle, color = TwitchText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StreamTags(tags: List<String>) {
    FlowRow(
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag -> StreamTagPill(tag) }
    }
}

/**
 * The category under the title, or the name in the collapsed panel; [progress] is the panel's opening. [onClick] opens
 * the category; without it a tap goes to the panel.
 */
@Composable
private fun RowScope.HeaderBottomLabel(
    label: String?,
    expanded: Boolean,
    progress: () -> Float,
    onClick: (() -> Unit)?,
) {
    val openLabel = stringResource(R.string.stream_category_open)
    // A category that opens is in the link color; it fades as the panel closes and the tap goes to the panel.
    val color by animateColorAsState(
        targetValue = if (onClick != null) ChatLink else TwitchTextSecondary,
        animationSpec = tween(HEADER_SIZE_MS, easing = HeaderPanelEasing),
        label = "headerBottomLabelColor",
    )
    // A recording's category changes with its chapters: the old one fades out as the new one
    // fades in, and the row takes the new height at the panel's own pace.
    AnimatedContent(
        targetState = label,
        modifier = Modifier
            .weight(1f, fill = label != null)
            .alignByBaseline(),
        transitionSpec = {
            (HeaderFadeIn togetherWith HeaderFadeOut).using(
                SizeTransform { _, _ -> tween(HEADER_SIZE_MS, easing = HeaderEmphasizedDecelerate) },
            )
        },
        label = "headerBottomLabel",
    ) { shownLabel ->
        if (shownLabel == null) return@AnimatedContent
        // Every line stays laid out while the panel moves, and the panel's progress reveals them
        // from the first line down, so a closing panel cuts them off as it shrinks, not at once.
        var firstLineBottomPx by remember { mutableIntStateOf(0) }
        Text(
            text = shownLabel,
            color = color,
            fontSize = 12.sp,
            maxLines = collapsibleTextMaxLines(expanded),
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> firstLineBottomPx = layout.getLineBottom(0).roundToInt() },
            // The row stretches the label to the meta; the tap and its ripple keep to the text.
            modifier = Modifier
                .wrapContentWidth(Alignment.Start)
                .revealLines({ firstLineBottomPx }, progress)
                .padding(end = 8.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        )
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
    onOpenCategory: ((TwitchCategory) -> Unit)?,
    followStatus: ChannelFollowStatus,
    followAction: FollowAction,
    onOpenSettings: () -> Unit,
    onHeldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showTags: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
) {
    StreamInfoPanel(
        modifier = modifier,
        contentPadding = contentPadding,
        channel = channel,
        profile = profile,
        title = profile.streamTitle,
        tags = if (showTags) profile.tags else emptyList(),
        category = profile.categoryName?.let { name ->
            TwitchCategory(name, profile.categoryBoxArtUrl, profile.categoryId)
        },
        expanded = expanded,
        onToggle = onToggle,
        onOpenProfile = onOpenProfile,
        onOpenCategory = onOpenCategory,
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
                    visible = followStatus.showsFollowButton,
                    enter = HeaderFadeIn + expandIn(
                        tween(HEADER_SIZE_MS, easing = HeaderEmphasizedDecelerate),
                        expandFrom = Alignment.TopEnd,
                    ),
                    exit = HeaderFadeOut + shrinkOut(
                        tween(HEADER_SIZE_MS, easing = HeaderEmphasizedAccelerate),
                        shrinkTowards = Alignment.TopEnd,
                    ),
                ) {
                    FollowButton(followStatus == ChannelFollowStatus.Following, followAction)
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

/** How far the stream info's content sits below the panel's top. */
internal val StreamInfoTopPadding = 10.dp
private val HeaderAvatarSize = 40.dp
private val HeaderAvatarEndPadding = 10.dp
private val HeaderAvatarSlotWidth = HeaderAvatarSize + HeaderAvatarEndPadding
