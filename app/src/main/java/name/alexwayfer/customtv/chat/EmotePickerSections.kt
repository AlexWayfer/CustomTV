package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.UserTwitchEmotes

internal data class TwitchCatalogEmote(
    val name: String,
    val url: String,
    val group: TwitchEmoteGroup,
)

internal enum class TwitchEmoteGroup {
    Follower,
    Subscriptions,
    Global,
}

internal data class PickerEmote(
    val name: String,
    val url: String,
    val aspectRatio: Float = 1f,
)

internal enum class EmotePickerPlace {
    Frequent,
    Channel,
    Personal,
    OwnChannel,
    Unlocked,
    Global,
}

internal enum class EmotePickerKind {
    Follow,
    Subscriptions,
    SevenTv,
    Bttv,
    Ffz,
    Twitch,
}

internal data class EmotePickerSection(
    val place: EmotePickerPlace,
    val kind: EmotePickerKind,
    val label: String,
    val emotes: List<PickerEmote>,
)

internal data class ChannelEmoteSource(
    val label: String,
    val twitch: List<TwitchCatalogEmote> = emptyList(),
    val sevenTv: Map<String, SevenTvEmote> = emptyMap(),
    val bttv: Map<String, SevenTvEmote> = emptyMap(),
    val ffz: Map<String, SevenTvEmote> = emptyMap(),
)

/**
 * The emotes that belong to the signed-in user and work in any channel: their unlocked Twitch sets, and their 7TV and
 * BTTV personal emotes.
 */
internal data class OwnEmotes(
    val twitch: UserTwitchEmotes? = null,
    val sevenTv: Map<String, SevenTvEmote> = emptyMap(),
    val bttv: Map<String, SevenTvEmote> = emptyMap(),
)

/** The emotes every channel shares: Twitch's own and the third-party platforms' global sets. */
internal data class GlobalEmoteSource(
    val twitch: List<TwitchCatalogEmote> = emptyList(),
    val sevenTv: Map<String, SevenTvEmote> = emptyMap(),
    val bttv: Map<String, SevenTvEmote> = emptyMap(),
    val ffz: Map<String, SevenTvEmote> = emptyMap(),
)

internal fun emotePickerSections(
    channel: ChannelEmoteSource,
    own: ChannelEmoteSource?,
    global: GlobalEmoteSource,
    sources: EmoteSources,
    ownEmotes: OwnEmotes = OwnEmotes(),
): List<EmotePickerSection> {
    return buildList {
        addChannel(EmotePickerPlace.Channel, channel, sources)
        if (sources.sevenTv) addPlatform(EmotePickerPlace.Personal, "", EmotePickerKind.SevenTv, ownEmotes.sevenTv)
        if (sources.bttv) addPlatform(EmotePickerPlace.Personal, "", EmotePickerKind.Bttv, ownEmotes.bttv)
        if (own != null) {
            addChannel(EmotePickerPlace.OwnChannel, own, EmoteSources(sevenTv = false, ffz = false, bttv = false))
        }
        unlockedEmoteGroups(ownEmotes.twitch?.unlocked.orEmpty()).forEach { group ->
            add(
                EmotePickerSection(
                    place = EmotePickerPlace.Unlocked,
                    kind = EmotePickerKind.Twitch,
                    label = group.title,
                    emotes = group.emotes,
                ),
            )
        }
        addGlobal(
            EmotePickerKind.Twitch,
            pickerEmotes(
                twitchPickerEmotes(
                    global.twitch,
                    ownEmotes.twitch?.smilies.orEmpty(),
                    ownEmotes.twitch?.turbo.orEmpty(),
                ),
            ),
        )
        if (sources.sevenTv) addGlobal(EmotePickerKind.SevenTv, pickerEmotes(global.sevenTv))
        if (sources.bttv) addGlobal(EmotePickerKind.Bttv, pickerEmotes(global.bttv))
        if (sources.ffz) addGlobal(EmotePickerKind.Ffz, pickerEmotes(global.ffz))
    }
}

