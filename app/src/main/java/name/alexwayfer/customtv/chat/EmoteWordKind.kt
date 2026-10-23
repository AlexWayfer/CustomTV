package name.alexwayfer.customtv.chat

/** What one word of chat text is to the emotes around it. */
enum class EmoteWordKind {
    /** Text, or anything else that ends the run of emotes before it. */
    Other,
    Emote,

    /** A zero-width emote: it stacks on the emote before it, or stands alone without one. */
    Overlay,

    /** A BTTV modifier such as `w!`: it waits for the emote after it. */
    BttvModifier,

    /** An FFZ modifier known only by name, such as `ffzW` while FFZ's emotes load: text without an emote before it. */
    FfzModifier,

    /** An FFZ modifier emote, such as `ffzCursed`: it changes the emote before it, or shows itself without one. */
    FfzModifierEmote,
}

/** The target of a word that applies to no other word. */
const val NO_TARGET = -1

/**
 * For each word of [kinds], the index of the emote word it applies to, or [NO_TARGET]. BTTV
 * modifiers apply to the emote right after them; FFZ modifiers and overlays to the emote right
 * before them, with only other modifiers and overlays between. Chat and the message field both
 * read their emote groups from here, so a draft shows what chat will.
 */
fun emoteWordTargets(kinds: List<EmoteWordKind>): IntArray {
    val targets = IntArray(kinds.size) { NO_TARGET }
    val waiting = ArrayList<Int>()
    var previousEmote = NO_TARGET
    fun take(index: Int) {
        for (modifier in waiting) targets[modifier] = index
        waiting.clear()
    }
    kinds.forEachIndexed { index, kind ->
        when (kind) {
            EmoteWordKind.Other -> {
                waiting.clear()
                previousEmote = NO_TARGET
            }
            EmoteWordKind.BttvModifier -> waiting += index
            EmoteWordKind.Emote -> {
                take(index)
                previousEmote = index
            }
            EmoteWordKind.Overlay -> {
                take(index)
                if (previousEmote == NO_TARGET) previousEmote = index else targets[index] = previousEmote
            }
            EmoteWordKind.FfzModifier, EmoteWordKind.FfzModifierEmote -> when {
                previousEmote != NO_TARGET -> {
                    waiting.clear()
                    targets[index] = previousEmote
                }
                kind == EmoteWordKind.FfzModifierEmote -> {
                    take(index)
                    previousEmote = index
                }
                else -> waiting.clear()
            }
        }
    }
    return targets
}
