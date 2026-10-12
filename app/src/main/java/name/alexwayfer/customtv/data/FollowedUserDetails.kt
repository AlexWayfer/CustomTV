package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

internal data class FollowedCollaboration(
    val sharedViewerCount: Int? = null,
    val collaboratorCount: Int? = null,
    val collaboratorAvatarUrls: List<String> = emptyList(),
)

internal data class FollowedUserDetails(
    val lastBroadcastAtMillis: Map<String, Long> = emptyMap(),
    val collaborationByLogin: Map<String, FollowedCollaboration> = emptyMap(),
    val collaborationFetchedLogins: Set<String> = emptySet(),
)

internal fun lastBroadcastsQuery(logins: List<String>): String {
    return JSONObject()
        .put(
            "query",
            $$"query($logins:[String!]!){users(logins:$logins){login lastBroadcast{startedAt} " +
                "stream{viewersCount collaborationViewersCount} " +
                "channel{collaboration{collaborators{status user{login profileImageURL(width:150)}}}}}}",
        )
        .put("variables", JSONObject().put("logins", JSONArray(logins)))
        .toString()
}

internal fun parseFollowedCollaborations(body: String): Map<String, FollowedCollaboration>? {
    val users = followedUsers(body) ?: return null
    return users.objects().mapNotNull(::parseFollowedCollaboration).toMap()
}

/** A channel's shared stream by its lowercase login; a channel streaming alone is none. */
private fun parseFollowedCollaboration(user: JSONObject): Pair<String, FollowedCollaboration>? {
    val login = user.text("login") ?: return null
    val stream = user.optJSONObject("stream")
    val own = stream?.let { value ->
        if (!value.has("viewersCount") || value.isNull("viewersCount")) null else value.optInt("viewersCount")
    }
    val shared = stream?.let { value ->
        if (!value.has("collaborationViewersCount") || value.isNull("collaborationViewersCount")) {
            null
        } else {
            value.optInt("collaborationViewersCount")
        }
    }
    val collaborators = user.optJSONObject("channel")
        ?.optJSONObject("collaboration")
        ?.optJSONArray("collaborators")
    val collaboration = FollowedCollaboration(
        sharedViewerCount = sharedStreamViewerCount(own, shared),
        collaboratorCount = collaborators?.let {
            collaborationOthersCount(activeCollaboratorLogins(it), login)
        },
        collaboratorAvatarUrls = collaborators?.let { activeCollaboratorAvatarUrls(it, login) }.orEmpty(),
    )
    if (collaboration.sharedViewerCount == null && collaboration.collaboratorCount == null) return null
    return login.lowercase() to collaboration
}

private fun followedUsers(body: String): JSONArray? {
    return body.toJsonObjectOrNull()
        ?.optJSONObject("data")
        ?.optJSONArray("users")
}

internal fun parseLastBroadcasts(body: String): Map<String, Long>? {
    val users = followedUsers(body) ?: return null
    return users.objects().mapNotNull(::parseLastBroadcast).toMap()
}

/** When a channel's last broadcast started, by its lowercase login. */
private fun parseLastBroadcast(user: JSONObject): Pair<String, Long>? {
    val login = user.text("login") ?: return null
    val startedAt = user.optJSONObject("lastBroadcast")?.optString("startedAt") ?: return null
    val millis = instantMillisOrNull(startedAt) ?: return null
    return login.lowercase() to millis
}
