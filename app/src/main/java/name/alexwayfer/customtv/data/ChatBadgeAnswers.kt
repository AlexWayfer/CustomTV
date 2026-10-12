package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

internal data class StoredBadge(val url: String, val title: String?)

internal class ChannelBadgeAnswer(val userId: String?, val badges: Map<String, StoredBadge>)

internal fun parseGlobalBadges(body: String): Map<String, StoredBadge>? =
    body.toJsonObjectOrNull()?.optJSONObject("data")?.let { parseBadgeArray(it.optJSONArray("badges")) }

internal fun parseChannelBadges(body: String): ChannelBadgeAnswer? {
    val user = body.toJsonObjectOrNull()?.optJSONObject("data")?.optJSONObject("user") ?: return null
    return ChannelBadgeAnswer(
        userId = user.text("id"),
        badges = parseBadgeArray(user.optJSONArray("broadcastBadges")),
    )
}

private fun parseBadgeArray(array: JSONArray?): Map<String, StoredBadge> {
    if (array == null) return emptyMap()
    return array.objects().mapNotNull(::parseBadge).toMap()
}

/** A badge under its `set/version` key; one without its set or picture is dropped. */
private fun parseBadge(item: JSONObject): Pair<String, StoredBadge>? {
    val setId = item.text("setID") ?: return null
    val url = item.text("imageURL") ?: return null
    val version = item.optString("version")
    val title = item.text("title")
    return "$setId/$version" to StoredBadge(url, title)
}
