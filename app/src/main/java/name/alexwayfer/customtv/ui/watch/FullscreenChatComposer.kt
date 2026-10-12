package name.alexwayfer.customtv.ui.watch

/**
 * How tall the keyboard is when fully open: its target while it opens or stays, and the remembered height while it
 * closes, when the target is already zero. Zero once it is gone.
 */
internal fun fullscreenKeyboardHeightPx(imePx: Int, targetPx: Int, rememberedPx: Int): Int = when {
    targetPx > 0 -> maxOf(targetPx, imePx)
    imePx > 0 -> maxOf(rememberedPx, imePx)
    else -> 0
}

/**
 * What the layout takes for the keyboard, as its current and target heights: the keyboard itself, or while the emote
 * picker takes its place ([pickerPx] above zero), the picker. It is as tall as the keyboard was in this orientation,
 * so the video and the chat keep the size they had with the keyboard.
 */
internal fun fullscreenComposerHeightsPx(imePx: Int, imeTargetPx: Int, pickerPx: Int): Pair<Int, Int> =
    if (pickerPx <= 0) imePx to imeTargetPx else pickerPx to pickerPx

/**
 * Where the panel under the video starts while the emote picker takes the keyboard's place, so the space the
 * keyboard would cover beside the chat looks like part of the picker; null without the picker.
 */
internal fun fullscreenPickerFillTopPx(areaHeightPx: Int, pickerPx: Int): Int? =
    if (pickerPx > 0) (areaHeightPx - pickerPx).coerceAtLeast(0) else null

/** The keyboard's full height between layout passes, so a closing keyboard knows how far it has gone. */
internal class FullscreenKeyboardMemory {
    private var heightPx = 0

    /** The keyboard's full height and how far it is open, from 0 to 1; the emote picker counts as the keyboard. */
    fun measure(imePx: Int, imeTargetPx: Int, pickerPx: Int): Pair<Int, Float> {
        val (currentPx, targetPx) = fullscreenComposerHeightsPx(imePx, imeTargetPx, pickerPx)
        heightPx = fullscreenKeyboardHeightPx(currentPx, targetPx, heightPx)
        val progress = if (heightPx > 0) currentPx.toFloat() / heightPx else 0f
        return heightPx to progress
    }
}

/**
 * How opaque the message bar and its field are: see-through like the rest of a chat laid over the video, and solid
 * once the keyboard or the emote picker opens and the chat moves beside the video.
 */
internal fun chatInputBarAlpha(overVideo: Boolean, composerOpen: Boolean): Float =
    if (overVideo && !composerOpen) FULLSCREEN_OVERLAY_CHAT_ALPHA else 1f

/** A chat laid over the video has no message bar; it unfolds while the keyboard or the emote picker is open. */
internal fun chatInputBarFolded(overVideo: Boolean, composerOpen: Boolean): Boolean = overVideo && !composerOpen
