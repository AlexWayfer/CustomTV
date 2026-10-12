package name.alexwayfer.customtv.chat

/** Matches BTTV, FFZ, and 7TV emotes and the modifiers of the enabled platforms in chat text. */
object ThirdPartyEmoteMatcher {
    fun overlay(
        parts: List<ChatPart>,
        emotes: Map<String, SevenTvEmote>,
        modifierPlatforms: Set<EmotePlatform> = emptySet(),
    ): List<ChatPart> {
        if (parts.isEmpty()) return parts
        val bttvModifiers = EmotePlatform.Bttv in modifierPlatforms
        val out = ArrayList<ChatPart>(parts.size)
        var prefixEffects: EmoteEffects? = null
        var prefixModifiers: List<EmoteModifier> = emptyList()
        parts.forEachIndexed { index, part ->
            when (part) {
                is ChatPart.Emote -> {
                    out += prefixEffects?.let {
                        part.copy(
                            effects = part.effects.combine(it),
                            modifiers = part.modifiers + prefixModifiers,
                        )
                    } ?: part
                    prefixEffects = null
                    prefixModifiers = emptyList()
                }
                is ChatPart.Gif -> {
                    out += part
                    prefixEffects = null
                    prefixModifiers = emptyList()
                }
                is ChatPart.Text -> {
                    val next = parts.getOrNull(index + 1)
                    val effects = if (bttvModifiers) EmoteEffects.bttvPrefix(part.text) else null
                    if (effects != null && next is ChatPart.Emote) {
                        prefixEffects = effects
                        prefixModifiers = part.text.trim().split(Regex("\\s+"))
                            .mapNotNull { token ->
                                // bttvPrefix above has already read every token as an effect.
                                EmoteEffects.bttv(token)?.let { EmoteModifier(token, EmotePlatform.Bttv, it) }
                            }
                    } else {
                        overlayText(part.text, emotes, modifierPlatforms, out)
                    }
                }
            }
        }
        return out.ifEmpty { parts }
    }

    private fun overlayText(
        text: String,
        emotes: Map<String, SevenTvEmote>,
        modifierPlatforms: Set<EmotePlatform>,
        out: MutableList<ChatPart>,
    ) {
        val bttvModifiers = EmotePlatform.Bttv in modifierPlatforms
        val ffzModifiers = EmotePlatform.Ffz in modifierPlatforms
        if (text.isEmpty()) {
            out += ChatPart.Text(text)
            return
        }
        val overlay = TextOverlay(emotes, bttvModifiers, ffzModifiers, out)
        text.split(TOKEN_SPLIT).forEach(overlay::add)
    }

    private val TOKEN_SPLIT = Regex("(?<=\\s)|(?=\\s)")
}

/**
 * Adds the words of one text part to [out]: emotes as emotes, the rest as text. BTTV modifier words wait for the next
 * emote; FFZ modifier words change the emote before them.
 */
private class TextOverlay(
    private val emotes: Map<String, SevenTvEmote>,
    private val bttvModifiers: Boolean,
    private val ffzModifiers: Boolean,
    private val out: MutableList<ChatPart>,
) {
    private var pendingEffects = EmoteEffects()
    private val pendingModifiers = mutableListOf<EmoteModifier>()
    private var consumeFollowingWhitespace = false

    fun add(token: String) {
        if (token.isEmpty()) return
        if (consumeFollowingWhitespace && token.isBlank()) {
            consumeFollowingWhitespace = false
            return
        }
        if (holdBttvModifier(token)) return
        val emote = emotes[token]
        when {
            emote == null && ffzModifiers && modifyPreviousEmote(token) -> Unit
            emote != null -> addEmote(token, emote)
            else -> addText(token)
        }
    }

    /** A BTTV modifier word waits for the next emote; true when [token] is one. */
    private fun holdBttvModifier(token: String): Boolean {
        val effects = (if (bttvModifiers) EmoteEffects.bttv(token) else null) ?: return false
        pendingEffects = pendingEffects.combine(effects)
        pendingModifiers += EmoteModifier(token, EmotePlatform.Bttv, effects)
        consumeFollowingWhitespace = true
        return true
    }

    /** A known FFZ modifier word changes the emote before it; true when [token] did. */
    private fun modifyPreviousEmote(token: String): Boolean {
        val fallback = EmoteEffects.knownFfz(token) ?: return false
        val baseIndex = previousEmoteIndex(out) ?: return false
        val base = out[baseIndex] as ChatPart.Emote
        out[baseIndex] = base.copy(
            effects = base.effects.combine(fallback),
            modifiers = base.modifiers + EmoteModifier(token, EmotePlatform.Ffz, fallback),
        )
        out.subList(baseIndex + 1, out.size).clear()
        clearPending()
        return true
    }

    private fun addEmote(token: String, emote: SevenTvEmote) {
        val part = ChatPart.Emote(
            name = token,
            url = emote.url,
            aspectRatio = emote.aspectRatio,
            effects = pendingEffects,
            modifiers = pendingModifiers.toList(),
        )
        clearPending()
        val effects = emote.effects
        val baseIndex = previousEmoteIndex(out)
        when {
            // An FFZ modifier emote changes the emote before it instead of showing itself.
            effects != null && baseIndex != null -> {
                val base = out[baseIndex] as ChatPart.Emote
                out[baseIndex] = base.copy(
                    effects = base.effects.combine(effects),
                    modifiers = base.modifiers + EmoteModifier(token, EmotePlatform.Ffz, effects),
                )
                out.subList(baseIndex + 1, out.size).clear()
            }
            emote.overlay && baseIndex != null -> {
                val base = out[baseIndex] as ChatPart.Emote
                out[baseIndex] = base.copy(overlays = base.overlays + part)
            }
            else -> out += part
        }
    }

    private fun addText(token: String) {
        clearPending()
        val last = out.lastOrNull()
        if (last is ChatPart.Text) {
            out[out.lastIndex] = ChatPart.Text(last.text + token)
        } else {
            out += ChatPart.Text(token)
        }
    }

    private fun clearPending() {
        pendingEffects = EmoteEffects()
        pendingModifiers.clear()
    }
}

/** The emote right before the end of [parts], with only blank text after it. */
private fun previousEmoteIndex(parts: List<ChatPart>): Int? {
    val last = parts.indexOfLast { it !is ChatPart.Text || it.text.isNotBlank() }
    return last.takeIf { it >= 0 && parts[it] is ChatPart.Emote }
}
