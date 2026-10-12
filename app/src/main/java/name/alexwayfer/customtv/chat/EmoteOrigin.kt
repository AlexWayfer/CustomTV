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
        !prime && (kind == "GLOBALS" || kind == "SMILIES") -> EmoteOrigin.TwitchGlobal
        owner == null -> EmoteOrigin.Twitch
        else -> ownedTwitchEmoteOrigin(kind, prime, tier, owner)
    }
}

/** The origin of a Twitch emote whose channel is known: how a viewer gets it there. */
private fun ownedTwitchEmoteOrigin(kind: String, prime: Boolean, tier: Int?, owner: String): EmoteOrigin = when {
    prime -> EmoteOrigin.TwitchPrime(owner)
    kind == "SUBSCRIPTIONS" -> EmoteOrigin.TwitchSubscription(tier, owner)
    kind == "FOLLOWER" -> EmoteOrigin.TwitchFollower(owner)
    "BITS" in kind -> EmoteOrigin.TwitchBits(owner)
    else -> EmoteOrigin.TwitchChannel(owner)
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

private fun subscriptionTierNumber(tier: String?): Int? = tier?.uppercase()?.let(SUBSCRIPTION_TIERS::get)

/** A subscription tier by each name Twitch gives it. */
private val SUBSCRIPTION_TIERS = mapOf(
    "TIER_1" to 1, "1" to 1, "1000" to 1,
    "TIER_2" to 2, "2" to 2, "2000" to 2,
    "TIER_3" to 3, "3" to 3, "3000" to 3,
)

private val TWITCH_EMOTE_ID = Regex("/emoticons/v2/([^/]+)/")
