package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.SevenTvEmote
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** A channel's emotes in one source, and the badges its room replaces there (FrankerFaceZ only). */
internal data class EmoteChannel(
    val emotes: Map<String, SevenTvEmote>,
    val badges: Map<String, String> = emptyMap(),
)

/**
 * The emotes of one third-party source: its global set and each channel's, as the last start kept them in [file]
 * and as this run loaded them. The source gives only its addresses and how to read its answers.
 */
internal class EmoteSourceCache(
    private val file: String,
    private val globalUrl: String,
    private val channelUrl: (twitchUserId: String) -> String,
    private val parseGlobal: (body: String) -> Map<String, SevenTvEmote>,
    private val parseChannel: (twitchUserId: String, body: String) -> EmoteChannel,
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(EMOTE_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(EMOTE_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) {
    private val globalFile = AssetFiles.global(file)
    private val channelCache = ConcurrentHashMap<String, EmoteChannel>()
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
            if (twitchUserId.isNullOrBlank()) {
                emptyMap()
            } else {
                loadChannel(twitchUserId, forceRefresh)?.emotes.orEmpty()
            }
        }
        global.await() + channel.await()
    }

    fun cached(twitchUserId: String?): Map<String, SevenTvEmote>? {
        val global = globalCache ?: return null
        return global + cachedChannel(twitchUserId)
    }

    fun isLoaded(twitchUserId: String?): Boolean {
        return globalFresh && (twitchUserId.isNullOrBlank() || twitchUserId in freshChannels)
    }

    /** What the last start kept for [twitchUserId], shown until [forTwitchUser] answers from the network. */
    suspend fun restore(twitchUserId: String?): Map<String, SevenTvEmote>? = withContext(Dispatchers.IO) {
        if (globalCache == null) {
            EmoteHttp.stored(globalFile, parseGlobal)?.let { stored ->
                synchronized(this@EmoteSourceCache) { if (globalCache == null) globalCache = stored }
            }
        }
        if (!twitchUserId.isNullOrBlank()) restoreStoredChannel(twitchUserId)
        cached(twitchUserId)
    }

    /** The channel as the last start kept it, unless this run already has it; the global set stays as it is. */
    suspend fun restoreChannel(twitchUserId: String): EmoteChannel? = withContext(Dispatchers.IO) {
        restoreStoredChannel(twitchUserId)
        channelCache[twitchUserId]
    }

    private fun restoreStoredChannel(twitchUserId: String) {
        if (channelCache.containsKey(twitchUserId)) return
        val stored = EmoteHttp.stored(AssetFiles.channel(twitchUserId, file)) { body ->
            parseChannel(twitchUserId, body)
        } ?: return
        channelCache.putIfAbsent(twitchUserId, stored)
    }

    fun cachedGlobal(): Map<String, SevenTvEmote> = globalCache.orEmpty()

    fun cachedChannel(twitchUserId: String?): Map<String, SevenTvEmote> {
        if (twitchUserId.isNullOrBlank()) return emptyMap()
        return channelCache[twitchUserId]?.emotes.orEmpty()
    }

    /** Changes the channel's emotes as a live event says; null when the channel is not loaded. */
    fun mutateChannel(
        twitchUserId: String,
        transform: (Map<String, SevenTvEmote>) -> Map<String, SevenTvEmote>,
    ): Map<String, SevenTvEmote>? {
        if (!channelCache.containsKey(twitchUserId)) return null
        channelCache.compute(twitchUserId) { _, current ->
            val channel = current ?: EmoteChannel(emptyMap())
            channel.copy(emotes = transform(channel.emotes))
        }
        return cached(twitchUserId)
    }

    private suspend fun loadGlobal(forceRefresh: Boolean): Map<String, SevenTvEmote> =
        globalCache?.takeIf { !forceRefresh && globalFresh }
            ?: EmoteHttp.fetchStored(http, globalUrl, globalFile, emptyMap(), parseGlobal)?.also { fetched ->
                globalCache = fetched
                globalFresh = true
            }
            ?: globalCache.orEmpty()

    /** The channel loaded in this run, from the network unless it already answered; the kept one when it fails. */
    suspend fun loadChannel(twitchUserId: String, forceRefresh: Boolean = false): EmoteChannel? =
        channelCache[twitchUserId]?.takeIf { !forceRefresh && twitchUserId in freshChannels }
            ?: EmoteHttp.fetchStored(
                http,
                channelUrl(twitchUserId),
                AssetFiles.channel(twitchUserId, file),
                EmoteChannel(emptyMap()),
            ) { body -> parseChannel(twitchUserId, body) }?.also { fetched ->
                channelCache[twitchUserId] = fetched
                freshChannels += twitchUserId
            }
            ?: channelCache[twitchUserId]
}

private const val EMOTE_CONNECT_TIMEOUT_SECONDS = 10L
private const val EMOTE_READ_TIMEOUT_SECONDS = 15L

/** The shape of an emote from its own width and height; square when they are missing. */
internal fun emoteAspectRatio(item: JSONObject): Float {
    val width = item.optInt("width")
    val height = item.optInt("height")
    if (width <= 0 || height <= 0) return 1f
    return width.toFloat() / height.toFloat()
}
