package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import name.alexwayfer.customtv.chat.EmotePickerSection
import name.alexwayfer.customtv.chat.EmoteUse
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.emoteQueryAtCursor
import name.alexwayfer.customtv.chat.matchingPickerEmotes
import name.alexwayfer.customtv.chat.matchingSessionNicks
import name.alexwayfer.customtv.chat.nickQueryAtCursor
import name.alexwayfer.customtv.chat.pickerEmotesForCompletion

/** The emotes the picker shows: the channel's, the user's own, and the global ones of the enabled sources. */
@Composable
internal fun rememberChatPickerCatalog(input: ChatInput): EmotePickerCatalog {
    val emotes = input.compose.emotes
    return rememberEmotePickerSections(
        enabled = input.user.canSend,
        channelId = input.channel.broadcasterId,
        channelLabel = input.channel.owner.displayName.ifBlank { input.channel.login },
        ownUserId = input.user.userId,
        ownLabel = input.user.displayName?.ifBlank { null } ?: input.user.login,
        sevenTvEnabled = emotes.sevenTvEnabled,
        bttvEnabled = emotes.bttvEnabled,
        ffzEnabled = emotes.ffzEnabled,
        sevenTvEmotes = emotes.sevenTv,
        bttvEmotes = emotes.bttv,
        ffzEmotes = emotes.ffz,
        accessToken = input.user.accessToken,
    )
}

/** The emotes the draft can hold: the ones to complete, how often each is used, and how the field marks them. */
internal class ChatDraftEmotes(
    val completable: List<PickerEmote>,
    val usage: List<EmoteUse>,
    val isEmoteCode: (String) -> Boolean,
    val gaps: ChatDraftEmoteGaps,
)

@Composable
internal fun rememberDraftEmotes(input: ChatInput, sections: List<EmotePickerSection>): ChatDraftEmotes {
    val completable = remember(sections) { pickerEmotesForCompletion(sections) }
    val names = remember(sections) {
        sections.flatMapTo(HashSet()) { section -> section.emotes.map { it.name } }
    }
    val bttvEnabled = input.compose.emotes.bttvEnabled
    val ffzEnabled = input.compose.emotes.ffzEnabled
    // The field and its plaques read the current rule from this state, so emotes that load after
    // the text was typed get highlighted without replacing the field's output transformation.
    val currentDraftEmoteCode = rememberUpdatedState(
        remember(names, bttvEnabled, ffzEnabled) {
            { token: String -> chatDraftIsEmoteCode(token, names, bttvEnabled, ffzEnabled) }
        },
    )
    val isEmoteCode = remember { { token: String -> currentDraftEmoteCode.value(token) } }
    val gaps = remember { ChatDraftEmoteGaps(isEmoteCode) }
    val usage = rememberEmoteUsage(
        channelId = input.channel.broadcasterId,
        ownUserId = input.user.userId,
        liveMessages = input.channel.liveMessages,
        emotes = completable,
    )
    return ChatDraftEmotes(completable, usage, isEmoteCode, gaps)
}

/** The commands, nicks, or emotes that complete the word at the cursor; none while a warning holds the field. */
@Composable
internal fun rememberChatCompletion(input: ChatInput, pickerOpen: Boolean, emotes: ChatDraftEmotes): ChatCompletion? {
    val draft = input.compose.draft
    val canSend = input.user.canSend
    val draftText = draft.text.toString()
    val commandMatches = chatCommandQueryAtCursor(draftText, draft.selection.end)
        ?.takeIf { canSend }
        ?.let { matchingChatCommands(it.text) }
        .orEmpty()
    val nickQuery = nickQueryAtCursor(draftText, draft.selection.end)
    // Matching sorts every candidate, so it runs again only when the query or the candidates change.
    val nickQueryText = nickQuery?.text?.takeIf { canSend }
    // The chatters are read only with a nick query, so a busy chat does not recompose the input otherwise.
    val chatters = if (nickQueryText == null) null else input.compose.sessionChatters()
    val owner = input.channel.owner
    val nickMatches = remember(nickQueryText, chatters, owner) {
        if (nickQueryText == null || chatters == null) {
            emptyList()
        } else {
            matchingSessionNicks(chatters, nickQueryText, owner)
        }
    }
    val emoteQuery = emoteQueryAtCursor(
        draftText,
        draft.selection.end,
        withoutColon = input.compose.emotes.completionWithoutColon,
    )
    val emoteQueryText = emoteQuery?.text?.takeIf { canSend && !pickerOpen }
    val emoteMatches = remember(emoteQueryText, emotes.completable, emotes.usage) {
        if (emoteQueryText == null) {
            emptyList()
        } else {
            matchingPickerEmotes(emotes.completable, emoteQueryText, emotes.usage)
        }
    }
    return chatCompletion(commandMatches, nickMatches, emoteMatches).takeUnless { input.warned }
}
