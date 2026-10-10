package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.SevenTvEmote
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BttvRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val channelCache = ConcurrentHashMap<String, Map<String, SevenTvEmote>>()
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
                synchronized(this@BttvRepository) { if (globalCache == null) globalCache = stored }
            }
        }
        if (!twitchUserId.isNullOrBlank() && !channelCache.containsKey(twitchUserId)) {
            EmoteHttp.stored(AssetFiles.channel(twitchUserId, FILE), ::parseChannel)
                ?.let { channelCache.putIfAbsent(twitchUserId, it) }
        }
        cached(twitchUserId)
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

    fun addChannelEmote(
        twitchUserId: String,
        name: String,
        emote: SevenTvEmote,
    ): Map<String, SevenTvEmote>? {
        return mutateChannel(twitchUserId) { current -> current + (name to emote) }
    }

    fun removeChannelEmote(twitchUserId: String, name: String): Map<String, SevenTvEmote>? {
        return mutateChannel(twitchUserId) { current -> current - name }
    }

    fun renameChannelEmote(
        twitchUserId: String,
        oldName: String,
        newName: String,
        emote: SevenTvEmote,
    ): Map<String, SevenTvEmote>? {
        return mutateChannel(twitchUserId) { current ->
            current - oldName + (newName to emote)
        }
    }

    fun channelEmote(twitchUserId: String, name: String): SevenTvEmote? {
        return channelCache[twitchUserId]?.get(name)
    }

    fun channelEmoteNameById(twitchUserId: String, emoteId: String): String? {
        return channelCache[twitchUserId]?.entries
            ?.firstOrNull { it.value.id == emoteId }
            ?.key
    }

    fun removeChannelEmoteById(
        twitchUserId: String,
        emoteId: String,
    ): Pair<String, Map<String, SevenTvEmote>>? {
        val name = channelEmoteNameById(twitchUserId, emoteId) ?: return null
        val maps = removeChannelEmote(twitchUserId, name) ?: return null
        return name to maps
    }

    private fun mutateChannel(
        twitchUserId: String,
        transform: (Map<String, SevenTvEmote>) -> Map<String, SevenTvEmote>,
    ): Map<String, SevenTvEmote>? {
        if (!channelCache.containsKey(twitchUserId)) return null
        channelCache.compute(twitchUserId) { _, current ->
            transform(current ?: emptyMap())
        }
        return cached(twitchUserId)
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
        channelCache[twitchUserId] = fetched
        freshChannels += twitchUserId
        return fetched
    }

    private suspend fun fetchGlobal(): Map<String, SevenTvEmote>? =
        EmoteHttp.fetchStored(http, "$API/cached/emotes/global", GLOBAL_FILE, emptyMap(), ::parseGlobal)

    private suspend fun fetchChannel(twitchUserId: String): Map<String, SevenTvEmote>? = EmoteHttp.fetchStored(
        http,
        "$API/cached/users/twitch/$twitchUserId",
        AssetFiles.channel(twitchUserId, FILE),
        emptyMap(),
        ::parseChannel,
    )

    internal fun parseGlobal(body: String): Map<String, SevenTvEmote> =
        parseEmotes(JSONArray(body))

    internal fun parseChannel(body: String): Map<String, SevenTvEmote> {
        val json = JSONObject(body)
        val mapped = LinkedHashMap<String, SevenTvEmote>()
        mapped.putAll(parseEmotes(json.optJSONArray("sharedEmotes")))
        mapped.putAll(parseEmotes(json.optJSONArray("channelEmotes")))
        return mapped
    }

    private fun parseEmotes(array: JSONArray?): Map<String, SevenTvEmote> {
        if (array == null) return emptyMap()
        val mapped = LinkedHashMap<String, SevenTvEmote>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optBoolean("modifier")) continue
            val name = item.optString("code").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: continue
            mapped[name] = SevenTvEmote(
                url = "$CDN/$id/2x.webp",
                aspectRatio = aspectRatio(item),
                overlay = name in ZERO_WIDTH,
                id = id,
            )
        }
        return mapped
    }

    private fun aspectRatio(item: JSONObject): Float {
        val width = item.optInt("width")
        val height = item.optInt("height")
        if (width <= 0 || height <= 0) return 1f
        return width.toFloat() / height.toFloat()
    }

    private const val API = "https://api.betterttv.net/3"
    private const val CDN = "https://cdn.betterttv.net/emote"
    private const val FILE = "bttv"
    private val GLOBAL_FILE = AssetFiles.global(FILE)
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
