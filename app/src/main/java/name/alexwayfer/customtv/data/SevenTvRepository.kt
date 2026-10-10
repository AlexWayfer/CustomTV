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

object SevenTvRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val channelCache = ConcurrentHashMap<String, Map<String, SevenTvEmote>>()
    private val channelSetIds = ConcurrentHashMap<String, String>()
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
            EmoteHttp.stored(GLOBAL_FILE, ::parseSet)?.let { stored ->
                synchronized(this@SevenTvRepository) { if (globalCache == null) globalCache = stored }
            }
        }
        if (!twitchUserId.isNullOrBlank() && !channelCache.containsKey(twitchUserId)) {
            EmoteHttp.stored(AssetFiles.channel(twitchUserId, FILE)) { body -> parseChannel(twitchUserId, body) }
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

    fun channelEmoteSetId(twitchUserId: String?): String? {
        if (twitchUserId.isNullOrBlank()) return null
        return channelSetIds[twitchUserId]
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
        val fetched = EmoteHttp.fetchStored(http, "$API/emote-sets/global", GLOBAL_FILE, emptyMap(), ::parseSet)
            ?: return globalCache ?: emptyMap()
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

    private suspend fun fetchChannel(twitchUserId: String): Map<String, SevenTvEmote>? = EmoteHttp.fetchStored(
        http,
        "$API/users/twitch/$twitchUserId",
        AssetFiles.channel(twitchUserId, FILE),
        emptyMap(),
    ) { body -> parseChannel(twitchUserId, body) }

    private fun parseChannel(twitchUserId: String, body: String): Map<String, SevenTvEmote> {
        val json = JSONObject(body)
        val set = json.optJSONObject("emote_set")
        val setId = set?.optString("id")?.takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("emote_set_id").takeIf { it.isNotBlank() && it != "null" }
        if (setId != null) {
            channelSetIds[twitchUserId] = setId
        }
        return parseEmotes(set?.optJSONArray("emotes"))
    }

    private fun parseSet(body: String): Map<String, SevenTvEmote> =
        parseEmotes(JSONObject(body).optJSONArray("emotes"))

    internal fun parseSetItem(item: JSONObject): Pair<String, SevenTvEmote>? {
        val name = item.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: return null
        val host = item.optJSONObject("data")?.optJSONObject("host")
            ?: item.optJSONObject("host")
        val url = emoteUrl(host) ?: return null
        val id = item.optJSONObject("data")?.optString("id")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: item.optString("id").takeIf { it.isNotBlank() && it != "null" }
        val data = item.optJSONObject("data")
        val tags = data?.optJSONArray("tags")?.let { array ->
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
        }.orEmpty()
        val originalName = data?.optString("name")?.takeIf { it.isNotBlank() && it != "null" && it != name }
        return name to SevenTvEmote(
            url = url,
            aspectRatio = aspectRatio(host),
            overlay = isOverlay(item),
            id = id,
            tags = tags,
            originalName = originalName,
        )
    }

    private fun parseEmotes(array: JSONArray?): Map<String, SevenTvEmote> {
        if (array == null) return emptyMap()
        val mapped = LinkedHashMap<String, SevenTvEmote>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val parsed = parseSetItem(item) ?: continue
            mapped[parsed.first] = parsed.second
        }
        return mapped
    }

    private fun emoteUrl(host: JSONObject?): String? {
        val raw = host?.optString("url")?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        val base = if (raw.startsWith("//")) "https:$raw" else raw.trimEnd('/')
        return "$base/2x.webp"
    }

    private fun aspectRatio(host: JSONObject?): Float {
        val files = host?.optJSONArray("files") ?: return 1f
        var fallback: Float? = null
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val width = file.optInt("width")
            val height = file.optInt("height")
            if (width <= 0 || height <= 0) continue
            val ratio = width.toFloat() / height.toFloat()
            if (file.optString("name") == "2x.webp") return ratio
            if (fallback == null && file.optString("format").equals("WEBP", ignoreCase = true)) {
                fallback = ratio
            }
        }
        return fallback ?: 1f
    }

    private fun isOverlay(item: JSONObject): Boolean {
        val dataFlags = item.optJSONObject("data")?.optInt("flags") ?: 0
        return (dataFlags or item.optInt("flags")) and FLAG_ZERO_WIDTH != 0
    }

    private const val API = "https://7tv.io/v3"
    private const val FLAG_ZERO_WIDTH = 256
    private const val FILE = "7tv"
    private val GLOBAL_FILE = AssetFiles.global(FILE)
}
