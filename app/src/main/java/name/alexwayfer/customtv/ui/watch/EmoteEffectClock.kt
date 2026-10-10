package name.alexwayfer.customtv.ui.watch

/** Emote effects redraw at most 30 times a second instead of at the display refresh rate. */
internal const val EMOTE_EFFECT_FRAME_NANOS = 1_000_000_000L / 30

/**
 * The effect time for a display frame, rounded down to the effect frame.
 * Every emote rounds the same shared monotonic clock, so identical emotes stay in step.
 */
internal fun emoteEffectTime(frameTimeNanos: Long): Long =
    frameTimeNanos - Math.floorMod(frameTimeNanos, EMOTE_EFFECT_FRAME_NANOS)

/** How long to sleep after [frameTimeNanos] before the next effect frame is due. */
internal fun nanosUntilNextEmoteEffectFrame(frameTimeNanos: Long): Long =
    EMOTE_EFFECT_FRAME_NANOS - Math.floorMod(frameTimeNanos, EMOTE_EFFECT_FRAME_NANOS)
