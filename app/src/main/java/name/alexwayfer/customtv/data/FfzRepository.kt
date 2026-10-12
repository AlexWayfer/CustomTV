package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.chat.EmoteEffects
import org.json.JSONObject

object FfzRepository {
    internal val emotes = EmoteSourceCache(
        file = "ffz",
        globalUrl = "$API/set/global",
        channelUrl = { twitchUserId -> "$API/room/id/$twitchUserId" },
        parseGlobal = ::parseGlobal,
        parseChannel = { _, body -> EmoteChannel(parseRoom(body), parseRoomBadges(body)) },
    )

    /**
     * The channel's custom moderator and VIP badges, under the keys of Twitch's own badges they replace.
     * The room is the one the channel's emotes come from, so whichever loads first answers both.
     */
    suspend fun roomBadges(twitchUserId: String): Map<String, String> =
        emotes.loadChannel(twitchUserId)?.badges.orEmpty()

    /** The room badges the last start kept, shown until [roomBadges] answers. */
    suspend fun restoreRoomBadges(twitchUserId: String): Map<String, String> =
        emotes.restoreChannel(twitchUserId)?.badges.orEmpty()

    internal fun parseGlobal(body: String): Map<String, SevenTvEmote> {
        val json = JSONObject(body)
        val sets = json.optJSONObject("sets") ?: return emptyMap()
        val mapped = LinkedHashMap<String, SevenTvEmote>()
        val defaults = json.optJSONArray("default_sets")
        if (defaults == null || defaults.length() == 0) {
            parseAllSets(sets, mapped)
        } else {
            for (index in 0 until defaults.length()) {
                val setId = when (val value = defaults.opt(index)) {
                    is Number -> value.toInt().toString()
                    else -> value.toString()
                }
                parseSet(sets.optJSONObject(setId), mapped)
            }
        }
        return mapped
    }

    internal fun parseRoom(body: String): Map<String, SevenTvEmote> {
        val sets = JSONObject(body).optJSONObject("sets") ?: return emptyMap()
        val mapped = LinkedHashMap<String, SevenTvEmote>()
        parseAllSets(sets, mapped)
        return mapped
    }

    internal fun parseRoomBadges(body: String): Map<String, String> {
        val room = JSONObject(body).optJSONObject("room") ?: return emptyMap()
        val moderator = pickScale(room.optJSONObject("mod_urls"))
            ?: room.text("moderator_badge")
        val vip = pickScale(room.optJSONObject("vip_badge"))
        return buildMap {
            moderator?.let { put(MODERATOR_BADGE, it) }
            vip?.let { put(VIP_BADGE, it) }
        }
    }

    private fun parseAllSets(sets: JSONObject, mapped: MutableMap<String, SevenTvEmote>) {
        val keys = sets.keys()
        while (keys.hasNext()) {
            parseSet(sets.optJSONObject(keys.next()), mapped)
        }
    }

    private fun parseSet(set: JSONObject?, mapped: MutableMap<String, SevenTvEmote>) {
        val emoticons = set?.optJSONArray("emoticons") ?: return
        emoticons.objects().mapNotNull(::parseEmote).forEach { (name, emote) -> mapped[name] = emote }
    }

    /** An emote by its name; a hidden one is none, unless it is a modifier. */
    private fun parseEmote(item: JSONObject): Pair<String, SevenTvEmote>? {
        val modifier = item.optBoolean("modifier")
        if (item.optBoolean("hidden") && !modifier) return null
        val name = item.text("name") ?: return null
        val url = emoteUrl(item) ?: return null
        return name to SevenTvEmote(
            url = url,
            aspectRatio = emoteAspectRatio(item),
            overlay = modifier && (item.optInt("modifier_flags") and EmoteEffects.FFZ_HIDDEN) == 0,
            effects = item.optInt("modifier_flags").takeIf { modifier && it != 0 }
                ?.let(EmoteEffects::ffz),
        )
    }

    private fun emoteUrl(item: JSONObject): String? {
        pickScale(item.optJSONObject("animated"))?.let { return it }
        return pickScale(item.optJSONObject("urls"))
    }

    private fun pickScale(urls: JSONObject?): String? {
        if (urls == null) return null
        for (scale in SCALE_KEYS) {
            val raw = urls.text(scale) ?: continue
            return if (raw.startsWith("//")) "https:$raw" else raw
        }
        return null
    }

    private const val API = "https://api.frankerfacez.com/v1"
    private val SCALE_KEYS = arrayOf("2", "4", "1")
    private const val MODERATOR_BADGE = "moderator/1"
    private const val VIP_BADGE = "vip/1"
}
