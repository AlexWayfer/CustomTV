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
    val root = body.toJsonObjectOrNull() ?: return null
    val data = root.optJSONArray("data") ?: return null
    val smilies = mutableListOf<TwitchCatalogEmote>()
    val turbo = mutableListOf<TwitchCatalogEmote>()
    val unlocked = mutableListOf<UnlockedSourceEmote>()
    val typeCounts = linkedMapOf<String, Int>()
    for (item in data.objects()) {
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
    val cursor = root.optJSONObject("pagination")?.text("cursor")
    return UserTwitchEmotePage(smilies, turbo, cursor, typeCounts, unlocked)
}

internal fun parseHelixEmotes(body: String, global: Boolean): List<TwitchCatalogEmote>? {
    val data = body.toJsonObjectOrNull()?.optJSONArray("data") ?: return null
    return buildList {
        for (item in data.objects()) {
            val group = if (global) TwitchEmoteGroup.Global else channelEmoteGroup(item.optString("emote_type"))
            group?.let { helixEmote(item, it) }?.let(::add)
        }
    }
}

/** A channel's own emote groups; other emote types the channel answer lists are not shown. */
private fun channelEmoteGroup(type: String): TwitchEmoteGroup? = when (type) {
    "follower" -> TwitchEmoteGroup.Follower
    "subscriptions" -> TwitchEmoteGroup.Subscriptions
    else -> null
}

