package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.ui.components.SharedEmoteImage

internal val LocalChatterLabels = compositionLocalOf<Map<String, List<ChatterLabelIcon>>> { emptyMap() }

internal val BadgeGap = 4.dp

internal fun badgeInlineContent(
    model: Any?,
    contentDescription: String,
    badgeSize: Dp,
    slotWidth: TextUnit,
    slotHeight: TextUnit,
    onClick: (() -> Unit)? = null,
): InlineTextContent = InlineTextContent(
    Placeholder(slotWidth, slotHeight, PlaceholderVerticalAlign.Center),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (model is String) {
            // Chat badges repeat on every message; the shared store loads each once, far cheaper than a request per row.
            SharedEmoteImage(
                url = model,
                contentDescription = contentDescription,
                size = badgeSize,
                // The square comes first, so a wide badge fits inside it as before instead of growing out of its slot.
                modifier = Modifier.size(badgeSize).ffzModBadgeBackground(model),
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.size(badgeSize).ffzModBadgeBackground(model),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

