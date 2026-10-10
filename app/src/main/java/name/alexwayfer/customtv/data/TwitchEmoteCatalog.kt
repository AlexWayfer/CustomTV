package name.alexwayfer.customtv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.chat.TwitchCatalogEmote
import name.alexwayfer.customtv.chat.TwitchEmoteGroup
import name.alexwayfer.customtv.chat.UnlockedSourceEmote
import name.alexwayfer.customtv.chat.isTwitchSmileyName
import name.alexwayfer.customtv.chat.unlockedEmoteSet
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.diagnostics.HELIX_EXPECTED_CODES
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal data class UserTwitchEmotePage(
    val smilies: List<TwitchCatalogEmote>,
    val turbo: List<TwitchCatalogEmote>,
    val cursor: String?,
    val typeCounts: Map<String, Int> = emptyMap(),
    val unlocked: List<UnlockedSourceEmote> = emptyList(),
)

internal fun parseUserEmotePage(body: String): UserTwitchEmotePage? {
    val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
    val data = root.optJSONArray("data") ?: return null
    val smilies = mutableListOf<TwitchCatalogEmote>()
    val turbo = mutableListOf<TwitchCatalogEmote>()
    val unlocked = mutableListOf<UnlockedSourceEmote>()
    val typeCounts = linkedMapOf<String, Int>()
    for (index in 0 until data.length()) {
        val item = data.optJSONObject(index) ?: continue
        val type = item.optString("emote_type").ifBlank { "unknown" }
        typeCounts[type] = typeCounts.getOrDefault(type, 0) + 1
        val emote = helixEmote(item, TwitchEmoteGroup.Global) ?: continue
        when {
            type == "turbo" -> turbo.add(emote)
            isTwitchSmileyName(emote.name) -> smilies.add(emote)
            unlockedEmoteSet(type, emote.name) != null ->
                unlocked.add(UnlockedSourceEmote(emote.name, emote.url, type))
        }
    }
    val cursor = root.optJSONObject("pagination")?.optString("cursor")
        ?.takeIf { it.isNotBlank() && it != "null" }
    return UserTwitchEmotePage(smilies, turbo, cursor, typeCounts, unlocked)
}

internal fun parseHelixEmotes(body: String, global: Boolean): List<TwitchCatalogEmote>? {
    val data = runCatching { JSONObject(body) }.getOrNull()?.optJSONArray("data") ?: return null
    return buildList {
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val group = if (global) {
                TwitchEmoteGroup.Global
            } else {
                when (item.optString("emote_type")) {
                    "follower" -> TwitchEmoteGroup.Follower
                    "subscriptions" -> TwitchEmoteGroup.Subscriptions
                    else -> continue
                }
            }
            val emote = helixEmote(item, group) ?: continue
            add(emote)
        }
    }
}

private fun helixEmote(item: JSONObject, group: TwitchEmoteGroup): TwitchCatalogEmote? {
    val name = item.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: return null
    val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: return null
    val formats = item.optJSONArray("format")
    var animated = false
    if (formats != null) {
        for (formatIndex in 0 until formats.length()) {
            if (formats.optString(formatIndex).equals("animated", ignoreCase = true)) animated = true
        }
    }
    val format = if (animated) "animated" else "static"
    return TwitchCatalogEmote(
        name = name,
        url = "https://static-cdn.jtvnw.net/emoticons/v2/$id/$format/dark/2.0",
        group = group,
    )
}

/** The user's emotes from the pages a run kept, a JSON array of the Helix answers in order. */
internal fun parseStoredUserEmotes(body: String): UserTwitchEmotes? {
    val pages = JSONArray(body)
    val smilies = mutableListOf<TwitchCatalogEmote>()
    val turbo = mutableListOf<TwitchCatalogEmote>()
    val unlocked = mutableListOf<UnlockedSourceEmote>()
    for (index in 0 until pages.length()) {
        val page = parseUserEmotePage(pages.optString(index)) ?: return null
        smilies += page.smilies
        turbo += page.turbo
        unlocked += page.unlocked
    }
    return UserTwitchEmotes(smilies, turbo, unlocked)
}

internal data class UserTwitchEmotes(
    val smilies: List<TwitchCatalogEmote>,
    val turbo: List<TwitchCatalogEmote>,
    val unlocked: List<UnlockedSourceEmote> = emptyList(),
)

internal object TwitchEmoteCatalogRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val channels = ConcurrentHashMap<String, List<TwitchCatalogEmote>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var globals: List<TwitchCatalogEmote>? = null
    private val users = ConcurrentHashMap<String, UserTwitchEmotes>()

    suspend fun channelEmotes(
        clientId: String,
        accessToken: String,
        broadcasterId: String,
    ): List<TwitchCatalogEmote> {
        if (broadcasterId.isBlank()) return emptyList()
        channels[broadcasterId]?.let { return it }
        val file = AssetFiles.channel(broadcasterId, FILE)
        val url = CHANNEL_URL.toHttpUrl().newBuilder()
            .addQueryParameter("broadcaster_id", broadcasterId)
            .build()
            .toString()
        val fetch: suspend () -> List<TwitchCatalogEmote>? = {
            load(clientId, accessToken, url, global = false, file) { parsed -> channels[broadcasterId] = parsed }
        }
        val stored = withContext(Dispatchers.IO) { EmoteHttp.stored(file) { parseHelixEmotes(it, global = false) } }
        if (stored != null) {
            channels.putIfAbsent(broadcasterId, stored)
            scope.launch { fetch() }
            return channels.getValue(broadcasterId)
        }
        return fetch() ?: channels[broadcasterId].orEmpty()
    }

    /** The user's own emotes. The set the last start kept answers at once, and the network refreshes it quietly. */
    suspend fun userEmotes(clientId: String, accessToken: String, userId: String): UserTwitchEmotes {
        if (userId.isBlank()) return EMPTY_USER
        users[userId]?.let { return it }
        val stored = withContext(Dispatchers.IO) {
            EmoteHttp.stored(AssetFiles.user(userId), ::parseStoredUserEmotes)
        }
        if (stored != null) {
            users.putIfAbsent(userId, stored)
            scope.launch { fetchUserEmotes(clientId, accessToken, userId) }
            return users.getValue(userId)
        }
        return fetchUserEmotes(clientId, accessToken, userId)
    }

    private suspend fun fetchUserEmotes(clientId: String, accessToken: String, userId: String): UserTwitchEmotes {
        return withContext(Dispatchers.IO) {
            val smilies = mutableListOf<TwitchCatalogEmote>()
            val turbo = mutableListOf<TwitchCatalogEmote>()
            val unlocked = mutableListOf<UnlockedSourceEmote>()
            val typeCounts = linkedMapOf<String, Int>()
            val bodies = JSONArray()
            val seenCursors = HashSet<String>()
            var cursor: String? = null
            var pages = 0
            while (pages < MAX_USER_PAGES) {
                pages++
                val url = USER_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("user_id", userId)
                    .apply { if (!cursor.isNullOrBlank()) addQueryParameter("after", cursor) }
                    .build()
                    .toString()
                when (val page = fetchUserPage(clientId, accessToken, url)) {
                    UserPage.Unavailable -> return@withContext UserTwitchEmotes(smilies, turbo, unlocked)
                    UserPage.Rejected -> {
                        if (pages == 1) users[userId] = EMPTY_USER
                        return@withContext users[userId] ?: EMPTY_USER
                    }
                    is UserPage.Ready -> {
                        bodies.put(page.body)
                        smilies += page.parsed.smilies
                        turbo += page.parsed.turbo
                        unlocked += page.parsed.unlocked
                        page.parsed.typeCounts.forEach { (type, count) ->
                            typeCounts[type] = typeCounts.getOrDefault(type, 0) + count
                        }
                        val next = page.parsed.cursor
                        if (next.isNullOrBlank() || !seenCursors.add(next)) {
                            rememberUserEmotes(userId, pages, smilies, turbo, unlocked, typeCounts)
                            AssetFiles.write(AssetFiles.user(userId), bodies.toString())
                            return@withContext users.getValue(userId)
                        }
                        cursor = next
                    }
                }
            }
            AppLog.w(TAG, "user emotes stopped after $pages pages")
            rememberUserEmotes(userId, pages, smilies, turbo, unlocked, typeCounts)
            AssetFiles.write(AssetFiles.user(userId), bodies.toString())
            users.getValue(userId)
        }
    }

    private fun rememberUserEmotes(
        userId: String,
        pages: Int,
        smilies: List<TwitchCatalogEmote>,
        turbo: List<TwitchCatalogEmote>,
        unlocked: List<UnlockedSourceEmote>,
        typeCounts: Map<String, Int>,
    ) {
        val kinds = typeCounts.entries.joinToString(" ") { (type, count) -> "$type=$count" }
        AppLog.i(TAG, "user emotes pages=$pages smilies=${smilies.size} turbo=${turbo.size} $kinds")
        users[userId] = UserTwitchEmotes(smilies, turbo, unlocked)
    }

    suspend fun globalEmotes(clientId: String, accessToken: String): List<TwitchCatalogEmote> {
        globals?.let { return it }
        val fetch: suspend () -> List<TwitchCatalogEmote>? = {
            load(clientId, accessToken, GLOBAL_URL, global = true, GLOBAL_FILE) { parsed -> globals = parsed }
        }
        val stored = withContext(Dispatchers.IO) { EmoteHttp.stored(GLOBAL_FILE) { parseHelixEmotes(it, global = true) } }
        if (stored != null) {
            synchronized(this) { if (globals == null) globals = stored }
            scope.launch { fetch() }
            return globals ?: stored
        }
        return fetch() ?: globals.orEmpty()
    }

    private suspend fun load(
        clientId: String,
        accessToken: String,
        url: String,
        global: Boolean,
        file: String,
        store: (List<TwitchCatalogEmote>) -> Unit,
    ): List<TwitchCatalogEmote>? = withContext(Dispatchers.IO) {
        val fetched = try {
            val request = helixRequest(url, accessToken, clientId)
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                when (response.code) {
                    in 200..299 -> parseHelixEmotes(body, global)?.also { AssetFiles.write(file, body) }
                    in 400..499 -> {
                        AppLog.w(TAG, "emotes rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/chat/emotes",
                            response.code,
                            twitchAuthMessage(body),
                            HELIX_EXPECTED_CODES,
                        )
                        emptyList()
                    }
                    else -> {
                        AppLog.w(TAG, "emotes failed HTTP ${response.code}")
                        null
                    }
                }
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "emotes failed ${error.javaClass.simpleName}")
            null
        }
        if (fetched != null) store(fetched)
        fetched
    }

    private fun fetchUserPage(clientId: String, accessToken: String, url: String): UserPage {
        return try {
            val request = helixRequest(url, accessToken, clientId)
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                when (response.code) {
                    in 200..299 -> {
                        val parsed = parseUserEmotePage(body)
                        if (parsed == null) UserPage.Unavailable else UserPage.Ready(parsed, body)
                    }
                    401, 403 -> {
                        AppLog.w(TAG, "user emotes rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/chat/emotes/user",
                            response.code,
                            twitchAuthMessage(body),
                            HELIX_EXPECTED_CODES,
                        )
                        UserPage.Unavailable
                    }
                    in 400..499 -> {
                        AppLog.w(TAG, "user emotes rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/chat/emotes/user",
                            response.code,
                            twitchAuthMessage(body),
                            HELIX_EXPECTED_CODES,
                        )
                        UserPage.Rejected
                    }
                    else -> {
                        AppLog.w(TAG, "user emotes failed HTTP ${response.code}")
                        UserPage.Unavailable
                    }
                }
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "user emotes failed ${error.javaClass.simpleName}")
            UserPage.Unavailable
        }
    }

    private sealed interface UserPage {
        class Ready(val parsed: UserTwitchEmotePage, val body: String) : UserPage
        data object Unavailable : UserPage
        data object Rejected : UserPage
    }

    private const val TAG = "TwitchEmotes"
    private const val FILE = "twitch"
    private val GLOBAL_FILE = AssetFiles.global(FILE)
    private const val MAX_USER_PAGES = 40
    private val EMPTY_USER = UserTwitchEmotes(emptyList(), emptyList())
    private const val CHANNEL_URL = "https://api.twitch.tv/helix/chat/emotes"
    private const val USER_URL = "https://api.twitch.tv/helix/chat/emotes/user"
    private const val GLOBAL_URL = "https://api.twitch.tv/helix/chat/emotes/global"
}
