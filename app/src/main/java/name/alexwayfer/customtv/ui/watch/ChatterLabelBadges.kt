package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.chat.ShownBadge
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.ui.components.SharedEmoteImage

internal val LocalChatterLabels = compositionLocalOf<Map<String, List<ChatterLabelIcon>>> { emptyMap() }

internal val BadgeGap = 4.dp

/**
 * A badge's image in a slot of the [sizes] of its line. The [model] is a URL, a [ShownBadge] drawn on its own color, or
 * another picture.
 */
internal fun badgeInlineContent(
    model: Any?,
    contentDescription: String,
    sizes: ChatTextSizes,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
): InlineTextContent = InlineTextContent(
    Placeholder(sizes.badgeSlotWidth, sizes.badgeSlotHeight, PlaceholderVerticalAlign.Center),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // A badge takes the touch around it too, so it passes a long press on to the message.
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val shown = model as? ShownBadge
        val url = shown?.url ?: model as? String
        if (url != null) {
            // Chat badges repeat on every message; the shared store loads each once, far cheaper than a request per
            // row.
            SharedEmoteImage(
                url = url,
                contentDescription = contentDescription,
                size = sizes.badge,
                // The square comes first, so a wide badge fits inside it as before instead of growing out of its slot.
                modifier = Modifier
                    .size(sizes.badge)
                    .ffzModBadgeBackground(url)
                    .badgeBackground(shown?.background ?: Color.Unspecified),
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.size(sizes.badge).ffzModBadgeBackground(model),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

