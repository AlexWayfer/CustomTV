package name.alexwayfer.customtv.data

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.auth.followListCanLoad
import name.alexwayfer.customtv.diagnostics.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The last loaded follow list, shown at start in its saved order until a fresh load replaces it.
 * The paid build keeps the watch streaks of that list in the same file; the free build never reads them.
 */
internal class FollowedChannelsCache(context: Context) {
    private val file = File(context.applicationContext.filesDir, "followed-channels.json")

    fun read(userId: String): List<FollowedChannel> {
        if (userId.isBlank()) return emptyList()
        synchronized(gate) {
            return decodeFollowedChannelsCache(readRaw() ?: return emptyList(), userId)
        }
    }

    /** The saved list with the account it belongs to, whichever account that is. */
    fun readSaved(): SavedFollows? {
        synchronized(gate) {
            val raw = readRaw() ?: return null
            val userId = decodeFollowedChannelsCacheOwner(raw) ?: return null
            return SavedFollows(userId, decodeFollowedChannelsCache(raw, userId))
        }
    }

    fun write(userId: String, channels: List<FollowedChannel>) {
        if (userId.isBlank()) return
        synchronized(gate) {
            writeRaw(encodeFollowedChannelsCache(userId, channels, readRaw()))
        }
    }

    /** The watch streaks saved with the cached list, by channel ID. */
    fun readWatchStreaks(): Map<String, Int> {
        synchronized(gate) {
            return decodeFollowedWatchStreaks(readRaw() ?: return emptyMap())
        }
    }

    /** Saves [streaks] with the cached list; without a saved list there is nothing to attach them to. */
    fun writeWatchStreaks(streaks: Map<String, Int>) {
        synchronized(gate) {
            val raw = readRaw() ?: return
            followedCacheWithStreaks(raw, streaks)?.let(::writeRaw)
        }
    }

    fun clear() {
        synchronized(gate) {
            file.delete()
        }
    }

    private fun readRaw(): String? {
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrElse { failure ->
            AppLog.w(TAG, "follows cache read failed: ${failure.javaClass.simpleName}")
            null
        }
    }

