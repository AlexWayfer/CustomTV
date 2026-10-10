package name.alexwayfer.customtv.chat

/** The keyboard's settled heights in each window orientation, 0 where it was not seen; [keyboardId] names the IME. */
internal data class KeyboardHeights(
    val keyboardId: String?,
    val portraitPx: Int,
    val landscapePx: Int,
)

/**
 * The settled keyboard height for each window orientation: the keyboard of a landscape window is lower, so a height
 * seen in one orientation does not size the emote picker in the other. A height belongs to one keyboard: another one
 * has its own heights, so switching drops them.
 */
internal class KeyboardHeightMemory {
    private var keyboardId: String? = null
    private var portraitPx = 0
    private var landscapePx = 0
    private var lastLandscape: Boolean? = null

    /** The orientation the keyboard was last seen moving in, since the window last turned. */
    private var movedInLandscape: Boolean? = null

    val heights: KeyboardHeights get() = KeyboardHeights(keyboardId, portraitPx, landscapePx)

    /**
     * Right after the window turns, the keyboard insets still hold the other orientation's height, so a settled
     * keyboard counts only once it was seen rising in this orientation.
     */
    fun heightPx(imeBottomPx: Int, imeTargetBottomPx: Int, landscape: Boolean): Int {
        if (landscape != lastLandscape) {
            lastLandscape = landscape
            movedInLandscape = null
        }
        if (imeTargetBottomPx > 0 && imeBottomPx != imeTargetBottomPx) movedInLandscape = landscape
        val counts = movedInLandscape == landscape
        return if (landscape) {
            if (counts) landscapePx = settledKeyboardHeightPx(imeBottomPx, imeTargetBottomPx, landscapePx)
            landscapePx
        } else {
            if (counts) portraitPx = settledKeyboardHeightPx(imeBottomPx, imeTargetBottomPx, portraitPx)
            portraitPx
        }
    }

    /** Forgets the heights when [id] is not the keyboard they were seen with; true when it forgot any. */
    fun useKeyboard(id: String?): Boolean {
        if (id == keyboardId) return false
        val forgets = portraitPx > 0 || landscapePx > 0
        keyboardId = id
        portraitPx = 0
        landscapePx = 0
        return forgets
    }

    /**
     * The keyboard [id] has just settled in one orientation. A new keyboard keeps only that height: the other
     * orientation's height was the previous keyboard's.
     */
    fun keyboardShown(id: String?, landscape: Boolean) {
        if (id == keyboardId) return
        keyboardId = id
        if (landscape) portraitPx = 0 else landscapePx = 0
    }

    /**
     * Takes saved heights of the current keyboard for the orientations not seen since; a height seen in this run is
     * newer than the saved one.
     */
    fun restore(saved: KeyboardHeights) {
        if (saved.keyboardId != keyboardId) return
        if (portraitPx == 0) portraitPx = saved.portraitPx
        if (landscapePx == 0) landscapePx = saved.landscapePx
    }
}
