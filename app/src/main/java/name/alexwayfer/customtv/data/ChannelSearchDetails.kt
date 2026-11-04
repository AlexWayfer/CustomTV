package name.alexwayfer.customtv.data

import org.json.JSONObject

/** Current stream details, or when the last broadcast ended, for a search suggestion. */
data class ChannelSearchDetails(
    val isLive: Boolean,
    val viewerCount: Int?,
    val categoryName: String?,
    val lastBroadcastAtMillis: Long?,
)

internal const val SEARCH_USER_FIELDS = "lastBroadcast{startedAt} stream{viewersCount game{displayName}}"

internal fun channelSearchUserDetails(user: JSONObject): ChannelSearchDetails {
    val stream = user.optJSONObject("stream")
    val endedAt = user.optJSONObject("lastBroadcast")?.text("startedAt")
    return ChannelSearchDetails(
        isLive = stream != null,
        viewerCount = stream?.intOrNull("viewersCount")?.takeIf { it >= 0 },
        categoryName = stream?.optJSONObject("game")?.trimmedText("displayName"),
        lastBroadcastAtMillis = endedAt?.takeIf { stream == null }?.let(::instantMillisOrNull),
    )
}
