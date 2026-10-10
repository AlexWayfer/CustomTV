package name.alexwayfer.customtv.ui.watch


/** How far the last message sticks out past the viewport. Zero when the live edge is already visible. */
internal fun chatTailOverflow(
    lastVisibleIndex: Int,
    totalItems: Int,
    itemEnd: Int,
    viewportEnd: Int,
): Int {
    if (totalItems <= 0 || lastVisibleIndex != totalItems - 1) return 0
    return (itemEnd - viewportEnd).coerceAtLeast(0)
}

/**
 * Whether the list must jump back to the last message: rows inserted or grown above it,
 * such as recent chat loading before Welcome, pushed it wholly below the viewport.
 */
internal fun chatTailPushedOutOfView(
    stickToBottom: Boolean,
    scrolling: Boolean,
    tailCaughtUp: Boolean,
    lastVisibleIndex: Int,
    totalItems: Int,
): Boolean {
    return stickToBottom && !scrolling && tailCaughtUp && lastVisibleIndex in 0 until totalItems - 1
}

/**
 * Pixels to follow after the tail grows, such as a link preview finishing.
 * A tail that was already past the edge is left where the arrival scroll put it. A pixel past the edge is rounding,
 * not a tail left there: an animated growth, such as the raider frame, passes it on its first frames and goes on.
 */
internal fun chatTailGrowthToFollow(
    stickToBottom: Boolean,
    scrolling: Boolean,
    tailCaughtUp: Boolean,
    pinnedOverflow: Int,
    overflow: Int,
): Int {
    if (!stickToBottom || scrolling || !tailCaughtUp) return 0
    if (pinnedOverflow !in 0..CHAT_TAIL_EDGE_SLACK_PX || overflow <= CHAT_TAIL_EDGE_SLACK_PX) return 0
    return overflow
}

private const val CHAT_TAIL_EDGE_SLACK_PX = 1
