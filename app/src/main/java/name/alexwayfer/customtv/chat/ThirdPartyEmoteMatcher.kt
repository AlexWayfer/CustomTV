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
                            .map { token -> EmoteModifier(token, EmotePlatform.Bttv, EmoteEffects.bttv(token)!!) }
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
        val tokens = text.split(TOKEN_SPLIT)
        var pendingEffects = EmoteEffects()
        val pendingModifiers = mutableListOf<EmoteModifier>()
        var consumeFollowingWhitespace = false
        for (token in tokens) {
            if (token.isEmpty()) continue
            if (consumeFollowingWhitespace && token.isBlank()) {
                consumeFollowingWhitespace = false
                continue
            }
            val bttvEffects = if (bttvModifiers) EmoteEffects.bttv(token) else null
            if (bttvEffects != null) {
                pendingEffects = pendingEffects.combine(bttvEffects)
                pendingModifiers += EmoteModifier(token, EmotePlatform.Bttv, bttvEffects)
                consumeFollowingWhitespace = true
                continue
            }
            val emote = emotes[token]
            if (emote == null && ffzModifiers) {
                val fallback = EmoteEffects.knownFfz(token)
                val baseIndex = previousEmoteIndex(out)
                if (fallback != null && baseIndex != null) {
                    val base = out[baseIndex] as ChatPart.Emote
                    out[baseIndex] = base.copy(
                        effects = base.effects.combine(fallback),
                        modifiers = base.modifiers + EmoteModifier(token, EmotePlatform.Ffz, fallback),
                    )
                    out.subList(baseIndex + 1, out.size).clear()
                    pendingEffects = EmoteEffects()
                    pendingModifiers.clear()
                    continue
                }
            }
            if (emote != null) {
                val part = ChatPart.Emote(
                    name = token,
                    url = emote.url,
                    aspectRatio = emote.aspectRatio,
                    effects = pendingEffects,
                    modifiers = pendingModifiers.toList(),
                )
                pendingEffects = EmoteEffects()
                pendingModifiers.clear()
                emote.effects?.let { effects ->
                    val baseIndex = previousEmoteIndex(out)
                    if (baseIndex != null) {
                        val base = out[baseIndex] as ChatPart.Emote
                        out[baseIndex] = base.copy(
                            effects = base.effects.combine(effects),
                            modifiers = base.modifiers + EmoteModifier(
                                token,
                                EmotePlatform.Ffz,
                                effects,
                            ),
                        )
                        out.subList(baseIndex + 1, out.size).clear()
                        continue
                    }
                }
                if (emote.overlay) {
                    val baseIndex = previousEmoteIndex(out)
                    if (baseIndex != null) {
                        val base = out[baseIndex] as ChatPart.Emote
                        out[baseIndex] = base.copy(overlays = base.overlays + part)
                        continue
                    }
                }
                out += part
            } else {
                pendingEffects = EmoteEffects()
                pendingModifiers.clear()
                val last = out.lastOrNull()
                if (last is ChatPart.Text) {
                    out[out.lastIndex] = ChatPart.Text(last.text + token)
                } else {
                    out += ChatPart.Text(token)
                }
            }
        }
    }

    private fun previousEmoteIndex(parts: List<ChatPart>): Int? {
        for (index in parts.indices.reversed()) {
            when (val part = parts[index]) {
                is ChatPart.Emote -> return index
                is ChatPart.Gif -> return null
                is ChatPart.Text -> if (part.text.isNotBlank()) return null
            }
        }
        return null
    }

    private val TOKEN_SPLIT = Regex("(?<=\\s)|(?=\\s)")
}
