package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatterProfile
import name.alexwayfer.customtv.data.chatterSharedChatSourceRoomId
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt

/** Where the dark fade under the text starts, down the banner image. */
private const val BANNER_SCRIM_START = 0.45f

/** The source channel's avatar before "From X chat", about as tall as the line, and its gap to the text. */
private val SharedChatSourceAvatarSize = 16.dp
private val SharedChatSourceAvatarGap = 6.dp

/** How dark the mention button's round fill is over the banner. */
private const val MENTION_BUTTON_FILL_ALPHA = 0.45f

private val OnImageTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.9f),
    offset = Offset(0f, 1f),
    blurRadius = 14f,
)

/** The card's banner: a tap on the avatar opens the chatter's profile, and the @ button mentions them. */
@Composable
internal fun ChatterCardSheetHeader(
    profile: ChatterProfile?,
    chatter: ChatterCardChatter,
    collapse: ChatterHeaderCollapse,
    pinProgress: () -> Float,
    onDismiss: () -> Unit,
    onMention: (() -> Unit)?,
) {
    val openProfile = LocalOpenChatterProfile.current
    val sharedChatSource = sharedChatSourceLook(chatterSharedChatSourceRoomId(chatter.sourceMessage, chatter.messages))
    ChatterCardHeader(
        profile = profile,
        chatter = chatter,
        sharedChatSource = sharedChatSource,
        collapse = collapse,
        pinProgress = pinProgress,
        onOpenProfile = openProfile?.let { open ->
            {
                onDismiss()
                open(chatter.login)
            }
        },
        onMention = onMention,
    )
}

/**
 * The chatter's channel banner with their avatar and names over its bottom, folding as the profile scrolls.
 * [pinProgress] is 0 on the profile and 1 on the card's other pages, which keep the banner folded.
 */
@Composable
private fun ChatterCardHeader(
    profile: ChatterProfile?,
    chatter: ChatterCardChatter,
    /** The chat of another channel in a shared chat that the chatter writes from. */
    sharedChatSource: SharedChatSourceLook?,
    collapse: ChatterHeaderCollapse,
    pinProgress: () -> Float,
    onOpenProfile: (() -> Unit)?,
    onMention: (() -> Unit)?,
) {
    val imageUrl = profile?.bannerImageUrl
    // The gray fill holds the banner's place, so the card does not jump when the image arrives.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Read while placing, so the banner shrinks with the scroll without composing again.
            .layout { measurable, constraints ->
                val shown = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress())
                val height = chatterHeaderHeightPx(ChatterHeaderExpandedHeight.roundToPx(), shown)
                val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .background(TwitchSurfaceAlt),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
            contentDescription = imageUrl?.let { stringResource(R.string.chatter_card_channel_image) },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        BANNER_SCRIM_START to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .offset {
                    val maxShiftPx = (ChatterHeaderMargin - ChatterHeaderFoldedMargin).roundToPx()
                    val shown = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress())
                    IntOffset(0, chatterHeaderRowShiftPx(shown, collapse.rangePx, maxShiftPx))
                }
                // No top padding, and never squeezed by the banner: only the fold itself shrinks the avatar.
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .padding(start = ChatterHeaderMargin, end = ChatterHeaderMargin, bottom = ChatterHeaderMargin),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatterCardProfileButton(
                label = stringResource(R.string.chatter_card_open_profile),
                onOpenProfile = onOpenProfile,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                ChatterAvatar(profile?.avatarUrl, collapse, pinProgress)
                ChatterCardNames(chatter.name, chatter.login, sharedChatSource, collapse)
            }
            onMention?.let { ChatterMentionButton(it) }
        }
    }
}

/**
 * Mentions the chatter in the draft, beside their names. As tall as the folded avatar, so it never makes the row
 * taller; the dark fill keeps it seen on a light banner.
 */
@Composable
private fun ChatterMentionButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(ChatterAvatarFoldedSize),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = MENTION_BUTTON_FILL_ALPHA),
            contentColor = Color.White,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_mention),
            contentDescription = stringResource(R.string.chat_mention),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * The avatar and the names are one button. Its pill ripple hugs the round avatar on a layer under them, so the pill's
 * rounded end does not cut off a long line of the names.
 */
@Composable
private fun ChatterCardProfileButton(
    label: String,
    onOpenProfile: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .indication(interactionSource, ripple()),
        )
        Row(
            modifier = if (onOpenProfile != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = label,
                    role = Role.Button,
                    onClick = onOpenProfile,
                )
            } else {
                Modifier
            },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The display name, the login under it when it differs, and the chat of a shared chat they write from. */
@Composable
private fun ChatterCardNames(
    name: String,
    login: String,
    sharedChatSource: SharedChatSourceLook?,
    collapse: ChatterHeaderCollapse,
) {
    // The folded banner fits whichever is taller, the folded avatar or the names.
    Column(modifier = Modifier.onSizeChanged { collapse.fitNames(it.height) }) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.copy(shadow = OnImageTextShadow),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        if (!name.equals(login, ignoreCase = true)) {
            Text(
                text = login,
                style = MaterialTheme.typography.bodyMedium.copy(shadow = OnImageTextShadow),
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        sharedChatSource?.let { ChatterCardSharedChatSource(it) }
    }
}

/** The source channel's small avatar and "From X chat". */
@Composable
private fun ChatterCardSharedChatSource(source: SharedChatSourceLook) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SharedChatSourceAvatarGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A circle holds the place until the channel is known, as in the chat row.
        Box(
            modifier = Modifier
                .size(SharedChatSourceAvatarSize)
                .clip(CircleShape)
                .background(TwitchSurfaceAlt),
        ) {
            source.avatarUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = source.description,
            style = MaterialTheme.typography.bodyMedium.copy(shadow = OnImageTextShadow),
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ChatterAvatar(url: String?, collapse: ChatterHeaderCollapse, pinProgress: () -> Float) {
    val modifier = Modifier
        // Read while measuring, so the avatar shrinks with the banner without composing again.
        .layout { measurable, _ ->
            val size = chatterAvatarSizePx(
                collapsedPx = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress()),
                rangePx = collapse.rangePx,
                expandedPx = ChatterAvatarSize.roundToPx(),
                foldedPx = ChatterAvatarFoldedSize.roundToPx(),
            )
            val placeable = measurable.measure(Constraints.fixed(size, size))
            layout(size, size) { placeable.place(0, 0) }
        }
        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        .padding(2.dp)
        .clip(CircleShape)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = modifier,
            tint = Color.White,
        )
    }
}
