package name.alexwayfer.customtv.chat

/**
 * The personal emotes 7TV and BTTV sent for the chatters of the open channel, by Twitch user ID. A chatter's client
 * tells the service it is in the channel, and the service tells every open chat there which emotes that chatter has;
 * nothing of it is kept after the channel closes.
 */
internal data class PersonalEmotes(
    /** 7TV sets by set ID; one set may belong to many chatters, such as an event's set. */
    val sevenTvSets: Map<String, Map<String, SevenTvEmote>> = emptyMap(),
    /** The 7TV sets of each chatter, in the order the service gave them. */
    val sevenTvUsers: Map<String, List<String>> = emptyMap(),
    /** Each BTTV Pro chatter's emotes. */
    val bttv: Map<String, Map<String, SevenTvEmote>> = emptyMap(),
) {
    /** The emotes [userId] may use: 7TV wins over BTTV, and an earlier 7TV set over a later one. */
    fun forUser(userId: String?): Map<String, SevenTvEmote> {
        val sevenTv = sevenTvFor(userId)
        val bttvEmotes = bttvFor(userId)
        return when {
            sevenTv.isEmpty() -> bttvEmotes
            bttvEmotes.isEmpty() -> sevenTv
            else -> bttvEmotes + sevenTv
        }
    }

    fun sevenTvFor(userId: String?): Map<String, SevenTvEmote> {
        if (userId.isNullOrBlank()) return emptyMap()
        val sets = sevenTvUsers[userId] ?: return emptyMap()
        if (sets.size == 1) return sevenTvSets[sets[0]].orEmpty()
        return buildMap {
            sets.asReversed().forEach { id -> sevenTvSets[id]?.let(::putAll) }
        }
    }

    fun bttvFor(userId: String?): Map<String, SevenTvEmote> {
        if (userId.isNullOrBlank()) return emptyMap()
        return bttv[userId].orEmpty()
    }

    /** The emotes of [userId] by platform, as the picker shows the signed-in user's own. */
    fun ownFor(userId: String?): OwnEmotes = OwnEmotes(sevenTv = sevenTvFor(userId), bttv = bttvFor(userId))

    /** These emotes without the platforms turned off in [sources]. */
    fun enabled(sources: EmoteSources): PersonalEmotes = when {
        sources.sevenTv && sources.bttv -> this
        sources.sevenTv -> copy(bttv = emptyMap())
        sources.bttv -> copy(sevenTvSets = emptyMap(), sevenTvUsers = emptyMap())
        else -> PersonalEmotes()
    }

    fun apply(event: PersonalEmoteEvent): PersonalEmotes = when (event) {
        is PersonalEmoteEvent.SevenTvSetChanged -> {
            val current = sevenTvSets[event.setId].orEmpty()
            copy(sevenTvSets = sevenTvSets + (event.setId to current - event.removed + event.added))
        }
        is PersonalEmoteEvent.SevenTvSetAssigned -> copy(
            sevenTvUsers = event.userIds.fold(sevenTvUsers) { users, userId ->
                val sets = users[userId].orEmpty()
                if (event.setId in sets) users else users + (userId to sets + event.setId)
            },
        )
        is PersonalEmoteEvent.BttvUser -> copy(
            bttv = if (event.emotes.isEmpty()) bttv - event.userId else bttv + (event.userId to event.emotes),
        )
    }
}

/** What a live emote socket says about the chatters' personal emotes. */
internal sealed interface PersonalEmoteEvent {
    /** A 7TV set's emotes changed: [removed] go first, so a renamed emote is in both under its two names. */
    data class SevenTvSetChanged(
        val setId: String,
        val added: Map<String, SevenTvEmote> = emptyMap(),
        val removed: Set<String> = emptySet(),
    ) : PersonalEmoteEvent

    /** The chatters with these Twitch user IDs have the 7TV set. */
    data class SevenTvSetAssigned(val setId: String, val userIds: List<String>) : PersonalEmoteEvent

    /** A BTTV Pro chatter's whole list of personal emotes. */
    data class BttvUser(val userId: String, val emotes: Map<String, SevenTvEmote>) : PersonalEmoteEvent
}
