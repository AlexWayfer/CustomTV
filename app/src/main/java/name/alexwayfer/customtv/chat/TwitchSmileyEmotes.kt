package name.alexwayfer.customtv.chat

private val smileyCanonical = mapOf(
    ":)" to ":)",
    ":-)" to ":)",
    ":(" to ":(",
    ":-(" to ":(",
    ":D" to ":D",
    ":-D" to ":D",
    ":p" to ":P",
    ":P" to ":P",
    ":-p" to ":P",
    ":-P" to ":P",
    ":o" to ":O",
    ":O" to ":O",
    ":-o" to ":O",
    ":-O" to ":O",
    ":z" to ":|",
    ":Z" to ":|",
    ":-z" to ":|",
    ":-Z" to ":|",
    ":|" to ":|",
    ":-|" to ":|",
    ":/" to ":/",
    ":-/" to ":/",
    ":\\" to ":/",
    ":-\\" to ":/",
    ";)" to ";)",
    ";-)" to ";)",
    ";p" to ";P",
    ";P" to ";P",
    ";-p" to ";P",
    ";-P" to ";P",
    "B)" to "B)",
    "b)" to "B)",
    "B-)" to "B)",
    "b-)" to "B)",
    "R)" to "R)",
    "r)" to "R)",
    "R-)" to "R)",
    "r-)" to "R)",
    "O_o" to "O_o",
    "O_O" to "O_o",
    "o_o" to "O_o",
    "o_O" to "O_o",
    "O.o" to "O_o",
    "O.O" to "O_o",
    "o.o" to "O_o",
    "o.O" to "O_o",
    "8)" to "B)",
    "8-)" to "B)",
    "\\:-?\\)" to ":)",
    "\\:-?\\(" to ":(",
    "\\:-?D" to ":D",
    "\\:-?(o|O)" to ":O",
    "\\:-?(p|P)" to ":P",
    "\\:-?[\\\\/]" to ":/",
    "\\:-?[z|Z|\\|]" to ":|",
    "\\;-?\\)" to ";)",
    "\\;-?(p|P)" to ";P",
    "B-?\\)" to "B)",
    "R-?\\)" to "R)",
    "[oO](_|\\.)[oO]" to "O_o",
    "\\&gt\\;\\(" to ">(",
    "\\&lt\\;3" to "<3",
)

internal fun isTwitchSmileyName(name: String): Boolean = name in smileyCanonical

internal fun twitchPickerEmotes(
    globals: List<TwitchCatalogEmote>,
    personalSmilies: List<TwitchCatalogEmote> = emptyList(),
    turbo: List<TwitchCatalogEmote> = emptyList(),
): List<TwitchCatalogEmote> {
    val chosen = LinkedHashMap<String, TwitchCatalogEmote>()
    val tierByKey = HashMap<String, Int>()
    val globalIds = HashMap<String, MutableSet<String>>()
    for (emote in globals) keepTwitchEmote(chosen, tierByKey, globalIds, emote, tier = 0)
    for (emote in personalSmilies) keepTwitchEmote(chosen, tierByKey, globalIds, emote, tier = 1)
    for (emote in turbo) keepTwitchEmote(chosen, tierByKey, globalIds, emote, tier = 2)
    return chosen.values.toList()
}

private fun keepTwitchEmote(
    chosen: LinkedHashMap<String, TwitchCatalogEmote>,
    tierByKey: MutableMap<String, Int>,
    globalIds: MutableMap<String, MutableSet<String>>,
    emote: TwitchCatalogEmote,
    tier: Int,
) {
    val canon = smileyCanonical[emote.name]
    val key = canon ?: emote.name
    val named = if (canon == null) {
        emote
    } else {
        emote.copy(name = canon, group = TwitchEmoteGroup.Global)
    }
    val imageId = emote.url.substringAfter("/emoticons/v2/", "").substringBefore("/")
    if (tier == 0) {
        globalIds.getOrPut(key) { HashSet() }.add(imageId)
    } else if (tier < 2 && imageId in globalIds[key].orEmpty()) {
        return
    }
    val existingTier = tierByKey[key]
    val existing = chosen[key]
    val betterImage = existing != null && smileyImageRank(emote) > smileyImageRank(existing)
    if (existingTier == null || tier > existingTier || (tier == existingTier && betterImage)) {
        chosen[key] = named
        tierByKey[key] = tier
    }
}

private fun smileyImageRank(emote: TwitchCatalogEmote): Int {
    val id = emote.url.substringAfter("/emoticons/v2/", "").substringBefore("/")
    return id.length + if (id.startsWith("emotesv2_")) 100 else 0
}
