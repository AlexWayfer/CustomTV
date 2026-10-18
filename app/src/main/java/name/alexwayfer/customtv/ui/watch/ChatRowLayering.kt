package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Rows draw in list order, so a later row covers what an earlier one spills; a spilling row goes under all of them. */
private const val SPILLING_ROW_Z_INDEX = -1f

/** Rounding slack, so a square emote turned on its side does not count as spilling. */
private const val SPILL_TOLERANCE = 0.01f

/**
 * The drawing order of a chat row. A row whose emote reaches out of its line, such as a wide emote turned on its
 * side, lies under the rows around it, so their text stays on top of it. Pass the message as it shows, with the
 * third-party emotes and modifiers put in (`rememberDisplayMessage`): before that, `r! emote` is plain text.
 */
internal fun chatRowZIndex(message: ChatMessage): Float {
    val spills = message.parts.any { part ->
        part is ChatPart.Emote && (
            emoteSpillsOutOfLine(resolvedEmoteAspectRatio(part.url, part.aspectRatio), part.effects) ||
                part.overlays.any { overlay ->
                    emoteSpillsOutOfLine(resolvedEmoteAspectRatio(overlay.url, overlay.aspectRatio), overlay.effects)
                }
            )
    }
    return if (spills) SPILLING_ROW_Z_INDEX else 0f
}

/** Whether an emote of [aspect] (width over height) stands taller than its line once [effects] widen and turn it. */
internal fun emoteSpillsOutOfLine(aspect: Float, effects: EmoteEffects): Boolean {
    val radians = Math.toRadians(effects.rotationDegrees.toDouble())
    val width = aspect * effects.widthMultiplier
    val turnedHeight = width * abs(sin(radians)) + abs(cos(radians))
    return turnedHeight > 1 + SPILL_TOLERANCE
}
