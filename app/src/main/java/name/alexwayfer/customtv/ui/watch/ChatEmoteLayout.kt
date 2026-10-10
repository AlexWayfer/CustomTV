package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.ui.components.resolvedEmoteAspectRatio

internal const val EmoteGapEm = 0.18f

internal fun isWhitespaceBetweenEmotes(parts: List<ChatPart>, index: Int): Boolean {
    val part = parts.getOrNull(index) as? ChatPart.Text ?: return false
    return part.text.isNotEmpty() && part.text.all { it.isWhitespace() } &&
        parts.getOrNull(index - 1) is ChatPart.Emote &&
        parts.getOrNull(index + 1) is ChatPart.Emote
}

internal fun stackedEmoteSlotAspect(part: ChatPart.Emote): Float {
    return stackedEmoteSlotAspect(
        baseAspect = resolvedEmoteAspectRatio(part.url, part.aspectRatio),
        baseWidthMultiplier = part.effects.widthMultiplier,
        overlays = part.overlays.map { overlay ->
            resolvedEmoteAspectRatio(overlay.url, overlay.aspectRatio) to
                overlay.effects.widthMultiplier
        },
    )
}

internal fun stackedEmoteSlotAspect(
    baseAspect: Float,
    baseWidthMultiplier: Float,
    overlays: List<Pair<Float, Float>>,
): Float = maxOf(
    baseAspect * baseWidthMultiplier,
    overlays.maxOfOrNull { (aspect, multiplier) -> aspect * multiplier } ?: 0f,
)

internal fun emoteGapWidth(part: ChatPart.Text, next: ChatPart.Emote): Float {
    return if (part.text.all { it.isWhitespace() } && next.effects.removeSpaceBefore) 0f else EmoteGapEm
}
