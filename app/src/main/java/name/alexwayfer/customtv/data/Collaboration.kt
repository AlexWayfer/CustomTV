package name.alexwayfer.customtv.data

import org.json.JSONObject

internal data class CollaborationChannel(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val viewerCount: Int?,
)

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
    val channel = runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("channel")
        ?: return null
    if (channel.isNull("collaboration")) return emptyList()
    val collaborators = channel.optJSONObject("collaboration")
        ?.optJSONArray("collaborators")
        ?: return emptyList()
    val parsed = buildList {
        for (index in 0 until collaborators.length()) {
            val item = collaborators.optJSONObject(index) ?: continue
            val status = item.optString("status")
            if (status.isNotBlank() && !status.equals("ACTIVE", ignoreCase = true)) continue
            val user = item.optJSONObject("user") ?: continue
            val id = user.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val login = user.optString("login").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val displayName = user.optString("displayName").takeIf { it.isNotBlank() && it != "null" } ?: login
            val avatar = user.optString("profileImageURL").takeIf { it.isNotBlank() && it != "null" }
            val viewers = user.optJSONObject("stream")?.let { stream ->
                if (stream.isNull("viewersCount")) null else stream.optInt("viewersCount")
            }
            add(
                ParsedCollaborator(
                    channel = CollaborationChannel(id, login, displayName, avatar, viewers),
                    leader = item.optString("role").equals("LEADER", ignoreCase = true),
                ),
            )
        }
    }
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
