package name.alexwayfer.customtv.ui

import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** The premium border along the screen's edge: a faint thin line with a glow into the screen. */
internal val PremiumGlowEdgeStyle = PremiumGlowStyle(
    line = 0.5.dp,
    lineAlpha = 0.6f,
    glow = 8.dp,
    glowAlpha = 0.3f,
    outward = false,
)

// The system's rounded corners overlay masks more of each corner than the radius it reports, which thins the line
// there; a larger radius keeps the line inside the visible glass.
private const val SCREEN_CORNER_SCALE = 1.15f

/**
 * The rounded corners of the screen the activity fills, read again when the screen turns; square where the system
 * does not tell.
 */
@Composable
internal fun rememberScreenCorners(): GlowCorners {
    val activity = LocalActivity.current
    val configuration = LocalConfiguration.current
    return remember(activity, configuration) {
        val insets = activity?.window?.decorView?.rootWindowInsets
        if (insets == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            GlowCorners.Square
        } else {
            GlowCorners(
                topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat() ?: 0f,
                topRight = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius?.toFloat() ?: 0f,
                bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius?.toFloat() ?: 0f,
                bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius?.toFloat() ?: 0f,
            ).scaled(SCREEN_CORNER_SCALE)
        }
    }
}