    private fun writeRaw(text: String) {
        runCatching { file.writeText(text) }.onFailure { failure ->
            AppLog.w(TAG, "follows cache write failed: ${failure.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "FollowedChannels"
        val gate = Any()
    }
}

/** The file for [channels]; the watch streaks of [previous] stay when it belongs to the same account. */
internal fun encodeFollowedChannelsCache(
    userId: String,
    channels: List<FollowedChannel>,
    previous: String? = null,
): String {
    val items = JSONArray()
    channels.forEach { channel ->
        items.put(
            JSONObject()
                .put("id", channel.id)
                .put("login", channel.login)
                .put("displayName", channel.displayName)
                .putOpt("avatarUrl", channel.avatarUrl)
                .put("isLive", channel.isLive)
                .putOpt("categoryName", channel.categoryName)
                .putOpt("viewerCount", channel.viewerCount)
                .putOpt("sharedViewerCount", channel.sharedViewerCount)
                .putOpt("collaborationCount", channel.collaborationCount)
                .put("collaboratorAvatarUrls", JSONArray(channel.collaboratorAvatarUrls))
                .putOpt("lastBroadcastAtMillis", channel.lastBroadcastAtMillis)
                .putOpt("streamTitle", channel.streamTitle)
                .putOpt("previewAtMillis", channel.previewAtMillis),
        )
    }
    val root = JSONObject().put("userId", userId).put("channels", items)
    previous?.let { runCatching { JSONObject(it) }.getOrNull() }
        ?.takeIf { it.optString("userId") == userId }
        ?.optJSONObject("watchStreaks")
        ?.let { root.put("watchStreaks", it) }
    return root.toString()
}

/** [raw] with [streaks] in place of the saved ones; null when nothing changes or the file is unreadable. */
internal fun followedCacheWithStreaks(raw: String, streaks: Map<String, Int>): String? {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (decodeFollowedWatchStreaks(raw) == streaks) return null
    if (streaks.isEmpty()) {
        root.remove("watchStreaks")
    } else {
        root.put("watchStreaks", JSONObject().apply { streaks.forEach { (id, count) -> put(id, count) } })
    }
    return root.toString()
}

internal fun decodeFollowedWatchStreaks(raw: String): Map<String, Int> {
    val streaks = runCatching { JSONObject(raw) }.getOrNull()?.optJSONObject("watchStreaks") ?: return emptyMap()
    return buildMap {
        streaks.keys().forEach { id ->
            val count = streaks.optInt(id)
            if (count > 0) put(id, count)
        }
    }
}

/** The saved list in its saved order, or empty when it is unreadable or belongs to another account. */
internal fun decodeFollowedChannelsCache(raw: String, userId: String): List<FollowedChannel> {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
    if (root.optString("userId") != userId) return emptyList()
    val items = root.optJSONArray("channels") ?: return emptyList()
    return (0 until items.length()).mapNotNull { index ->
        val item = items.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id")
        val login = item.optString("login")
        if (id.isBlank() || login.isBlank()) return@mapNotNull null
        val collaborators = item.optJSONArray("collaboratorAvatarUrls")
        FollowedChannel(
            id = id,
            login = login,
            displayName = item.optString("displayName").ifBlank { login },
            avatarUrl = item.optStringOrNull("avatarUrl"),
            isLive = item.optBoolean("isLive"),
            categoryName = item.optStringOrNull("categoryName"),
            viewerCount = item.optIntOrNull("viewerCount"),
            sharedViewerCount = item.optIntOrNull("sharedViewerCount"),
            collaborationCount = item.optIntOrNull("collaborationCount"),
            collaboratorAvatarUrls = (0 until (collaborators?.length() ?: 0)).mapNotNull { at ->
                collaborators?.optString(at)?.takeIf { it.isNotBlank() }
            },
            lastBroadcastAtMillis = if (item.has("lastBroadcastAtMillis")) item.optLong("lastBroadcastAtMillis") else null,
            streamTitle = item.optStringOrNull("streamTitle"),
            previewAtMillis = if (item.has("previewAtMillis")) item.optLong("previewAtMillis") else null,
        )
    }
}

private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

/** A saved follow list and the account it belongs to. */
internal class SavedFollows(val userId: String, val channels: List<FollowedChannel>)

/** The account the saved list belongs to, or null when [raw] is unreadable or names none. */
internal fun decodeFollowedChannelsCacheOwner(raw: String): String? =
    runCatching { JSONObject(raw) }.getOrNull()?.optString("userId")?.takeIf { it.isNotBlank() }

/**
 * The saved list Home can show before it loads anything: only a list of the saved account, while that account may
 * still read its follows. Another account's list, or one the account can no longer load, stays hidden.
 */
internal fun followsShownAtStart(saved: SavedFollows?, sessionUserId: String?, followsCanLoad: Boolean): SavedFollows? =
    saved?.takeIf { followsCanLoad && it.userId == sessionUserId && it.channels.isNotEmpty() }

/** The saved follow list read when the app starts, so Home opens with it on its first frame. */
internal object FollowedChannelsAtStart {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saved = CompletableDeferred<SavedFollows?>()
    private val handedOver = AtomicBoolean(false)

    fun start(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val shown = runCatching {
                val session = TwitchSessionStore(appContext).read()
                followsShownAtStart(
                    saved = FollowedChannelsCache(appContext).readSaved(),
                    sessionUserId = session?.account?.userId,
                    followsCanLoad = followListCanLoad(session?.grantedScopes),
                )
            }.onFailure { failure ->
                AppLog.w("FollowedChannels", "follows read at start failed: ${failure.javaClass.simpleName}")
            }.getOrNull()
            saved.complete(shown)
        }
    }

    /** The list read at start, given out once: a later Home reads the cache itself, which may be newer by then. */
    suspend fun take(): SavedFollows? = if (handedOver.compareAndSet(false, true)) saved.await() else null
}
