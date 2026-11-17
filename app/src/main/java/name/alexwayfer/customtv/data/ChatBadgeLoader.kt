package name.alexwayfer.customtv.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.OkHttpClient
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

internal class ChatBadgeLoader(
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) {
    private val channelAnswers = ConcurrentHashMap<String, ChannelBadgeAnswer>()
    private val channelResults = ConcurrentHashMap<String, BadgeFetch<ChannelBadgeAnswer>>()
    private val channelInFlight = ConcurrentHashMap<String, CompletableDeferred<BadgeLoad>>()
    private val titleByUrl = ConcurrentHashMap<String, String>()
    private val globalLoad = Mutex()
    @Volatile
    private var globalCache: Map<String, StoredBadge>? = null
    private var globalResult: BadgeFetch<Map<String, StoredBadge>>? = null

    fun titleForImage(url: String): String? = titleByUrl[url]

    suspend fun forChannel(login: String): ChannelBadgeUrls {
        return loadChannelOnce(login.lowercase()).urls
    }

    /** The first caller for [key] loads its badges; the others wait for its answer. */
    internal suspend fun loadChannelOnce(key: String): BadgeLoad {
        while (true) {
            val created = CompletableDeferred<BadgeLoad>()
            val existing = channelInFlight.putIfAbsent(key, created)
            if (existing != null) {
                existing.join()
                // A cancelled owner must not cancel an active chat that still needs these badges.
                if (existing.isCancelled) continue
                return existing.await()
            }
            return try {
                coroutineScope {
                    val global = async { loadGlobal() }
                    val fetched = loadChannelBadges(key)
                    val channel = fetched.value ?: channelAnswers[key]
                    val ffz = channel?.userId?.let { FfzRepository.roomBadges(it) }.orEmpty()
                    val globalBadges = global.await()
                    val urls = badgeUrls((globalBadges.value ?: globalCache.orEmpty()) + channel?.badges.orEmpty(), ffz)
                    val complete = fetched.value != null && globalBadges.value != null
                    AppLog.i(CHAT_BADGES_TAG, "loaded $key twitch=${urls.twitch.size} ffz=${urls.ffzRoom.size} " +
                        "complete=$complete")
                    BadgeLoad(urls, retryable = fetched.retryable || globalBadges.retryable).also(created::complete)
                }
            } finally {
                channelInFlight.remove(key, created)
                if (!created.isCompleted) created.cancel()
            }
        }
    }

    /** The badges the last start kept for [login]'s channel, shown until [forChannel] answers. */
    suspend fun restore(login: String): ChannelBadgeUrls? = withContext(Dispatchers.IO) {
        val key = login.lowercase()
        restoreGlobalBadges()
        ChannelAvatarRepository.awaitRestored()
        val userId = ChannelAvatarRepository.cached(key)?.id
        val global = globalCache.orEmpty()
        val channel = channelAnswers[key] ?: userId?.let {
            EmoteHttp.stored(AssetFiles.channel(it, FILE), ::parseChannelBadges)
                ?.also { stored -> channelAnswers.putIfAbsent(key, stored) }
        }
        val badges = channel?.badges?.let(::withTitles).orEmpty()
        if (global.isEmpty() && badges.isEmpty()) return@withContext null
        badgeUrls(global + badges, userId?.let { FfzRepository.restoreRoomBadges(it) }.orEmpty())
    }

    private fun restoreGlobalBadges() {
        if (globalCache != null) return
        val stored = EmoteHttp.stored(GLOBAL_FILE, ::parseGlobalBadges)?.let(::withTitles) ?: return
        synchronized(this) { if (globalCache == null) globalCache = stored }
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

    /** Shares successful loads; a failure or cancellation leaves the saved badges and allows another try. */
    private suspend fun loadGlobal(): BadgeFetch<Map<String, StoredBadge>> = globalLoad.withLock {
        globalResult?.let { return@withLock it }
        fetchGlobalBadges().also { fetched ->
            fetched.value?.let { badges ->
                globalCache = badges
                AppLog.i(CHAT_BADGES_TAG, "global loaded count=${badges.size}")
            }
            if (!fetched.retryable) globalResult = fetched
        }
    }

    private suspend fun fetchGlobalBadges(): BadgeFetch<Map<String, StoredBadge>> = withContext(Dispatchers.IO) {
        val payload = JSONObject().put(
            "query",
            "query{badges{setID version title imageURL(size:NORMAL)}}",
        )
        parseBadgeFetch(executeBadgeQuery(payload)) { body ->
            parseGlobalBadges(body)?.let(::withTitles)?.also { AssetFiles.write(GLOBAL_FILE, body) }
        }
    }

    private suspend fun loadChannelBadges(login: String): BadgeFetch<ChannelBadgeAnswer> {
        channelResults[login]?.let { return it }
        return withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put(
                    "query",
                    $$"query($login:String!){user(login:$login)" +
                        "{id broadcastBadges{setID version title imageURL(size:NORMAL)}}}",
                )
                .put("variables", JSONObject().put("login", login))
            parseBadgeFetch(executeBadgeQuery(payload)) { body ->
                parseChannelBadges(body)?.also { answer ->
                    withTitles(answer.badges)
                    answer.userId?.let { AssetFiles.write(AssetFiles.channel(it, FILE), body) }
                }
            }.also { result ->
                result.value?.let { channelAnswers[login] = it }
                if (!result.retryable) channelResults[login] = result
            }
        }
    }

    private fun executeBadgeQuery(payload: JSONObject): EmoteHttpBody {
        val request = twitchGqlRequest(payload.toString()).build()
        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                Diagnostics.reportGql(CHAT_BADGES_TAG, "badges", response.code, body)
                if (!response.isSuccessful) AppLog.w(CHAT_BADGES_TAG, "load failed HTTP ${response.code}")
                badgeQueryResponse(response.code, body)
            }
        } catch (error: IOException) {
            AppLog.w(CHAT_BADGES_TAG, "load failed", error)
            EmoteHttpBody.Failure(retryable = true)
        }
    }

    internal data class BadgeLoad(val urls: ChannelBadgeUrls, val retryable: Boolean)

    private companion object {
        const val FILE = "badges"
        const val TIMEOUT_SECONDS = 10L
        val GLOBAL_FILE = AssetFiles.global(FILE)
    }
}
