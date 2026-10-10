package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.chat.EmoteEffects
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object FfzRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val channelCache = ConcurrentHashMap<String, Map<String, SevenTvEmote>>()
    private val roomBadgeCache = ConcurrentHashMap<String, Map<String, String>>()
    @Volatile
    private var globalCache: Map<String, SevenTvEmote>? = null
    // What the network answered in this run; anything else in the caches above came from the last start.
    private val freshChannels: MutableSet<String> = ConcurrentHashMap.newKeySet()
    @Volatile
    private var globalFresh = false

    suspend fun forTwitchUser(
        twitchUserId: String?,
        forceRefresh: Boolean = false,
    ): Map<String, SevenTvEmote> = coroutineScope {
        val global = async { loadGlobal(forceRefresh) }
        val channel = async {
            if (twitchUserId.isNullOrBlank()) emptyMap() else loadChannel(twitchUserId, forceRefresh)
        }
        global.await() + channel.await()
    }

    fun cached(twitchUserId: String?): Map<String, SevenTvEmote>? {
        val global = globalCache ?: return null
        val channel = if (twitchUserId.isNullOrBlank()) {
            emptyMap()
        } else {
            channelCache[twitchUserId] ?: emptyMap()
        }
        return global + channel
    }

    fun isLoaded(twitchUserId: String?): Boolean {
        return globalFresh && (twitchUserId.isNullOrBlank() || twitchUserId in freshChannels)
    }

    /** What the last start kept for [twitchUserId], shown until [forTwitchUser] answers from the network. */
    suspend fun restore(twitchUserId: String?): Map<String, SevenTvEmote>? = withContext(Dispatchers.IO) {
        if (globalCache == null) {
            EmoteHttp.stored(GLOBAL_FILE, ::parseGlobal)?.let { stored ->
                synchronized(this@FfzRepository) { if (globalCache == null) globalCache = stored }
            }
        }
        if (!twitchUserId.isNullOrBlank()) restoreChannel(twitchUserId)
        cached(twitchUserId)
    }

    private fun restoreChannel(twitchUserId: String) {
        if (channelCache.containsKey(twitchUserId)) return
        val stored = EmoteHttp.stored(AssetFiles.channel(twitchUserId, FILE), ::parseRoomAnswer) ?: return
        channelCache.putIfAbsent(twitchUserId, stored.emotes)
        roomBadgeCache.putIfAbsent(twitchUserId, stored.badges)
    }

    fun isChannelEmote(twitchUserId: String?, name: String): Boolean {
        return !twitchUserId.isNullOrBlank() && channelCache[twitchUserId]?.containsKey(name) == true
    }

    fun isGlobalEmote(name: String): Boolean = globalCache?.containsKey(name) == true

    fun cachedGlobal(): Map<String, SevenTvEmote> = globalCache.orEmpty()

    fun cachedChannel(twitchUserId: String?): Map<String, SevenTvEmote> {
        if (twitchUserId.isNullOrBlank()) return emptyMap()
        return channelCache[twitchUserId].orEmpty()
    }

    private suspend fun loadGlobal(forceRefresh: Boolean): Map<String, SevenTvEmote> {
        if (!forceRefresh && globalFresh) globalCache?.let { return it }
        val fetched = fetchGlobal() ?: return globalCache ?: emptyMap()
        globalCache = fetched
        globalFresh = true
        return fetched
    }

    private suspend fun loadChannel(
        twitchUserId: String,
        forceRefresh: Boolean,
    ): Map<String, SevenTvEmote> {
        if (!forceRefresh && twitchUserId in freshChannels) channelCache[twitchUserId]?.let { return it }
        val fetched = fetchChannel(twitchUserId) ?: return channelCache[twitchUserId] ?: emptyMap()
        channelCache[twitchUserId] = fetched.emotes
        roomBadgeCache[twitchUserId] = fetched.badges
        freshChannels += twitchUserId
        return fetched.emotes
    }

    /**
     * The channel's custom moderator and VIP badges, under the keys of Twitch's own badges they replace.
     * The room is the one the channel's emotes come from, so whichever loads first answers both.
     */
    suspend fun roomBadges(twitchUserId: String): Map<String, String> {
        if (twitchUserId in freshChannels) roomBadgeCache[twitchUserId]?.let { return it }
        loadChannel(twitchUserId, forceRefresh = false)
        return roomBadgeCache[twitchUserId].orEmpty()
    }

    /** The room badges the last start kept, shown until [roomBadges] answers. */
    suspend fun restoreRoomBadges(twitchUserId: String): Map<String, String> = withContext(Dispatchers.IO) {
        restoreChannel(twitchUserId)
        roomBadgeCache[twitchUserId].orEmpty()
    }

    private suspend fun fetchGlobal(): Map<String, SevenTvEmote>? =
        EmoteHttp.fetchStored(http, "$API/set/global", GLOBAL_FILE, emptyMap(), ::parseGlobal)

    private suspend fun fetchChannel(twitchUserId: String): FfzRoom? = EmoteHttp.fetchStored(
        http,
        "$API/room/id/$twitchUserId",
        AssetFiles.channel(twitchUserId, FILE),
        FfzRoom(emptyMap(), emptyMap()),
        ::parseRoomAnswer,
    )

    private fun parseRoomAnswer(body: String): FfzRoom = FfzRoom(parseRoom(body), parseRoomBadges(body))

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
            ?: room.optString("moderator_badge").takeIf { it.isNotBlank() && it != "null" }
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
        for (index in 0 until emoticons.length()) {
            val item = emoticons.optJSONObject(index) ?: continue
            val modifier = item.optBoolean("modifier")
            if (item.optBoolean("hidden") && !modifier) continue
            val name = item.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val url = emoteUrl(item) ?: continue
            mapped[name] = SevenTvEmote(
                url = url,
                aspectRatio = aspectRatio(item),
                overlay = modifier && (item.optInt("modifier_flags") and EmoteEffects.FFZ_HIDDEN) == 0,
                effects = item.optInt("modifier_flags").takeIf { modifier && it != 0 }
                    ?.let(EmoteEffects::ffz),
            )
        }
    }

    private fun emoteUrl(item: JSONObject): String? {
        pickScale(item.optJSONObject("animated"))?.let { return it }
        return pickScale(item.optJSONObject("urls"))
    }

    private fun pickScale(urls: JSONObject?): String? {
        if (urls == null) return null
        for (scale in SCALE_KEYS) {
            val raw = urls.optString(scale).takeIf { it.isNotBlank() && it != "null" } ?: continue
            return if (raw.startsWith("//")) "https:$raw" else raw
        }
        return null
    }

    private fun aspectRatio(item: JSONObject): Float {
        val width = item.optInt("width")
        val height = item.optInt("height")
        if (width <= 0 || height <= 0) return 1f
        return width.toFloat() / height.toFloat()
    }

    private class FfzRoom(val emotes: Map<String, SevenTvEmote>, val badges: Map<String, String>)

    private const val API = "https://api.frankerfacez.com/v1"
    private const val FILE = "ffz"
    private val GLOBAL_FILE = AssetFiles.global(FILE)
    private val SCALE_KEYS = arrayOf("2", "4", "1")
    private const val MODERATOR_BADGE = "moderator/1"
    private const val VIP_BADGE = "vip/1"
}
