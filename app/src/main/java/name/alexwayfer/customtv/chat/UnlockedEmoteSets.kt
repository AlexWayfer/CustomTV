package name.alexwayfer.customtv.chat

internal data class UnlockedSourceEmote(
    val name: String,
    val url: String,
    val type: String,
)

internal data class UnlockedEmoteGroup(
    val title: String,
    val emotes: List<PickerEmote>,
)

internal fun unlockedEmoteSet(type: String, name: String): String? {
    when (type) {
        "globals", "smilies", "subscriptions", "follower", "bitstier", "turbo" -> return null
        "hypetrain" -> return "TwitchHypeTrain"
        "owl2019" -> return "ow_esports"
        "twofactor", "rewards", "channelpoints" -> return "Unlocked"
    }
    if (type != "limitedtime" && type != "prime") {
        return type.takeIf { it.isNotBlank() && it != "unknown" }
    }
    return when {
        name.startsWith("Pride") -> "StreamWithPride"
        name.startsWith("Luv") -> "streamerluv"
        name.startsWith("Haha") -> "Haha"
        name.startsWith("Hyper") -> "Hyper"
        name.startsWith("2020") -> "2020"
        name.startsWith("OWL") -> "ow_esports"
        else -> "Unlocked"
    }
}

internal fun unlockedEmoteGroups(emotes: List<UnlockedSourceEmote>): List<UnlockedEmoteGroup> {
    val byTitle = linkedMapOf<String, MutableList<UnlockedSourceEmote>>()
    for (emote in emotes) {
        val title = unlockedEmoteSet(emote.type, emote.name) ?: continue
        byTitle.getOrPut(title) { mutableListOf() }.add(emote)
    }
    return byTitle.entries
        .sortedWith(compareBy({ unlockedSetRank(it.key) }, { it.key.lowercase() }))
        .map { (title, group) ->
            val picked = group
                .map { PickerEmote(it.name, it.url) }
                .sortedWith(compareBy { it.name.lowercase() })
            UnlockedEmoteGroup(title, picked)
        }
}

private fun unlockedSetRank(title: String): Int {
    val known = UNLOCKED_SET_ORDER.indexOf(title)
    return if (known >= 0) known else UNLOCKED_SET_ORDER.size
}

private val UNLOCKED_SET_ORDER = listOf(
    "TwitchHypeTrain",
    "ow_esports",
    "StreamWithPride",
    "streamerluv",
    "Haha",
    "Hyper",
    "2020",
    "Unlocked",
)
