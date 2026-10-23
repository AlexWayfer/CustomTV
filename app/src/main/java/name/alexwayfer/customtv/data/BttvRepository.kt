package name.alexwayfer.customtv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.chat.SevenTvEmote
import org.json.JSONArray
import org.json.JSONObject

object BttvRepository {
    private val modifierPictures = MutableStateFlow<Map<String, SevenTvEmote>>(emptyMap())

    /** BTTV's modifiers such as `w!` by code, for their pictures; they come with the global emotes. */
    internal val modifiers: StateFlow<Map<String, SevenTvEmote>> = modifierPictures

    internal val emotes = EmoteSourceCache(
        file = "bttv",
        globalUrl = "$API/cached/emotes/global",
        channelUrl = { twitchUserId -> "$API/cached/users/twitch/$twitchUserId" },
        parseGlobal = { body ->
            modifierPictures.value = parseGlobalModifiers(body)
            parseGlobal(body)
        },
        parseChannel = { _, body -> EmoteChannel(parseChannel(body)) },
    )

    fun channelEmote(twitchUserId: String, name: String): SevenTvEmote? {
        return emotes.cachedChannel(twitchUserId)[name]
    }

    fun channelEmoteNameById(twitchUserId: String, emoteId: String): String? {
        return emotes.cachedChannel(twitchUserId).entries
            .firstOrNull { it.value.id == emoteId }
            ?.key
    }

    fun removeChannelEmoteById(
        twitchUserId: String,
        emoteId: String,
    ): Pair<String, Map<String, SevenTvEmote>>? {
        val name = channelEmoteNameById(twitchUserId, emoteId) ?: return null
        val maps = emotes.removeChannelEmote(twitchUserId, name) ?: return null
        return name to maps
    }

    internal fun parseGlobal(body: String): Map<String, SevenTvEmote> =
        parseEmotes(JSONArray(body))

    /** The modifiers of the global list, which [parseGlobal] leaves out. */
    internal fun parseGlobalModifiers(body: String): Map<String, SevenTvEmote> =
        parseEmotes(JSONArray(body), modifiers = true)

    internal fun parseChannel(body: String): Map<String, SevenTvEmote> {
        val json = JSONObject(body)
        val mapped = LinkedHashMap<String, SevenTvEmote>()
        mapped.putAll(parseEmotes(json.optJSONArray("sharedEmotes")))
        mapped.putAll(parseEmotes(json.optJSONArray("channelEmotes")))
        return mapped
    }

    private fun parseEmotes(array: JSONArray?, modifiers: Boolean = false): Map<String, SevenTvEmote> =
        array?.objects()?.mapNotNull { parseEmote(it, modifiers) }?.toMap().orEmpty()

    /** An emote by its name, or with [modifier] a modifier by its code; each list leaves the other out. */
    private fun parseEmote(item: JSONObject, modifier: Boolean): Pair<String, SevenTvEmote>? {
        if (item.optBoolean("modifier") != modifier) return null
        val name = item.text("code") ?: return null
        val id = item.text("id") ?: return null
        return name to SevenTvEmote(
            url = "$CDN/$id/2x.webp",
            aspectRatio = emoteAspectRatio(item),
            overlay = name in ZERO_WIDTH,
            id = id,
        )
    }

    private const val API = "https://api.betterttv.net/3"
    private const val CDN = "https://cdn.betterttv.net/emote"
    private val ZERO_WIDTH = setOf(
        "SoSnowy",
        "IceCold",
        "SantaHat",
        "TopHat",
        "ReinDeer",
        "CandyCane",
        "cvHazmat",
        "cvMask",
    )
}
