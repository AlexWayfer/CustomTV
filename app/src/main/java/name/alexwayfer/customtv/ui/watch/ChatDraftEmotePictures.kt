package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.TextRange
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.chat.EmoteWordKind
import name.alexwayfer.customtv.chat.NO_TARGET
import name.alexwayfer.customtv.chat.emoteWordTargets
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.SevenTvEmote

/** The one character the field shows in place of an emote code it draws as a picture. */
internal const val CHAT_DRAFT_EMOTE_PICTURE_CHAR = '￼'

/** Which side of its emote a modifier stands on: BTTV's before it, FFZ's after it. */
internal enum class ChatDraftModifierSide { BeforeEmote, AfterEmote }

/** A modifier word of the draft: the side it applies from and what it does to the emote. */
internal data class ChatDraftModifier(val side: ChatDraftModifierSide, val effects: EmoteEffects)

/**
 * The modifier [token] is, by the platforms on: a BTTV one such as `w!`, or an FFZ one from
 * [ffzEmotes] or, while they load, one of the names BTTV knows too, such as `ffzW`.
 */
internal fun chatDraftModifier(
    token: String,
    bttvModifiers: Boolean,
    ffzModifiers: Boolean,
    ffzEmotes: Map<String, SevenTvEmote>,
): ChatDraftModifier? {
    val bttv = if (bttvModifiers) EmoteEffects.bttv(token) else null
    if (bttv != null) return ChatDraftModifier(ChatDraftModifierSide.BeforeEmote, bttv)
    val ffz = if (ffzModifiers) ffzEmotes[token]?.effects ?: EmoteEffects.knownFfz(token) else null
    return ffz?.let { ChatDraftModifier(ChatDraftModifierSide.AfterEmote, it) }
}

/**
 * One emote of the draft as the field shows it: [source] is its range in the draft and [shown]
 * its range in the shown text. With a [picture] the field shows one stand-in character under that
 * picture; without one the code stays text on a plaque. A picture can stand for a group: an emote
 * with the modifiers that apply to it, hidden into its [effects]. [lastCode] is the group's last
 * word, the one a backspace takes first.
 */
internal data class ChatDraftShownEmote(
    val source: TextRange,
    val shown: TextRange,
    val picture: PickerEmote?,
    val effects: EmoteEffects = EmoteEffects(),
    val lastCode: TextRange = source,
)

/**
 * The draft's emotes as the field shows them. A code becomes its picture once whitespace follows
 * it, so the word being typed stays text until it is finished. A code [pictureFor] has no picture
 * for, such as a modifier before its list loads, stays text. Finished modifiers next to a finished
 * emote, BTTV's before it and FFZ's after it, join it into one picture with their effects, as chat
 * shows them; a modifier with no emote to apply to shows its own picture.
 */
internal fun chatDraftShownEmotes(
    text: CharSequence,
    isEmoteCode: (String) -> Boolean,
    pictureFor: (String) -> PickerEmote?,
    modifierFor: (String) -> ChatDraftModifier?,
): List<ChatDraftShownEmote> {
    // The words are split on whitespace, so one that ends before the text does has whitespace after it.
    val words = chatDraftWords(text).map { range ->
        DraftWord(range, text.substring(range.start, range.end), finished = range.end < text.length)
    }
    val pictures = words.map { if (it.finished) pictureFor(it.token) else null }
    // Chat's own rule groups the words, so the draft shows the emotes as the sent message will.
    val targets = emoteWordTargets(words.indices.map { draftWordKind(words[it], pictures[it], modifierFor) })
    val shown = ArrayList<ChatDraftShownEmote>()
    var collapsed = 0
    var index = 0
    while (index < words.size) {
        val word = words[index]
        val root = targets[index].takeUnless { it == NO_TARGET } ?: index
        val picture = pictures[root]
        if (picture == null) {
            if (isEmoteCode(word.token)) {
                val start = word.range.start - collapsed
                shown += ChatDraftShownEmote(word.range, TextRange(start, start + word.range.length), picture = null)
            }
            index++
            continue
        }
        // A modifier chat drops, such as `w!` before an FFZ modifier, vanishes into the picture with the rest.
        val last = (index until words.size).lastOrNull { it == root || targets[it] == root } ?: index
        val effects = (index..last).filter { it != root && targets[it] == root }
            .mapNotNull { modifierFor(words[it].token)?.effects }
            .fold(EmoteEffects(), EmoteEffects::combine)
        val source = TextRange(word.range.start, words[last].range.end)
        val start = source.start - collapsed
        shown += ChatDraftShownEmote(
            source = source,
            shown = TextRange(start, start + 1),
            picture = picture,
            effects = effects,
            lastCode = words[last].range,
        )
        collapsed += source.length - 1
        index = last + 1
    }
    return shown
}

private class DraftWord(val range: TextRange, val token: String, val finished: Boolean)

/** What [word] is to chat's grouping; a word still being typed is plain text until it is finished. */
private fun draftWordKind(
    word: DraftWord,
    picture: PickerEmote?,
    modifierFor: (String) -> ChatDraftModifier?,
): EmoteWordKind {
    if (!word.finished) return EmoteWordKind.Other
    val modifier = modifierFor(word.token)
    return when {
        modifier?.side == ChatDraftModifierSide.BeforeEmote -> EmoteWordKind.BttvModifier
        modifier != null && picture != null -> EmoteWordKind.FfzModifierEmote
        modifier != null -> EmoteWordKind.FfzModifier
        picture != null -> EmoteWordKind.Emote
        else -> EmoteWordKind.Other
    }
}

/**
 * The metrics the field shows pictures with, or none while a screen reader is on: the codes then
 * stay text, so it reads each emote by its name instead of the stand-in character.
 */
internal fun chatDraftPictureMetrics(
    measured: ChatDraftEmoteMetrics?,
    screenReaderOn: Boolean,
): ChatDraftEmoteMetrics? = measured.takeUnless { screenReaderOn }

/**
 * What else a backspace removes when it deletes the whitespace right after an emote shown as a
 * picture: that emote's code, as a range of [edited], which still holds it in place. Null for any
 * other edit, such as a deleted selection, a forward delete, or a letter. [shown] is how the field
 * showed [original].
 */
internal fun chatDraftWholeEmoteDeletion(
    original: CharSequence,
    originalSelection: TextRange,
    edited: CharSequence,
    shown: List<ChatDraftShownEmote>,
): TextRange? {
    val gap = originalSelection.start - 1
    val backspacedGap = originalSelection.collapsed &&
        gap >= 0 &&
        original[gap].isWhitespace() &&
        edited.contentEquals(original.removeRange(gap, gap + 1))
    if (!backspacedGap) return null
    return shown.firstOrNull { it.picture != null && it.source.end == gap }?.lastCode
}

/** How long the shown text is once the pictures of [emotes] replace their codes in a draft [length] long. */
internal fun chatDraftShownLength(length: Int, emotes: List<ChatDraftShownEmote>): Int =
    length - emotes.sumOf { it.source.length - it.shown.length }
