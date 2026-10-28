package name.alexwayfer.customtv.data

/** Slow uses before the tip for an action the user repeats often. */
private const val REPEATED_SLOW_USES = 3

/**
 * A one-time tip about a gesture, due once the user has done the same thing the slower way [slowUsesBeforeHint] times
 * without ever using the gesture. [key] names its values in the stored hints.
 */
internal enum class GestureHint(val key: String, val slowUsesBeforeHint: Int) {
    Reply("reply", REPEATED_SLOW_USES),
    RemoveRecent("remove_recent", 1),
    Minimize("minimize", REPEATED_SLOW_USES),
    EnterFullscreen("enter_fullscreen", REPEATED_SLOW_USES),
    ExitFullscreen("exit_fullscreen", REPEATED_SLOW_USES),
    FullscreenChatSide("fullscreen_chat_side", 1),
    FullscreenChatHide("fullscreen_chat_hide", 1),
    HideStream("hide_stream", REPEATED_SLOW_USES),

    /** Premium: the moderating swipe; a user who turned it off gets no tip about it. */
    ModerationSwipe("moderation_swipe", REPEATED_SLOW_USES),
}

/** What is stored for one tip: whether its gesture was ever used, the slow uses so far, and whether it was shown. */
internal data class GestureHintProgress(
    val gestureUsed: Boolean = false,
    val slowUses: Int = 0,
    val shown: Boolean = false,
)

internal data class GestureHintSlowUse(val progress: GestureHintProgress, val show: Boolean)

/**
 * Counts one slow use. The tip shows once, on the use that reaches [threshold]; never after it was shown or after the
 * gesture itself was used, and the count stops then.
 */
internal fun gestureHintAfterSlowUse(progress: GestureHintProgress, threshold: Int): GestureHintSlowUse {
    if (progress.gestureUsed || progress.shown) return GestureHintSlowUse(progress, show = false)
    val uses = progress.slowUses + 1
    val show = uses >= threshold
    return GestureHintSlowUse(progress.copy(slowUses = uses, shown = show), show)
}
