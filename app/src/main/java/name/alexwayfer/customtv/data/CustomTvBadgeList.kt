package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.time.Duration.Companion.hours

/** Which CustomTV badge a chatter shows: gold for a Premium buyer's account, white for anyone else using the app. */
internal enum class CustomTvBadgeEdition { Premium, Free }

/** The list is loaded again when a chat opens this long after the last load. */
internal val CUSTOM_TV_BADGES_RELOAD = 1.hours

/**
 * The Twitch user IDs that show a CustomTV badge, each list sorted for a binary search. A user in both shows the
 * Premium one.
 */
internal class CustomTvBadgeList(
    private val premium: LongArray = LongArray(0),
    private val free: LongArray = LongArray(0),
) {
    fun editionOf(userId: String?): CustomTvBadgeEdition? {
        val id = userId?.toLongOrNull() ?: return null
        return when {
            premium.binarySearch(id) >= 0 -> CustomTvBadgeEdition.Premium
            free.binarySearch(id) >= 0 -> CustomTvBadgeEdition.Free
            else -> null
        }
    }

    /**
     * The list with [userId] showing [edition], or no badge for a null one: the user's own change, shown before the
     * list loads again.
     */
    fun with(userId: String, edition: CustomTvBadgeEdition?): CustomTvBadgeList {
        val id = userId.toLongOrNull() ?: return this
        val premiumIds = premium.filterNot { it == id }
        val freeIds = free.filterNot { it == id }
        return CustomTvBadgeList(
            premium = (if (edition == CustomTvBadgeEdition.Premium) premiumIds + id else premiumIds).sortedIds(),
            free = (if (edition == CustomTvBadgeEdition.Free) freeIds + id else freeIds).sortedIds(),
        )
    }
}

/** The server's `{"premium":[...],"free":[...]}`; null for an answer that is not that. */
internal fun parseCustomTvBadges(body: String): CustomTvBadgeList? {
    val json = try {
        JSONObject(body)
    } catch (_: JSONException) {
        return null
    }
    val premium = json.optJSONArray("premium") ?: return null
    val free = json.optJSONArray("free") ?: return null
    return CustomTvBadgeList(premium.ids(), free.ids())
}

/** The edition the server gave a registered badge; null for an answer without one. */
internal fun parseCustomTvBadgeEdition(body: String): CustomTvBadgeEdition? {
    val json = try {
        JSONObject(body)
    } catch (_: JSONException) {
        return null
    }
    return when (json.optString("edition")) {
        "premium" -> CustomTvBadgeEdition.Premium
        "free" -> CustomTvBadgeEdition.Free
        else -> null
    }
}

internal fun customTvBadgesDue(loadedAtMillis: Long?, nowMillis: Long): Boolean =
    loadedAtMillis == null || nowMillis - loadedAtMillis >= CUSTOM_TV_BADGES_RELOAD.inWholeMilliseconds

private fun JSONArray.ids(): LongArray = (0 until length()).mapNotNull { (opt(it) as? Number)?.toLong() }.sortedIds()

private fun List<Long>.sortedIds(): LongArray = toLongArray().apply { sort() }
