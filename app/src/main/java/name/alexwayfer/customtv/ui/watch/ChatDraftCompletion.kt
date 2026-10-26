package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.chat.EmotePickerSection
import name.alexwayfer.customtv.chat.EmoteUse
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.emoteQueryAtCursor
import name.alexwayfer.customtv.chat.matchingPickerEmotes
import name.alexwayfer.customtv.chat.matchingSessionNicks
import name.alexwayfer.customtv.chat.nickQueryAtCursor
import name.alexwayfer.customtv.chat.pickerEmotesForCompletion
import name.alexwayfer.customtv.data.BttvRepository

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
        ownPersonal = emotes.ownPersonal,
    )
}

/** The emotes the draft can hold: the ones to complete, how often each is used, and how the field shows them. */
internal class ChatDraftEmotes(
    val completable: List<PickerEmote>,
    val usage: List<EmoteUse>,
    val output: ChatDraftEmoteOutput,
)

@Composable
internal fun rememberDraftEmotes(input: ChatInput, sections: List<EmotePickerSection>): ChatDraftEmotes {
    val completable = remember(sections) { pickerEmotesForCompletion(sections) }
    val bttvEnabled = input.compose.emotes.bttvEnabled
    val ffzEnabled = input.compose.emotes.ffzEnabled
    val bttvModifiers by BttvRepository.modifiers.collectAsStateWithLifecycle()
    // Each code shows the picture of the first section that has it, as the picker lists them. FFZ
    // modifiers are in the sections; BTTV ones are not, since the picker does not offer them.
    val pictures = remember(sections, bttvEnabled, bttvModifiers) {
        val byName = HashMap<String, PickerEmote>()
        for (section in sections) {
            for (emote in section.emotes) byName.putIfAbsent(emote.name, emote)
        }
        if (bttvEnabled) {
            for ((code, modifier) in bttvModifiers) {
                byName.putIfAbsent(code, PickerEmote(code, modifier.url, modifier.aspectRatio))
            }
        }
        byName
    }
    // The field and its marks read the current rule from this state, so emotes that load after
    // the text was typed get marked without replacing the field's output transformation.
    val currentDraftEmoteCode = rememberUpdatedState(
        remember(pictures, bttvEnabled, ffzEnabled) {
            { token: String -> chatDraftIsEmoteCode(token, pictures.keys, bttvEnabled, ffzEnabled) }
        },
    )
    val currentPictures = rememberUpdatedState(pictures)
    val ffzEmotes = input.compose.emotes.ffz
    val currentModifier = rememberUpdatedState(
        remember(bttvEnabled, ffzEnabled, ffzEmotes) {
            { token: String -> chatDraftModifier(token, bttvEnabled, ffzEnabled, ffzEmotes) }
        },
    )
    val isEmoteCode = remember { { token: String -> currentDraftEmoteCode.value(token) } }
    val output = remember {
        ChatDraftEmoteOutput(
            isEmoteCode = isEmoteCode,
            pictureFor = { token -> currentPictures.value[token] },
            modifierFor = { token -> currentModifier.value(token) },
        )
    }
    val usage = rememberEmoteUsage(
        channelId = input.channel.broadcasterId,
        ownUserId = input.user.userId,
        liveMessages = input.channel.liveMessages,
        emotes = completable,
    )
    return ChatDraftEmotes(completable, usage, output)
}

/** The commands, nicks, or emotes that complete the word at the cursor; none while a warning holds the field. */
@Composable
internal fun rememberChatCompletion(input: ChatInput, pickerOpen: Boolean, emotes: ChatDraftEmotes): ChatCompletion? {
    val draft = input.compose.draft
    val canSend = input.user.canSend
    val draftText = draft.text.toString()
    val commandMatches = chatCommandQueryAtCursor(draftText, draft.selection.end)
        ?.takeIf { canSend }
        ?.let { query ->
            val commands = availableChatCommands(
                predictionOpenable = input.compose.onOpenPrediction != null,
                chatModes = input.channel.modeControl != null,
            )
            matchingChatCommands(query.text, commands)
        }
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
            // Read here, not as a key: the times change with every message, and the list need not follow them.
            matchingSessionNicks(chatters, nickQueryText, owner, input.compose.chatterLastSpokeAt())
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
