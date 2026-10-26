package name.alexwayfer.customtv.data

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.ThirdPartyBadge
import name.alexwayfer.customtv.ui.theme.parseTwitchHexColor
import org.json.JSONArray
import org.json.JSONObject

/**
 * FFZ's badges, each with the sorted Twitch user IDs of its chatters. FFZ lists tens of thousands of supporters, so the
 * IDs stay numbers in arrays instead of a map.
 */
internal class FfzBadgeList(private val badges: List<Pair<ThirdPartyBadge, LongArray>> = emptyList()) {
    fun forUser(userId: String): List<ThirdPartyBadge> {
        val id = userId.toLongOrNull() ?: return emptyList()
        return badges.mapNotNull { (badge, users) -> badge.takeIf { users.binarySearch(id) >= 0 } }
    }
}

/** The badges FFZ and BTTV give chatters in every channel, each list loaded once a run while its switch is on. */
internal object ThirdPartyBadgeLists {
    private val _ffz = MutableStateFlow(FfzBadgeList())
    val ffz: StateFlow<FfzBadgeList> = _ffz

    private val _bttv = MutableStateFlow<Map<String, ThirdPartyBadge>>(emptyMap())

    /** BTTV's badges by Twitch user ID. */
    val bttv: StateFlow<Map<String, ThirdPartyBadge>> = _bttv

    private val ffzLoad = Mutex()
    private val bttvLoad = Mutex()

    @Volatile
    private var ffzLoaded = false

    @Volatile
    private var bttvLoaded = false

    /** Loads FFZ's list unless it is here; a failed load tries again the next time a chat opens. */
    suspend fun loadFfz() = ffzLoad.withLock {
        if (ffzLoaded) return@withLock
        val body = fetch(FFZ_URL) ?: return@withLock
        _ffz.value = withContext(Dispatchers.Default) { parseFfzBadges(body) }
        ffzLoaded = true
    }

    suspend fun loadBttv() = bttvLoad.withLock {
        if (bttvLoaded) return@withLock
        val body = fetch(BTTV_URL) ?: return@withLock
        _bttv.value = withContext(Dispatchers.Default) { parseBttvBadges(body) }
        bttvLoaded = true
    }

    // Read after the answer comes, off the main thread: FFZ's list is hundreds of kilobytes.
    private suspend fun fetch(url: String): String? =
        (EmoteHttp.get(sharedHttpClient, url) as? EmoteHttpBody.Success)?.body

    internal fun parseFfzBadges(body: String): FfzBadgeList {
        val json = JSONObject(body)
        val users = json.optJSONObject("users") ?: return FfzBadgeList()
        val badges = json.optJSONArray("badges")?.objects().orEmpty()
            .sortedBy { it.optInt("slot") }
            .mapNotNull { item ->
                val ids = users.optJSONArray(item.optInt("id").toString())?.longs()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                val url = item.optJSONObject("urls")?.text("2") ?: item.text("image") ?: return@mapNotNull null
                val title = item.text("title") ?: return@mapNotNull null
                ThirdPartyBadge(
                    url = url,
                    title = title,
                    background = parseTwitchHexColor(item.text("color")) ?: Color.Unspecified,
                    replaces = item.text("replaces"),
                ) to ids
            }
        return FfzBadgeList(badges)
    }

    internal fun parseBttvBadges(body: String): Map<String, ThirdPartyBadge> =
        JSONArray(body).objects().mapNotNull { item ->
            val userId = item.text("providerId") ?: return@mapNotNull null
            val badge = item.optJSONObject("badge") ?: return@mapNotNull null
            val url = badge.text("svg") ?: return@mapNotNull null
            val title = badge.text("description") ?: return@mapNotNull null
            userId to ThirdPartyBadge(url = url, title = title)
        }.toMap()

    private fun JSONArray.longs(): LongArray =
        LongArray(length()) { optLong(it) }.apply { sort() }

    private const val FFZ_URL = "https://api.frankerfacez.com/v1/badges/ids"
    private const val BTTV_URL = "https://api.betterttv.net/3/cached/badges/twitch"
}
