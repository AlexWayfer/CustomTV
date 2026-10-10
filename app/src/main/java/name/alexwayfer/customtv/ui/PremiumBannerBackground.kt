package name.alexwayfer.customtv.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.ui.theme.PremiumGold
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchPurple

// How far toward the diagonal each glow reaches before it fades out, leaving the middle dark.
private const val PURPLE_GLOW_REACH = 0.6f
private const val GOLD_GLOW_REACH = 0.5f

// How much of the sideways part of a glow's direction stays: below 1 turns it toward vertical.
private const val GLOW_SIDEWAYS = 0.35f

/**
 * The background of the free build's Premium banner and the Premium access screen: a purple glow from the top right
 * corner and a gold one from the bottom left, each fading out before the diagonal between the other two corners,
 * whatever the screen's proportions.
 */
internal fun Modifier.premiumBannerBackground(): Modifier = drawWithCache {
    val glows = premiumBannerGlows(size.width, size.height)
    val purple = Brush.linearGradient(
        colors = listOf(TwitchPurple.copy(alpha = 0.25f), Color.Transparent),
        start = glows.purpleFrom,
        end = premiumBannerGlowEnd(glows.purpleFrom, glows.purpleTo, PURPLE_GLOW_REACH, GLOW_SIDEWAYS),
    )
    val gold = Brush.linearGradient(
        colors = listOf(PremiumGold.copy(alpha = 0.15f), Color.Transparent),
        start = glows.goldFrom,
        end = premiumBannerGlowEnd(glows.goldFrom, glows.goldTo, GOLD_GLOW_REACH, GLOW_SIDEWAYS),
    )
    onDrawBehind {
        drawRect(TwitchBg)
        drawRect(purple)
        drawRect(gold)
    }
}

internal data class PremiumBannerGlows(
    val purpleFrom: Offset,
    val purpleTo: Offset,
    val goldFrom: Offset,
    val goldTo: Offset,
)

/**
 * Each glow runs from its corner straight to the diagonal from the top left to the bottom right corner, so the
 * diagonal is where both fade out. On a zero size every point is the origin.
 */
internal fun premiumBannerGlows(width: Float, height: Float): PremiumBannerGlows {
    val diagonalSquared = width * width + height * height
    if (diagonalSquared == 0f) return PremiumBannerGlows(Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero)
    // The point of the diagonal nearest to a corner (x, y) lies at (x·w + y·h) / (w² + h²) of its length.
    fun nearestOnDiagonal(x: Float, y: Float): Offset {
        val along = (x * width + y * height) / diagonalSquared
        return Offset(width * along, height * along)
    }
    return PremiumBannerGlows(
        purpleFrom = Offset(width, 0f),
        purpleTo = nearestOnDiagonal(width, 0f),
        goldFrom = Offset(0f, height),
        goldTo = nearestOnDiagonal(0f, height),
    )
}

/**
 * Where a glow from [from] fades out: [reach] of the way to [toward], turned toward vertical by keeping only
 * [sideways] of the sideways part of the direction. The distance stays the same as [reach] of the straight way.
 */
internal fun premiumBannerGlowEnd(from: Offset, toward: Offset, reach: Float, sideways: Float): Offset {
    val straight = toward - from
    val turned = Offset(straight.x * sideways, straight.y)
    val turnedLength = turned.getDistance()
    if (turnedLength == 0f) return from
    return from + turned * (straight.getDistance() * reach / turnedLength)
}
