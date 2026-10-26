package name.alexwayfer.customtv.chat

/**
 * Matches BTTV, FFZ, and 7TV emotes and the modifiers of the enabled platforms in chat text. The author's personal
 * emotes win over the channel's, as in chatterino7.
 */
object ThirdPartyEmoteMatcher {
    fun overlay(
        parts: List<ChatPart>,
        emotes: Map<String, SevenTvEmote>,
        modifierPlatforms: Set<EmotePlatform> = emptySet(),
        personal: Map<String, SevenTvEmote> = emptyMap(),
    ): List<ChatPart> {
        if (parts.isEmpty()) return parts
        val bttvModifiers = EmotePlatform.Bttv in modifierPlatforms
        val ffzModifiers = EmotePlatform.Ffz in modifierPlatforms
        val lookup = EmoteLookup(emotes, personal)
        val items = chatItems(parts)
        val words = items.filter { it.isWord }
        val kinds = words.map { it.kind(lookup, bttvModifiers, ffzModifiers) }
        val targets = emoteWordTargets(kinds)
        val wordIndexes = HashMap<ChatItem, Int>(words.size)
        words.forEachIndexed { index, item -> wordIndexes[item] = index }
        return ChatItemBuilder(words, kinds, targets, lookup).build(items, wordIndexes).ifEmpty { parts }
    }

    /**
     * The parts' words and whitespace in order: text split into tokens, other parts whole, so a
     * BTTV modifier reaches a Twitch emote in the next part as it reaches one in its own text.
     */
    private fun chatItems(parts: List<ChatPart>): List<ChatItem> {
        val items = ArrayList<ChatItem>()
        for (part in parts) {
            if (part !is ChatPart.Text || part.text.isEmpty()) {
                items += ChatItem(part = part)
                continue
            }
            for (token in part.text.split(TOKEN_SPLIT)) {
                if (token.isNotEmpty()) items += ChatItem(token = token)
            }
        }
        return items
    }

    private val TOKEN_SPLIT = Regex("(?<=\\s)|(?=\\s)")
}

/** The emote a word names: the author's personal one first, then the channel's or a global one. */
private class EmoteLookup(
    private val emotes: Map<String, SevenTvEmote>,
    private val personal: Map<String, SevenTvEmote>,
) {
    operator fun get(word: String): SevenTvEmote? = personal[word] ?: emotes[word]

    fun isPersonal(word: String): Boolean = word in personal
}

/** One word, whitespace, or whole part of a message being matched; compared by identity. */
private class ChatItem(val token: String? = null, val part: ChatPart? = null) {
    /** Whitespace and empty text are not words: they neither apply to emotes nor end a run of them. */
    val isWord: Boolean
        get() = when {
            token != null -> token.isNotBlank()
            part is ChatPart.Text -> false
            else -> true
        }

    fun kind(emotes: EmoteLookup, bttvModifiers: Boolean, ffzModifiers: Boolean): EmoteWordKind {
        if (part != null) return if (part is ChatPart.Emote) EmoteWordKind.Emote else EmoteWordKind.Other
        val word = token.orEmpty()
        if (bttvModifiers && EmoteEffects.bttv(word) != null) return EmoteWordKind.BttvModifier
        val emote = emotes[word]
        return when {
            emote?.effects != null -> EmoteWordKind.FfzModifierEmote
            emote?.overlay == true -> EmoteWordKind.Overlay
            emote != null -> EmoteWordKind.Emote
            ffzModifiers && EmoteEffects.knownFfz(word) != null -> EmoteWordKind.FfzModifier
            else -> EmoteWordKind.Other
        }
    }
}

/**
 * Builds the matched parts from the words' targets: an emote takes the BTTV modifiers that wait
 * for it, and an FFZ modifier or an overlay changes the emote it applies to. Modifier words leave
 * no text: a BTTV one takes the whitespace after it, an FFZ one the whitespace before it.
 */
