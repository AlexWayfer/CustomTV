package name.alexwayfer.customtv.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** A channel's badge pictures by Twitch badge key: Twitch's own, and FFZ's custom moderator and VIP ones. */
internal data class ChannelBadgeUrls(
    val twitch: Map<String, String> = emptyMap(),
    val ffzRoom: Map<String, String> = emptyMap(),
) {
    /** The pictures the chat shows: FFZ's in place of Twitch's while [ffzBadges] is on. */
    fun shown(ffzBadges: Boolean): Map<String, String> =
        if (ffzBadges && ffzRoom.isNotEmpty()) twitch + ffzRoom else twitch
}

internal object ChatBadgeRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val channelCache = ConcurrentHashMap<String, ChannelBadgeUrls>()
    private val channelInFlight = ConcurrentHashMap<String, CompletableDeferred<ChannelBadgeUrls>>()
    private val titleByUrl = ConcurrentHashMap<String, String>()
    private val globalLock = CompletableDeferred<Map<String, StoredBadge>>()
    @Volatile
    private var globalCache: Map<String, StoredBadge>? = null
    @Volatile
    private var globalStarted = false

    fun titleForImage(url: String): String? = titleByUrl[url]

    suspend fun forChannel(login: String): ChannelBadgeUrls {
        val key = login.lowercase()
        return channelCache[key] ?: loadChannelOnce(key)
    }

    /** The first caller for [key] loads its badges; the others wait for its answer. */
    private suspend fun loadChannelOnce(key: String): ChannelBadgeUrls {
        val created = CompletableDeferred<ChannelBadgeUrls>()
        val existing = channelInFlight.putIfAbsent(key, created)
        if (existing != null) return existing.await()
        return try {
            coroutineScope {
                val global = async { loadGlobal() }
                val channel = fetchChannelBadges(key)
                val ffz = channel.userId?.let { FfzRepository.roomBadges(it) }.orEmpty()
                val urls = badgeUrls(global.await() + channel.badges, ffz)
                channelCache[key] = urls
                created.complete(urls)
                urls
            }
        } finally {
            // A cancelled or failed load cancels the waiters too, so none of them waits forever.
            if (!created.isCompleted) created.cancel()
            channelInFlight.remove(key, created)
        }
    }

    /** The badges the last start kept for [login]'s channel, shown until [forChannel] answers. */
    suspend fun restore(login: String): ChannelBadgeUrls? = withContext(Dispatchers.IO) {
        val key = login.lowercase()
        channelCache[key]?.let { return@withContext it }
        ChannelAvatarRepository.awaitRestored()
        val userId = ChannelAvatarRepository.cached(key)?.id ?: return@withContext null
        val global = globalCache ?: EmoteHttp.stored(GLOBAL_FILE, ::parseGlobalBadges)?.let(::withTitles).orEmpty()
        val channel = EmoteHttp.stored(AssetFiles.channel(userId, FILE), ::parseChannelBadges)
            ?.badges?.let(::withTitles).orEmpty()
        if (global.isEmpty() && channel.isEmpty()) return@withContext null
        badgeUrls(global + channel, FfzRepository.restoreRoomBadges(userId))
    }

    // FFZ's custom moderator and VIP badges keep the title of the Twitch badge they replace.
    private fun badgeUrls(twitch: Map<String, StoredBadge>, ffz: Map<String, String>): ChannelBadgeUrls {
        val custom = withTitles(ffz.mapValues { (badge, url) -> StoredBadge(url, twitch[badge]?.title) })
        return ChannelBadgeUrls(twitch.mapValues { it.value.url }, custom.mapValues { it.value.url })
    }

    /** Remembers the title of each badge by its picture, for the chatter card, and passes the badges on. */
    private fun withTitles(badges: Map<String, StoredBadge>): Map<String, StoredBadge> {
        badges.values.forEach { badge -> badge.title?.let { titleByUrl[badge.url] = it } }
        return badges
    }

    private suspend fun loadGlobal(): Map<String, StoredBadge> = globalCache ?: loadGlobalOnce()

    /** The first caller fetches the global badges; the others wait for its answer. */
    private suspend fun loadGlobalOnce(): Map<String, StoredBadge> {
        val shouldFetch = synchronized(this) {
            if (globalStarted) {
                false
            } else {
                globalStarted = true
                true
            }
        }
        if (!shouldFetch) return globalLock.await()
        var fetched = emptyMap<String, StoredBadge>()
        try {
            fetched = fetchGlobalBadges()
        } finally {
            // A cancelled fetch leaves the waiters without global badges, as a failed one does.
            globalCache = fetched
            globalLock.complete(fetched)
        }
        return fetched
    }

    private suspend fun fetchGlobalBadges(): Map<String, StoredBadge> = withContext(Dispatchers.IO) {
        val payload = JSONObject().put(
            "query",
            "query{badges{setID version title imageURL(size:NORMAL)}}",
        )
        val body = executeBadgeQuery(payload) ?: return@withContext emptyMap()
        val badges = parseGlobalBadges(body)?.let(::withTitles) ?: return@withContext emptyMap()
        AssetFiles.write(GLOBAL_FILE, body)
        badges
    }

    private suspend fun fetchChannelBadges(login: String): ChannelBadgeAnswer = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put(
                "query",
                $$"query($login:String!){user(login:$login)" +
                    "{id broadcastBadges{setID version title imageURL(size:NORMAL)}}}",
            )
            .put("variables", JSONObject().put("login", login))
        val body = executeBadgeQuery(payload)
        val answer = body?.let(::parseChannelBadges)
            ?: return@withContext ChannelBadgeAnswer(null, emptyMap())
        withTitles(answer.badges)
        answer.userId?.let { AssetFiles.write(AssetFiles.channel(it, FILE), body) }
        answer
    }

    private fun executeBadgeQuery(payload: JSONObject): String? {
        val request = twitchGqlRequest(payload.toString()).build()
        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql("ChatBadges", "badges", response.code, body)
                body.takeIf { response.isSuccessful }
            }
        } catch (_: IOException) {
            null
        }
    }

    private const val FILE = "badges"
    private val GLOBAL_FILE = AssetFiles.global(FILE)
}
