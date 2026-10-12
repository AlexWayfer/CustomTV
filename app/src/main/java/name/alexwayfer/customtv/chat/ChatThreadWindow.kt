package name.alexwayfer.customtv.chat

internal data class ChatThreadWindow(
    val pinned: ChatThreadEntry?,
    val following: List<ChatThreadEntry>,
)

internal fun threadWindow(entries: List<ChatThreadEntry>, starterId: String): ChatThreadWindow {
    val pinned = entries.firstOrNull { it.id == starterId }
    val following = if (pinned == null) entries else entries.filter { it.id != starterId }
    return ChatThreadWindow(pinned = pinned, following = following)
}

internal fun threadPanelDragCloses(dragPx: Float, heightPx: Int, slopPx: Float): Boolean {
    if (dragPx <= 0f || heightPx <= 0) return false
    val limit = minOf(slopPx, heightPx * 0.35f).coerceAtLeast(1f)
    return dragPx >= limit
}

/**
 * The height the pinned starter may take in a thread body of [availablePx]. When the starter and the
 * replies do not both fit, the starter keeps what the replies leave free, but never less than half,
 * so neither part is pushed out and each scrolls in its own share. A height of 0 means not measured.
 */
internal fun threadPinnedMaxHeightPx(availablePx: Int, pinnedPx: Int, followingPx: Int): Int {
    if (pinnedPx <= 0 || followingPx <= 0) return availablePx
    if (pinnedPx + followingPx <= availablePx) return pinnedPx
    return minOf(pinnedPx, maxOf(availablePx / 2, availablePx - followingPx))
}

/** While replying with the keyboard or emote picker up, the chat mode plaques give their room to the thread. */
internal fun threadReplyHidesChrome(replying: Boolean, composerOpen: Boolean): Boolean = replying && composerOpen
