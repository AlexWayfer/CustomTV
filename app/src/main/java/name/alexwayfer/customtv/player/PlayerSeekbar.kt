package name.alexwayfer.customtv.player

/**
 * Where a recording's seek bar sits on the player page, as shares of its width and height: the bar's [left] and
 * [right] edges, its [barTop], and the top of the area above it that takes its touches, [touchTop].
 */
internal data class PlayerSeekbar(val left: Float, val right: Float, val barTop: Float, val touchTop: Float)

/** The seek bar the page reported, or null for shares outside the page or edges in the wrong order. */
internal fun playerSeekbarOrNull(left: Double, right: Double, barTop: Double, touchTop: Double): PlayerSeekbar? {
    val shares = listOf(left, right, barTop, touchTop)
    if (shares.any { !it.isFinite() || it < 0.0 || it > 1.0 }) return null
    if (left >= right || touchTop > barTop) return null
    return PlayerSeekbar(left.toFloat(), right.toFloat(), barTop.toFloat(), touchTop.toFloat())
}
