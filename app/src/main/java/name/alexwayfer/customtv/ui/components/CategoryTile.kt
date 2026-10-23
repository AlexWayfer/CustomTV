package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.CategoryTag
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.theme.TwitchLive
import java.text.NumberFormat

private val CategoryTileMinWidth = 120.dp
private const val CATEGORY_GRID_MIN_COLUMNS = 3

/** The room between the tiles of a category grid, across and down. */
internal val CategoryGridSpacing = 12.dp

/** Twitch's box art is a portrait cover. */
private const val BOX_ART_ASPECT_RATIO = 3f / 4f

/** A long name, which can run past a hundred characters, ends in an ellipsis on a tile; the category shows it whole. */
private const val CATEGORY_NAME_MAX_LINES = 3

/** Three tiles on a phone; a wider screen adds a column for each [CategoryTileMinWidth]. */
internal fun categoryGridColumns(width: Dp): Int =
    ((width + CategoryGridSpacing) / (CategoryTileMinWidth + CategoryGridSpacing)).toInt()
        .coerceAtLeast(CATEGORY_GRID_MIN_COLUMNS)

/**
 * A category's box art with its name under it, its live viewers and its tags when they are known; [onClick] opens
 * it, and [tagPill] draws each tag, a plain pill unless the tags do something on a tap. A long press goes to
 * [onLongClick], which [onLongClickLabel] names for TalkBack, when there is one.
 */
@Composable
internal fun CategoryTile(
    category: TwitchCategory,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tagPill: @Composable (CategoryTag) -> Unit = { StreamTagPill(it.name) },
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    Column(
        if (onClick == null) {
            modifier.semantics(mergeDescendants = true) {}
        } else {
            modifier.combinedClickable(
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                // The long press vibrates by itself.
                onLongClick = onLongClick,
                onClick = onClick,
            )
        },
    ) {
        CategoryBoxArt(category.boxArtUrl, RoundedCornerShape(8.dp), Modifier.fillMaxWidth())
        Text(
            text = category.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            maxLines = CATEGORY_NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        category.viewerCount?.let { CategoryViewers(it) }
        if (category.tags.isNotEmpty()) {
            OneLineTagPills(Modifier.padding(top = 4.dp)) { category.tags.forEach { tagPill(it) } }
        }
    }
}

/** A category's cover, as wide as the [modifier] makes it, or its place while there is none. Decorative. */
@Composable
internal fun CategoryBoxArt(url: String?, shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(BOX_ART_ASPECT_RATIO)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        url?.let { boxArt ->
            AsyncImage(
                model = boxArt,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** The live viewers, shortened as Twitch shows them; TalkBack reads the whole number. */
@Composable
internal fun CategoryViewers(viewers: Int) {
    val description = pluralStringResource(
        R.plurals.stream_viewers,
        viewers,
        NumberFormat.getIntegerInstance().format(viewers),
    )
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(TwitchLive))
        Spacer(Modifier.width(4.dp))
        Text(
            text = abbreviatedCount(viewers.toLong()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