internal fun sectionsWithFrequentlyUsed(
    sections: List<EmotePickerSection>,
    usage: List<EmoteUse>,
): List<EmotePickerSection> {
    val frequent = frequentlyUsedPickerEmotes(sections, usage)
    if (frequent.isEmpty()) return sections
    return listOf(
        EmotePickerSection(
            place = EmotePickerPlace.Frequent,
            kind = EmotePickerKind.Twitch,
            label = "",
            emotes = frequent,
        ),
    ) + sections
}

internal fun frequentlyUsedPickerEmotes(
    sections: List<EmotePickerSection>,
    usage: List<EmoteUse>,
): List<PickerEmote> {
    return frequentlyUsedEmotes(pickerEmotesForCompletion(sections), usage)
}

internal fun frequentlyUsedEmotes(emotes: List<PickerEmote>, usage: List<EmoteUse>): List<PickerEmote> {
    if (usage.isEmpty()) return emptyList()
    val byName = usage.associateBy { it.name }
    return emotes
        .filter { (byName[it.name]?.count ?: 0) > 0 }
        .sortedWith(
            compareByDescending<PickerEmote> { byName[it.name]?.count ?: 0 }
                .thenByDescending { byName[it.name]?.usedAtMillis ?: 0L }
                .thenBy { it.name.lowercase() },
        )
}

private fun MutableList<EmotePickerSection>.addChannel(
    place: EmotePickerPlace,
    source: ChannelEmoteSource,
    sources: EmoteSources,
) {
    addTwitch(place, source.label, EmotePickerKind.Follow, source.twitch)
    addTwitch(place, source.label, EmotePickerKind.Subscriptions, source.twitch)
    if (sources.sevenTv) addPlatform(place, source.label, EmotePickerKind.SevenTv, source.sevenTv)
    if (sources.bttv) addPlatform(place, source.label, EmotePickerKind.Bttv, source.bttv)
    if (sources.ffz) addPlatform(place, source.label, EmotePickerKind.Ffz, source.ffz)
}

private fun MutableList<EmotePickerSection>.addTwitch(
    place: EmotePickerPlace,
    label: String,
    kind: EmotePickerKind,
    emotes: List<TwitchCatalogEmote>,
) {
    val group = when (kind) {
        EmotePickerKind.Follow -> TwitchEmoteGroup.Follower
        EmotePickerKind.Subscriptions -> TwitchEmoteGroup.Subscriptions
        else -> return
    }
    val picked = emotes
        .filter { it.group == group }
        .map { PickerEmote(it.name, it.url) }
        .sortedWith(pickerNameOrder)
    if (picked.isEmpty()) return
    add(EmotePickerSection(place, kind, label, picked))
}

private fun MutableList<EmotePickerSection>.addPlatform(
    place: EmotePickerPlace,
    label: String,
    kind: EmotePickerKind,
    emotes: Map<String, SevenTvEmote>,
) {
    val picked = pickerEmotes(emotes)
    if (picked.isEmpty()) return
    add(EmotePickerSection(place, kind, label, picked))
}

private fun MutableList<EmotePickerSection>.addGlobal(
    kind: EmotePickerKind,
    emotes: List<PickerEmote>,
) {
    if (emotes.isEmpty()) return
    add(EmotePickerSection(EmotePickerPlace.Global, kind, label = "", emotes))
}

private fun pickerEmotes(emotes: List<TwitchCatalogEmote>): List<PickerEmote> {
    return emotes
        .map { PickerEmote(it.name, it.url) }
        .sortedWith(pickerNameOrder)
}

private fun pickerEmotes(emotes: Map<String, SevenTvEmote>): List<PickerEmote> {
    return emotes.map { (name, emote) ->
        PickerEmote(name, emote.url, emote.aspectRatio)
    }.sortedWith(pickerNameOrder)
}

private val pickerNameOrder = compareBy<PickerEmote> { it.name.lowercase() }
