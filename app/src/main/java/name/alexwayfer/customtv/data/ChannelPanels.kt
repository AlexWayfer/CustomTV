package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.auth.httpUrl
import org.json.JSONObject

/** One About panel a channel set up: any of a title, an image that may link somewhere, and Markdown text. */
internal data class ChannelPanel(
    val id: String,
    val title: String?,
    val imageUrl: String?,
    val linkUrl: String?,
    val description: String?,
)

/** The channel's About panels, and how many are extensions, which only Twitch can show. */
internal data class ChannelPanels(
    val panels: List<ChannelPanel>,
    val extensionCount: Int,
)

internal const val CHANNEL_PANELS_QUERY =
    $$"query($id:ID!){user(id:$id){panels{id type ... on DefaultPanel{title imageURL linkURL description}}}}"

/**
 * The panels in [body]. Null when Twitch left them out, which some networks get instead of the list. A panel with
 * nothing to show is skipped.
 */
internal fun parseChannelPanels(body: String): ChannelPanels? {
    val user = runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("user")
        ?: return null
    if (!user.has("panels") || user.isNull("panels")) return null
    val list = user.optJSONArray("panels") ?: return null
    var extensions = 0
    val panels = (0 until list.length()).mapNotNull { index ->
        val item = list.optJSONObject(index) ?: return@mapNotNull null
        if (item.optString("type") == "EXTENSION") {
            extensions++
            return@mapNotNull null
        }
        val panel = ChannelPanel(
            id = item.optString("id"),
            title = item.text("title"),
            imageUrl = httpUrl(item.optString("imageURL")),
            linkUrl = httpUrl(item.optString("linkURL")),
            description = item.text("description")?.trim()?.takeIf { it.isNotEmpty() },
        )
        panel.takeIf { it.title != null || it.imageUrl != null || it.description != null }
    }
    return ChannelPanels(panels, extensions)
}

private fun JSONObject.text(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
}
