package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

internal fun collaborationCount(user: JSONObject, channelLogin: String): Int? {
    val collaborators = user.optJSONObject("channel")
        ?.optJSONObject("collaboration")
        ?.optJSONArray("collaborators")
        ?: return null
    return collaborationOthersCount(activeCollaboratorLogins(collaborators), channelLogin)
}

internal fun collaboratorAvatarUrls(user: JSONObject, channelLogin: String): List<String> {
    val collaborators = user.optJSONObject("channel")
        ?.optJSONObject("collaboration")
        ?.optJSONArray("collaborators")
        ?: return emptyList()
    return activeCollaboratorAvatarUrls(collaborators, channelLogin)
}

internal fun sharedStreamViewerCount(own: Int?, shared: Int?): Int? {
    return shared?.takeIf { own != null && it > own }
}

internal fun activeCollaboratorLogins(collaborators: JSONArray): List<String> =
    activeCollaborators(collaborators).mapNotNull { user -> user.text("login") }

/** Avatars of the active collaborators other than [channelLogin], skipping those without one. */
internal fun activeCollaboratorAvatarUrls(collaborators: JSONArray, channelLogin: String): List<String> =
    activeCollaborators(collaborators)
        .filterNot { it.optString("login").equals(channelLogin, ignoreCase = true) }
        .mapNotNull { user -> user.text("profileImageURL") }

/** The users of the collaborators who are in the collaboration now, not only invited. */
private fun activeCollaborators(collaborators: JSONArray): List<JSONObject> =
    collaborators.objects().filter(::isActiveCollaborator).mapNotNull { it.optJSONObject("user") }

/** A collaborator without a status counts as active. */
internal fun isActiveCollaborator(item: JSONObject): Boolean {
    val status = item.optString("status")
    return status.isBlank() || status.equals("ACTIVE", ignoreCase = true)
}
