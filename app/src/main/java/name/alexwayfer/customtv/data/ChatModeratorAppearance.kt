package name.alexwayfer.customtv.data

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.ui.theme.parseTwitchHexColor
import org.json.JSONObject

internal data class ChatModeratorAppearance(val color: Color, val badges: DisplayedChatBadges)

internal fun parseModeratorAppearances(body: String): Map<String, ChatModeratorAppearance>? {
    val users = body.toJsonObjectOrNull()?.optJSONObject("data")?.optJSONArray("users") ?: return null
    return users.objects().mapNotNull { user ->
        val id = user.trimmedText("id") ?: return@mapNotNull null
        id to parseModeratorAppearance(user)
    }.toMap()
}

internal fun parseModeratorAppearance(user: JSONObject): ChatModeratorAppearance = ChatModeratorAppearance(
    color = parseTwitchHexColor(user.trimmedText("chatColor")) ?: Color.Unspecified,
    badges = parseUserDisplayedChatBadges(user) ?: DisplayedChatBadges(emptyList(), emptyMap()),
)
