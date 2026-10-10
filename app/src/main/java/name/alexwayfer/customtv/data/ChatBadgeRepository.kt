package name.alexwayfer.customtv.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ChatBadgeRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val channelCache = ConcurrentHashMap<String, Map<String, String>>()
    private val channelInFlight = ConcurrentHashMap<String, CompletableDeferred<Map<String, String>>>()
    private val titleByUrl = ConcurrentHashMap<String, String>()
    private val globalLock = CompletableDeferred<Map<String, StoredBadge>>()
    @Volatile
    private var globalCache: Map<String, StoredBadge>? = null
    @Volatile
    private var globalStarted = false

    fun titleForImage(url: String): String? = titleByUrl[url]

    suspend fun forChannel(login: String): Map<String, String> {
        val key = login.lowercase()
        channelCache[key]?.let { return it }
        val created = CompletableDeferred<Map<String, String>>()
        val existing = channelInFlight.putIfAbsent(key, created)
        if (existing != null) {
            return existing.await()
        }
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
        } catch (error: Throwable) {
            created.completeExceptionally(error)
            throw error
        } finally {
            channelInFlight.remove(key, created)
        }
    }

    /** The badges the last start kept for [login]'s channel, shown until [forChannel] answers. */
    suspend fun restore(login: String): Map<String, String>? = withContext(Dispatchers.IO) {
        val key = login.lowercase()
        channelCache[key]?.let { return@withContext it }
        ChannelAvatarRepository.awaitRestored()
        val userId = ChannelAvatarRepository.cached(key)?.id ?: return@withContext null
        val global = globalCache ?: EmoteHttp.stored(GLOBAL_FILE, ::parseGlobalBadges).orEmpty()
        val channel = EmoteHttp.stored(AssetFiles.channel(userId, FILE), ::parseChannelBadges)?.badges.orEmpty()
        if (global.isEmpty() && channel.isEmpty()) return@withContext null
        badgeUrls(global + channel, FfzRepository.restoreRoomBadges(userId))
    }

    // FFZ's custom moderator and VIP badges keep the title of the Twitch badge they replace.
    private fun badgeUrls(twitch: Map<String, StoredBadge>, ffz: Map<String, String>): Map<String, String> {
        val custom = ffz.mapValues { (badge, url) -> StoredBadge(url, twitch[badge]?.title) }
        custom.values.forEach { badge -> badge.title?.let { titleByUrl[badge.url] = it } }
        return (twitch + custom).mapValues { it.value.url }
    }

    private suspend fun loadGlobal(): Map<String, StoredBadge> {
        globalCache?.let { return it }
        val shouldFetch = synchronized(this) {
            if (globalStarted) {
                false
            } else {
                globalStarted = true
                true
            }
        }
        if (!shouldFetch) {
            return globalLock.await()
        }
        val fetched = runCatching { fetchGlobalBadges() }.getOrDefault(emptyMap())
        globalCache = fetched
        globalLock.complete(fetched)
        return fetched
    }

    private suspend fun fetchGlobalBadges(): Map<String, StoredBadge> = withContext(Dispatchers.IO) {
        val payload = JSONObject().put(
            "query",
            "query{badges{setID version title imageURL(size:NORMAL)}}",
        )
        val body = executeBadgeQuery(payload) ?: return@withContext emptyMap()
        val badges = parseGlobalBadges(body) ?: return@withContext emptyMap()
        AssetFiles.write(GLOBAL_FILE, body)
        badges
    }

    private fun parseGlobalBadges(body: String): Map<String, StoredBadge>? =
        JSONObject(body).optJSONObject("data")?.let { parseBadgeArray(it.optJSONArray("badges")) }

    private fun parseChannelBadges(body: String): ChannelBadgeAnswer? {
        val user = JSONObject(body).optJSONObject("data")?.optJSONObject("user") ?: return null
        return ChannelBadgeAnswer(
            userId = user.optString("id").takeIf { it.isNotBlank() && it != "null" },
            badges = parseBadgeArray(user.optJSONArray("broadcastBadges")),
        )
    }

    private suspend fun fetchChannelBadges(login: String): ChannelBadgeAnswer = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put(
                "query",
                $$"query($login:String!){user(login:$login){id broadcastBadges{setID version title imageURL(size:NORMAL)}}}",
            )
            .put("variables", JSONObject().put("login", login))
        val body = executeBadgeQuery(payload)
        val answer = body?.let { runCatching { parseChannelBadges(it) }.getOrNull() }
            ?: return@withContext ChannelBadgeAnswer(null, emptyMap())
        answer.userId?.let { AssetFiles.write(AssetFiles.channel(it, FILE), body) }
        answer
    }

    private fun executeBadgeQuery(payload: JSONObject): String? {
        val request = twitchGqlRequest(payload.toString()).build()
        return runCatching {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql("ChatBadges", "badges", response.code, body)
                body.takeIf { response.isSuccessful }
            }
        }.getOrNull()
    }

    private fun parseBadgeArray(array: JSONArray?): Map<String, StoredBadge> {
        if (array == null) return emptyMap()
        val mapped = LinkedHashMap<String, StoredBadge>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val setId = item.optString("setID").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val version = item.optString("version")
            val url = item.optString("imageURL").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val title = item.optString("title").takeIf { it.isNotBlank() && it != "null" }
            mapped["$setId/$version"] = StoredBadge(url, title)
            if (title != null) titleByUrl[url] = title
        }
        return mapped
    }

    private data class StoredBadge(val url: String, val title: String?)

    private class ChannelBadgeAnswer(val userId: String?, val badges: Map<String, StoredBadge>)

    private const val FILE = "badges"
    private val GLOBAL_FILE = AssetFiles.global(FILE)
}
