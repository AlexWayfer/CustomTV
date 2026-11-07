package name.alexwayfer.customtv.data

import org.json.JSONObject

/** One channel moderator; a missing presence is unknown, not an absent chatter. */
internal data class ChatModerator(
    val id: String,
    val login: String,
    val displayName: String,
    val inChat: Boolean?,
    val appearance: ChatModeratorAppearance? = null,
)

internal data class ChatModeratorsPage(val moderators: List<ChatModerator>, val nextCursor: String?)

/** The public IVR list has no presence; its VIP list is unrelated to this menu. */
internal fun parsePublicChatModerators(body: String): List<ChatModerator>? {
    val mods = body.toJsonObjectOrNull()?.optJSONArray("mods") ?: return null
    return (0 until mods.length()).map { index ->
        parseChatModerator(mods.optJSONObject(index), inChat = null) ?: return null
    }
}

private fun parseChatModerator(node: JSONObject?, inChat: Boolean?): ChatModerator? {
    val id = node?.trimmedText("id") ?: return null
    val login = node.trimmedText("login") ?: return null
    return ChatModerator(
        id, login, node.trimmedText("displayName") ?: login, inChat,
        appearance = node.takeIf { it.has("displayBadges") }?.let(::parseModeratorAppearance),
    )
}

/** A complete GQL page, or null when the list or its pagination cannot be trusted. */
internal fun parseChatModeratorsPage(body: String): ChatModeratorsPage? {
    val connection = body.toJsonObjectOrNull()?.optJSONObject("data")?.optJSONObject("user")
        ?.optJSONObject("mods") ?: return null
    val edges = connection.optJSONArray("edges") ?: return null
    val pageInfo = connection.optJSONObject("pageInfo") ?: return null
    val hasNext = pageInfo.opt("hasNextPage") as? Boolean ?: return null
    val moderators = (0 until edges.length()).map { index ->
        val edge = edges.optJSONObject(index) ?: return null
        parseChatModerator(edge.optJSONObject("node"), edge.opt("isActive") as? Boolean) ?: return null
    }
    val cursor = if (hasNext) {
        edges.optJSONObject(edges.length() - 1)?.trimmedText("cursor") ?: return null
    } else {
        null
    }
    return ChatModeratorsPage(moderators, cursor)
}

internal fun sortedChatModerators(moderators: List<ChatModerator>): List<ChatModerator> =
    moderators.distinctBy { it.id }.sortedWith(
        compareBy<ChatModerator> {
            when (it.inChat) {
                true -> 0
                false -> 1
                null -> 2
            }
        }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.login },
    )
