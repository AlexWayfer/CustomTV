package name.alexwayfer.customtv.ui.watch

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * How far in from the side the screen's rounded top corner of [radiusPx] reaches [depthPx] below the top edge, beyond
 * the room the content already keeps from that side ([sideInsetPx]), such as for the display cutout.
 */
internal fun roundedCornerInsetPx(radiusPx: Int, depthPx: Int, sideInsetPx: Int): Int {
    if (radiusPx <= 0 || depthPx >= radiusPx) return 0
    val radius = radiusPx.toDouble()
    val above = radius - depthPx.coerceAtLeast(0)
    val edge = radius - sqrt(radius * radius - above * above)
    return (edge.roundToInt() - sideInsetPx).coerceAtLeast(0)
}

/**
 * The room the stream info keeps from the screen's rounded top corners while it sits at the top ([active]), so its
 * first line stays as far from the curve as from a straight side; none where the screen tells no corners.
 */
@Composable
internal fun streamInfoCornersPadding(active: Boolean): PaddingValues {
    if (!active || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return PaddingValues()
    val display = LocalView.current.display ?: return PaddingValues()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val depthPx = with(density) { StreamInfoTopPadding.roundToPx() }
    val leftRadius = display.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
    val rightRadius = display.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
    return with(density) {
        PaddingValues.Absolute(
            left = roundedCornerInsetPx(leftRadius, depthPx, cutout.getLeft(density, layoutDirection)).toDp(),
            right = roundedCornerInsetPx(rightRadius, depthPx, cutout.getRight(density, layoutDirection)).toDp(),
        )
    }
}
