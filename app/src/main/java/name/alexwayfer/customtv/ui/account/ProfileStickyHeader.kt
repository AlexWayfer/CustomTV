package name.alexwayfer.customtv.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The height of the header once the profile has scrolled it into a bar. */
internal val ProfileBarHeight = 64.dp

/** The avatar's size in the bar. */
private val BarAvatarSize = 40.dp

/** The bar's start padding, and the gap between its avatar and the name. */
private val BarSpacing = 16.dp

/** How far below the avatar's top the name sits while the header is open. */
private val NameTopOffset = 56.dp

/** The gap between the avatar and the name while the header is open. */
private val NameStartGap = 16.dp

/**
 * How far the header has folded into the bar, from 0 to 1: it folds as the profile scrolls, and is a bar once the
 * place it held has gone under the bar.
 */
internal fun profileHeaderCollapse(scrollPx: Int, headerTopPx: Int, headerHeightPx: Int, barPx: Int): Float {
    val rangePx = headerTopPx + headerHeightPx - barPx
    if (rangePx <= 0) return if (scrollPx > 0) 1f else 0f
    return (scrollPx.toFloat() / rangePx).coerceIn(0f, 1f)
}

/** The lines under the name fade out over the first half of the fold. */
internal fun profileHeaderDetailsAlpha(collapse: Float): Float = (1f - collapse * 2f).coerceIn(0f, 1f)

/** How far down the tab row moves to stay under the bar once the profile has scrolled it there; 0 before that. */
internal fun profileTabRowPinPx(scrollPx: Int, barPx: Int, tabRowTopPx: Int): Int =
    (scrollPx + barPx - tabRowTopPx).coerceAtLeast(0)

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

/**
 * The profile's avatar, name, and the lines under the name, laid over the scrolling profile. Open, they sit where
 * the profile leaves room for them, [headerTop] below the top, and scroll with it; as the profile scrolls, the
 * avatar shrinks into the corner, the name moves next to it and shrinks by [barNameScale], the lines fade, and a
 * bar fills in behind them. The layout is [headerTop] plus the open header's height, so the profile can leave that
 * room. [scroll] and the moves are read while laying out, so scrolling does not recompose the header.
 */
@Composable
internal fun ProfileStickyHeader(
    headerTop: Dp,
    barNameScale: Float,
    roomForMenu: Boolean,
    scroll: () -> Int,
    avatar: @Composable () -> Unit,
    name: @Composable () -> Unit,
    details: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.background
    Layout(
        contents = listOf({ Box(Modifier.background(barColor)) }, details, avatar, name),
        modifier = modifier,
    ) { (barMeasurables, detailsMeasurables, avatarMeasurables, nameMeasurables), constraints ->
        val width = constraints.maxWidth
        val barPx = ProfileBarHeight.roundToPx()
        val headerTopPx = headerTop.roundToPx()
        val sidePx = 24.dp.roundToPx()
        val avatarPlaceable = avatarMeasurables.first().measure(Constraints())
        val textStartPx = sidePx + avatarPlaceable.width + NameStartGap.roundToPx()
        val textConstraints = Constraints(maxWidth = (width - textStartPx - sidePx).coerceAtLeast(0))
        val namePlaceable = nameMeasurables.first().measure(textConstraints)
        val detailsPlaceable = detailsMeasurables.first().measure(textConstraints)
        val nameTopPx = NameTopOffset.roundToPx()
        val headerHeightPx = maxOf(avatarPlaceable.height, nameTopPx + namePlaceable.height + detailsPlaceable.height)
        val barPlaceable = barMeasurables.first().measure(Constraints.fixed(width, barPx))

        val collapse = profileHeaderCollapse(scroll(), headerTopPx, headerHeightPx, barPx)
        // Open, the header scrolls with the profile; folded, it rests in the bar.
        val openTop = (headerTopPx - scroll()).toFloat()
        val barAvatarPx = BarAvatarSize.toPx()
        val avatarScale = lerp(1f, barAvatarPx / avatarPlaceable.width.coerceAtLeast(1), collapse)
        val avatarX = lerp(sidePx.toFloat(), BarSpacing.toPx(), collapse)
        val avatarY = lerp(openTop, (barPx - barAvatarPx) / 2f, collapse)
        val nameScale = lerp(1f, barNameScale, collapse)
        val nameX = lerp(textStartPx.toFloat(), BarSpacing.toPx() * 2 + barAvatarPx, collapse)
        val nameY = lerp(openTop + nameTopPx, (barPx - namePlaceable.height * barNameScale) / 2f, collapse)
        val detailsAlpha = profileHeaderDetailsAlpha(collapse)
        // The menu button keeps the bar's end, so a long name stops short of it.
        val nameRoomPx = width - nameX - (if (roomForMenu) 64.dp.toPx() else BarSpacing.toPx())
        val nameClip = (nameRoomPx / (namePlaceable.width * nameScale).coerceAtLeast(1f)).coerceAtMost(1f)

        layout(width, headerTopPx + headerHeightPx) {
            barPlaceable.placeWithLayer(0, 0) { alpha = collapse }
            detailsPlaceable.placeWithLayer(textStartPx, (openTop + nameTopPx + namePlaceable.height).roundToInt()) {
                alpha = detailsAlpha
            }
            avatarPlaceable.placeWithLayer(avatarX.roundToInt(), avatarY.roundToInt()) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = avatarScale
                scaleY = avatarScale
            }
            namePlaceable.placeWithLayer(nameX.roundToInt(), nameY.roundToInt()) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = nameScale * nameClip
                scaleY = nameScale * nameClip
            }
        }
    }
}
