package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.SevenTvEmote

// The live emote changes of a channel, by name; each is null when the channel is not loaded.

internal fun EmoteSourceCache.addChannelEmote(
    twitchUserId: String,
    name: String,
    emote: SevenTvEmote,
): Map<String, SevenTvEmote>? = mutateChannel(twitchUserId) { current -> current + (name to emote) }

internal fun EmoteSourceCache.removeChannelEmote(twitchUserId: String, name: String): Map<String, SevenTvEmote>? =
    mutateChannel(twitchUserId) { current -> current - name }

internal fun EmoteSourceCache.renameChannelEmote(
    twitchUserId: String,
    oldName: String,
    newName: String,
    emote: SevenTvEmote,
): Map<String, SevenTvEmote>? = mutateChannel(twitchUserId) { current -> current - oldName + (newName to emote) }
