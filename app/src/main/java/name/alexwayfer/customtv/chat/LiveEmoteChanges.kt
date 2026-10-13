package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import name.alexwayfer.customtv.data.BttvRepository
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.data.addChannelEmote
import name.alexwayfer.customtv.data.removeChannelEmote
import name.alexwayfer.customtv.data.renameChannelEmote

/** Applies the 7TV and BTTV emote changes from the live event socket to the emotes the chat shows. */
internal class LiveEmoteChanges(
    private val sevenTv: MutableStateFlow<Map<String, SevenTvEmote>>,
    private val bttv: MutableStateFlow<Map<String, SevenTvEmote>>,
) {
    /** Null hides the event line. */
    fun apply(channelId: String, message: ChatMessage, change: ChatEmoteChange): ChatMessage? =
        when (change.platform) {
            EmotePlatform.SevenTv -> applySevenTvChange(channelId, message, change)
            EmotePlatform.Bttv -> applyBttvChange(channelId, message, change)
            EmotePlatform.Ffz -> message
        }

    private fun applySevenTvChange(
        channelId: String,
        message: ChatMessage,
        change: ChatEmoteChange,
    ): ChatMessage {
        val maps = when (change.action) {
            EmoteChangeAction.Added -> {
                val emote = change.emote ?: return message
                SevenTvRepository.emotes.addChannelEmote(channelId, change.emoteName, emote)
            }
            EmoteChangeAction.Removed -> {
                SevenTvRepository.emotes.removeChannelEmote(channelId, change.emoteName)
            }
            EmoteChangeAction.Renamed -> {
                val oldName = change.previousName ?: return message
                val emote = change.emote ?: return message
                SevenTvRepository.emotes.renameChannelEmote(channelId, oldName, change.emoteName, emote)
            }
        }
        maps?.let { sevenTv.value = it }
        return message
    }

    private fun applyBttvChange(
        channelId: String,
        message: ChatMessage,
        change: ChatEmoteChange,
    ): ChatMessage? = when (change.action) {
        EmoteChangeAction.Added -> applyBttvAdd(channelId, message, change)
        EmoteChangeAction.Removed -> applyBttvRemoval(channelId, message, change)
        EmoteChangeAction.Renamed -> applyBttvRename(channelId, message, change)
    }

    private fun applyBttvAdd(channelId: String, message: ChatMessage, change: ChatEmoteChange): ChatMessage {
        val emote = change.emote ?: return message
        BttvRepository.emotes.addChannelEmote(channelId, change.emoteName, emote)
            ?.let { bttv.value = it }
        return message
    }

    private fun applyBttvRemoval(channelId: String, message: ChatMessage, change: ChatEmoteChange): ChatMessage {
        val resolvedName = change.emoteName.ifBlank {
            change.emoteId
                ?.let { BttvRepository.channelEmoteNameById(channelId, it) }
                .orEmpty()
        }
        val cachedEmote = resolvedName.takeIf { it.isNotBlank() }
            ?.let { BttvRepository.channelEmote(channelId, it) }
        val maps = if (resolvedName.isNotBlank()) {
            BttvRepository.emotes.removeChannelEmote(channelId, resolvedName)
        } else {
            change.emoteId?.let { BttvRepository.removeChannelEmoteById(channelId, it)?.second }
        }
        maps?.let { bttv.value = it }
        return resolveBttvRemovalMessage(message, resolvedName, cachedEmote)
    }

    private fun applyBttvRename(channelId: String, message: ChatMessage, change: ChatEmoteChange): ChatMessage? {
        val emote = change.emote ?: return message
        val oldName = change.previousName
            ?: change.emoteId?.let { BttvRepository.channelEmoteNameById(channelId, it) }
        if (oldName != null && oldName == change.emoteName) {
            BttvRepository.emotes.addChannelEmote(channelId, change.emoteName, emote)
                ?.let { bttv.value = it }
            return null
        }
        val maps = if (oldName != null) {
            BttvRepository.emotes.renameChannelEmote(channelId, oldName, change.emoteName, emote)
        } else {
            BttvRepository.emotes.addChannelEmote(channelId, change.emoteName, emote)
        }
        maps?.let { bttv.value = it }
        return resolveBttvRenameMessage(message, oldName)
    }
}