private class ChatItemBuilder(
    private val words: List<ChatItem>,
    private val kinds: List<EmoteWordKind>,
    private val targets: IntArray,
    private val emotes: EmoteLookup,
) {
    private val out = ArrayList<ChatPart>()

    /** Where each emote word went in [out], by word index. */
    private val placed = HashMap<Int, Int>()
    private var consumeFollowingWhitespace = false

    fun build(items: List<ChatItem>, wordIndexes: Map<ChatItem, Int>): List<ChatPart> {
        for (item in items) {
            val index = wordIndexes[item]
            if (index == null) {
                addWhitespace(item)
            } else {
                addWord(item, index)
            }
        }
        return out
    }

    private fun addWhitespace(item: ChatItem) {
        val token = item.token
        if (token == null) {
            out += item.part ?: return
            return
        }
        if (consumeFollowingWhitespace) {
            consumeFollowingWhitespace = false
            return
        }
        addText(token)
    }

    private fun addWord(item: ChatItem, index: Int) {
        consumeFollowingWhitespace = false
        val kind = kinds[index]
        val target = targets[index]
        when {
            kind == EmoteWordKind.BttvModifier -> consumeFollowingWhitespace = true
            target != NO_TARGET -> applyToEmote(item, index, kind, placed.getValue(target))
            item.part is ChatPart.Emote -> place(index, withPrefix(item.part, index))
            kind == EmoteWordKind.Other || kind == EmoteWordKind.FfzModifier -> addOther(item)
            else -> place(index, withPrefix(emotePart(item.token.orEmpty()), index))
        }
    }

    private fun addOther(item: ChatItem) {
        val token = item.token
        if (token != null) addText(token) else out += item.part ?: return
    }

    private fun place(index: Int, part: ChatPart.Emote) {
        placed[index] = out.size
        out += part
    }

    /** An FFZ modifier or an overlay word [index] changes the emote placed at [baseIndex]. */
    private fun applyToEmote(item: ChatItem, index: Int, kind: EmoteWordKind, baseIndex: Int) {
        val base = out[baseIndex] as ChatPart.Emote
        val token = item.token.orEmpty()
        if (kind == EmoteWordKind.Overlay) {
            out[baseIndex] = base.copy(overlays = base.overlays + withPrefix(emotePart(token), index))
            return
        }
        val effects = emotes[token]?.effects ?: EmoteEffects.knownFfz(token) ?: return
        out[baseIndex] = base.copy(
            effects = base.effects.combine(effects),
            modifiers = base.modifiers + EmoteModifier(token, EmotePlatform.Ffz, effects),
        )
        out.subList(baseIndex + 1, out.size).clear()
    }

    private fun emotePart(token: String): ChatPart.Emote {
        val emote = checkNotNull(emotes[token])
        return ChatPart.Emote(
            name = token,
            url = emote.url,
            aspectRatio = emote.aspectRatio,
            personal = emotes.isPersonal(token),
        )
    }

    /** [part] with the BTTV modifiers that wait for word [index]. */
    private fun withPrefix(part: ChatPart.Emote, index: Int): ChatPart.Emote {
        val prefix = words.indices
            .filter { targets[it] == index && kinds[it] == EmoteWordKind.BttvModifier }
            .mapNotNull { modifier ->
                val token = words[modifier].token.orEmpty()
                EmoteEffects.bttv(token)?.let { EmoteModifier(token, EmotePlatform.Bttv, it) }
            }
        if (prefix.isEmpty()) return part
        return part.copy(
            effects = prefix.fold(part.effects) { effects, modifier -> effects.combine(modifier.effects) },
            modifiers = part.modifiers + prefix,
        )
    }

    private fun addText(token: String) {
        val last = out.lastOrNull()
        if (last is ChatPart.Text) {
            out[out.lastIndex] = ChatPart.Text(last.text + token)
        } else {
            out += ChatPart.Text(token)
        }
    }
}
