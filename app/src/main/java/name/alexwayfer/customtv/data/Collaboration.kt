package name.alexwayfer.customtv.data

import org.json.JSONObject

private data class ParsedCollaborator(
    val channel: CollaborationChannel,
    val leader: Boolean,
)

internal fun collaborationOthersCount(collaboratorLogins: List<String>, channelLogin: String): Int? {
    val others = collaboratorLogins.count { login ->
        login.isNotBlank() && !login.equals(channelLogin, ignoreCase = true)
    }
    return others.takeIf { it > 0 }
}

internal fun collaborationRowOpensChannel(rowLogin: String, currentLogin: String): Boolean =
    !rowLogin.equals(currentLogin, ignoreCase = true)

internal fun parseCollaborationChannels(
    body: String,
    currentLogin: String? = null,
): List<CollaborationChannel>? {
    val channel = body.toJsonObjectOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("channel")
        ?: return null
    if (channel.isNull("collaboration")) return emptyList()
    val collaborators = channel.optJSONObject("collaboration")
        ?.optJSONArray("collaborators")
        ?: return emptyList()
    val parsed = collaborators.objects().filter(::isActiveCollaborator).mapNotNull(::parseCollaborator)
    return parsed.sortedWith(
        compareByDescending<ParsedCollaborator> {
            currentLogin != null && it.channel.login.equals(currentLogin, ignoreCase = true)
        }
            .thenByDescending { if (it.leader) 1 else 0 }
            .thenByDescending { it.channel.viewerCount ?: -1 }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.channel.displayName }
            .thenBy { it.channel.login },
    ).map { it.channel }
}

/** A collaborator with the channel shown for them; one without an ID or login is dropped. */
private fun parseCollaborator(item: JSONObject): ParsedCollaborator? {
    val user = item.optJSONObject("user") ?: return null
    val id = user.text("id") ?: return null
    val login = user.text("login") ?: return null
    val displayName = user.text("displayName") ?: login
    val avatar = user.text("profileImageURL")
    val viewers = user.optJSONObject("stream")?.intOrNull("viewersCount")
    return ParsedCollaborator(
        channel = CollaborationChannel(id, login, displayName, avatar, viewers),
        leader = item.optString("role").equals("LEADER", ignoreCase = true),
    )
}
