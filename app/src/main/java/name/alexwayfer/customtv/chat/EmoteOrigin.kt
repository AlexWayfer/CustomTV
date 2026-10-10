package name.alexwayfer.customtv.chat

internal fun twitchEmoteId(url: String): String? =
    TWITCH_EMOTE_ID.find(url)?.groupValues?.get(1)

internal fun emoteLibraryFromUrl(url: String): EmoteLibrary? {
    val lower = url.lowercase()
    return when {
        "7tv.app" in lower || "7tv.io" in lower -> EmoteLibrary.SevenTv
        "betterttv.net" in lower -> EmoteLibrary.Bttv
        "frankerfacez.com" in lower -> EmoteLibrary.Ffz
        else -> null
    }
}

internal enum class EmoteLibrary { SevenTv, Bttv, Ffz }

internal sealed interface EmoteOrigin {
    data object TwitchGlobal : EmoteOrigin
    data class TwitchSubscription(val tier: Int?, val channel: String) : EmoteOrigin
    data class TwitchFollower(val channel: String) : EmoteOrigin
    data class TwitchBits(val channel: String) : EmoteOrigin
    data class TwitchPrime(val channel: String) : EmoteOrigin
    data class TwitchChannel(val channel: String) : EmoteOrigin
    data object Twitch : EmoteOrigin
    data class LibraryGlobal(val library: EmoteLibrary) : EmoteOrigin
    data class LibraryChannel(val library: EmoteLibrary, val channel: String) : EmoteOrigin
    data class Library(val library: EmoteLibrary) : EmoteOrigin
}

internal fun twitchEmoteOrigin(
    type: String?,
    subscriptionTier: String?,
    ownerDisplayName: String?,
): EmoteOrigin {
    val kind = type?.uppercase().orEmpty()
    val owner = ownerDisplayName?.takeIf { it.isNotBlank() }
    val tier = subscriptionTierNumber(subscriptionTier)
    val prime = kind == "PRIME" || subscriptionTier.equals("PRIME", ignoreCase = true)
    return when {
        prime && owner != null -> EmoteOrigin.TwitchPrime(owner)
        prime -> EmoteOrigin.Twitch
        kind == "GLOBALS" || kind == "SMILIES" -> EmoteOrigin.TwitchGlobal
        kind == "SUBSCRIPTIONS" && owner != null -> EmoteOrigin.TwitchSubscription(tier, owner)
        kind == "SUBSCRIPTIONS" -> EmoteOrigin.Twitch
        kind == "FOLLOWER" && owner != null -> EmoteOrigin.TwitchFollower(owner)
        kind == "FOLLOWER" -> EmoteOrigin.Twitch
        "BITS" in kind && owner != null -> EmoteOrigin.TwitchBits(owner)
        "BITS" in kind -> EmoteOrigin.Twitch
        owner != null -> EmoteOrigin.TwitchChannel(owner)
        else -> EmoteOrigin.Twitch
    }
}

internal fun libraryEmoteOrigin(
    library: EmoteLibrary,
    onThisChannel: Boolean,
    channelName: String,
    global: Boolean,
): EmoteOrigin {
    val channel = channelName.takeIf { it.isNotBlank() }
    return when {
        onThisChannel && channel != null -> EmoteOrigin.LibraryChannel(library, channel)
        global -> EmoteOrigin.LibraryGlobal(library)
        else -> EmoteOrigin.Library(library)
    }
}

private fun subscriptionTierNumber(tier: String?): Int? = when (tier?.uppercase()) {
    "TIER_1", "1", "1000" -> 1
    "TIER_2", "2", "2000" -> 2
    "TIER_3", "3", "3000" -> 3
    else -> null
}

private val TWITCH_EMOTE_ID = Regex("/emoticons/v2/([^/]+)/")