private fun helixEmote(item: JSONObject, group: TwitchEmoteGroup): TwitchCatalogEmote? {
    val name = item.text("name") ?: return null
    val id = item.text("id") ?: return null
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

/** How Helix answered one page of the user's emotes. */
internal sealed interface UserEmotePageAnswer {
    class Ready(val parsed: UserTwitchEmotePage, val body: String) : UserEmotePageAnswer
    data object Unavailable : UserEmotePageAnswer
    data object Rejected : UserEmotePageAnswer
}

/** What the pages of the user's emotes gave. */
internal sealed interface UserEmotePages {
    /**
     * Read to the last page, or to the page limit when not [complete]: the emotes, the pages' [bodies] to keep, and
     * the emotes counted by type for the log.
     */
    data class Read(
        val emotes: UserTwitchEmotes,
        val pages: Int,
        val bodies: List<String>,
        val typeCounts: Map<String, Int>,
        val complete: Boolean,
    ) : UserEmotePages

    /** A page did not answer: what the pages before it gave, which is not kept. */
    data class Unavailable(val partial: UserTwitchEmotes) : UserEmotePages

    /** Twitch refused a page; on the first one the user has no emotes to show. */
    data class Rejected(val firstPage: Boolean) : UserEmotePages
}

/**
 * Reads the user's emotes page by page through [fetch], which takes the cursor of the page, until a page names no
 * next one or one already read, or until [maxPages].
 */
internal fun readUserEmotePages(maxPages: Int, fetch: (cursor: String?) -> UserEmotePageAnswer): UserEmotePages {
    val smilies = mutableListOf<TwitchCatalogEmote>()
    val turbo = mutableListOf<TwitchCatalogEmote>()
    val unlocked = mutableListOf<UnlockedSourceEmote>()
    val typeCounts = linkedMapOf<String, Int>()
    val bodies = mutableListOf<String>()
    val seenCursors = HashSet<String>()
    var cursor: String? = null
    var pages = 0
    var result: UserEmotePages? = null
    while (result == null && pages < maxPages) {
        pages++
        result = when (val page = fetch(cursor)) {
            UserEmotePageAnswer.Unavailable -> UserEmotePages.Unavailable(UserTwitchEmotes(smilies, turbo, unlocked))
            UserEmotePageAnswer.Rejected -> UserEmotePages.Rejected(firstPage = pages == 1)
            is UserEmotePageAnswer.Ready -> {
                bodies += page.body
                smilies += page.parsed.smilies
                turbo += page.parsed.turbo
                unlocked += page.parsed.unlocked
                page.parsed.typeCounts.forEach { (type, count) ->
                    typeCounts[type] = typeCounts.getOrDefault(type, 0) + count
                }
                val next = page.parsed.cursor
                if (next.isNullOrBlank() || !seenCursors.add(next)) {
                    UserEmotePages.Read(UserTwitchEmotes(smilies, turbo, unlocked), pages, bodies, typeCounts, true)
                } else {
                    cursor = next
                    null
                }
            }
        }
    }
    return result
        ?: UserEmotePages.Read(UserTwitchEmotes(smilies, turbo, unlocked), pages, bodies, typeCounts, complete = false)
}

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
        return channels[broadcasterId] ?: loadChannelEmotes(clientId, accessToken, broadcasterId)
    }

    /** The set the last start kept answers at once, and the network refreshes it quietly. */
    private suspend fun loadChannelEmotes(
        clientId: String,
        accessToken: String,
        broadcasterId: String,
    ): List<TwitchCatalogEmote> {
        val file = AssetFiles.channel(broadcasterId, FILE)
        val url = CHANNEL_URL.toHttpUrl().newBuilder()
            .addQueryParameter("broadcaster_id", broadcasterId)
            .build()
            .toString()
        val fetch: suspend () -> List<TwitchCatalogEmote>? = {
            load(clientId, accessToken, HelixEmoteList(url, global = false, file)) { parsed ->
                channels[broadcasterId] = parsed
            }
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
        return users[userId] ?: loadUserEmotes(clientId, accessToken, userId)
    }

    private suspend fun loadUserEmotes(clientId: String, accessToken: String, userId: String): UserTwitchEmotes {
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
            val result = readUserEmotePages(MAX_USER_PAGES) { cursor ->
                val url = USER_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("user_id", userId)
                    .apply { if (!cursor.isNullOrBlank()) addQueryParameter("after", cursor) }
                    .build()
                    .toString()
                fetchUserPage(clientId, accessToken, url)
            }
            when (result) {
                is UserEmotePages.Unavailable -> result.partial
                is UserEmotePages.Rejected -> {
                    if (result.firstPage) users[userId] = EMPTY_USER
                    users[userId] ?: EMPTY_USER
                }
                is UserEmotePages.Read -> {
                    if (!result.complete) AppLog.w(TAG, "user emotes stopped after ${result.pages} pages")
                    rememberUserEmotes(userId, result.pages, result.emotes, result.typeCounts)
                    AssetFiles.write(AssetFiles.user(userId), JSONArray(result.bodies).toString())
                    users.getValue(userId)
                }
            }
        }
    }

    private fun rememberUserEmotes(userId: String, pages: Int, emotes: UserTwitchEmotes, typeCounts: Map<String, Int>) {
        val kinds = typeCounts.entries.joinToString(" ") { (type, count) -> "$type=$count" }
        AppLog.i(TAG, "user emotes pages=$pages smilies=${emotes.smilies.size} turbo=${emotes.turbo.size} $kinds")
        users[userId] = emotes
    }

    suspend fun globalEmotes(clientId: String, accessToken: String): List<TwitchCatalogEmote> =
        globals ?: loadGlobalEmotes(clientId, accessToken)

    private suspend fun loadGlobalEmotes(clientId: String, accessToken: String): List<TwitchCatalogEmote> {
        val fetch: suspend () -> List<TwitchCatalogEmote>? = {
            load(clientId, accessToken, HelixEmoteList(GLOBAL_URL, global = true, GLOBAL_FILE)) { parsed ->
                globals = parsed
            }
        }
        val stored = withContext(Dispatchers.IO) {
            EmoteHttp.stored(GLOBAL_FILE) { parseHelixEmotes(it, global = true) }
        }
        if (stored != null) {
            synchronized(this) { if (globals == null) globals = stored }
            scope.launch { fetch() }
            return globals ?: stored
        }
        return fetch() ?: globals.orEmpty()
    }

    /** A Helix emote list: where it loads from, whether it is the global set, and the file that keeps it. */
    private data class HelixEmoteList(val url: String, val global: Boolean, val file: String)

    private suspend fun load(
        clientId: String,
        accessToken: String,
        list: HelixEmoteList,
        store: (List<TwitchCatalogEmote>) -> Unit,
    ): List<TwitchCatalogEmote>? = withContext(Dispatchers.IO) {
        val fetched = try {
            val request = helixRequest(list.url, accessToken, clientId)
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                when (response.code) {
                    in HTTP_SUCCESS -> parseHelixEmotes(body, list.global)?.also { AssetFiles.write(list.file, body) }
                    in HTTP_CLIENT_ERROR -> {
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

    private fun fetchUserPage(clientId: String, accessToken: String, url: String): UserEmotePageAnswer {
        return try {
            val request = helixRequest(url, accessToken, clientId)
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                when (response.code) {
                    in HTTP_SUCCESS -> {
                        val parsed = parseUserEmotePage(body)
                        if (parsed == null) UserEmotePageAnswer.Unavailable else UserEmotePageAnswer.Ready(parsed, body)
                    }
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> {
                        AppLog.w(TAG, "user emotes rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/chat/emotes/user",
                            response.code,
                            twitchAuthMessage(body),
                            HELIX_EXPECTED_CODES,
                        )
                        UserEmotePageAnswer.Unavailable
                    }
                    in HTTP_CLIENT_ERROR -> {
                        AppLog.w(TAG, "user emotes rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/chat/emotes/user",
                            response.code,
                            twitchAuthMessage(body),
                            HELIX_EXPECTED_CODES,
                        )
                        UserEmotePageAnswer.Rejected
                    }
                    else -> {
                        AppLog.w(TAG, "user emotes failed HTTP ${response.code}")
                        UserEmotePageAnswer.Unavailable
                    }
                }
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "user emotes failed ${error.javaClass.simpleName}")
            UserEmotePageAnswer.Unavailable
        }
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
