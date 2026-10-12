package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt

/** Where the dark fade under the text starts, down the banner image. */
private const val BANNER_SCRIM_START = 0.45f

private val OnImageTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.9f),
    offset = Offset(0f, 1f),
    blurRadius = 14f,
)

/**
 * The chatter's channel banner with their avatar and names over its bottom, folding as the profile scrolls.
 * [pinProgress] is 0 on the profile and 1 on the card's other pages, which keep the banner folded.
 */
@Composable
internal fun ChatterCardHeader(
    profile: ChatterProfile?,
    chatter: ChatterCardChatter,
    collapse: ChatterHeaderCollapse,
    pinProgress: () -> Float,
    onOpenProfile: (() -> Unit)?,
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
                .offset {
                    val maxShiftPx = (ChatterHeaderMargin - ChatterHeaderFoldedMargin).roundToPx()
                    val shown = pinnedCollapsePx(collapse.collapsedPx, collapse.rangePx, pinProgress())
                    IntOffset(0, chatterHeaderRowShiftPx(shown, collapse.rangePx, maxShiftPx))
                }
                // No top padding, and never squeezed by the banner: only the fold itself shrinks the avatar.
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .padding(start = ChatterHeaderMargin, end = ChatterHeaderMargin, bottom = ChatterHeaderMargin)
                .openProfileButton(stringResource(R.string.chatter_card_open_profile), onOpenProfile),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatterAvatar(profile?.avatarUrl, collapse, pinProgress)
            ChatterCardNames(chatter.name, chatter.login, collapse)
        }
    }
}

/** The avatar and the name are one button; the pill ripple hugs the round avatar. */
private fun Modifier.openProfileButton(label: String, onOpenProfile: (() -> Unit)?): Modifier =
    if (onOpenProfile != null) {
        clip(CircleShape).clickable(onClickLabel = label, role = Role.Button, onClick = onOpenProfile)
    } else {
        this
    }

/** The display name, and the login under it when it differs. */
@Composable
private fun ChatterCardNames(name: String, login: String, collapse: ChatterHeaderCollapse) {
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
