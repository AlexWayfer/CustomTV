package name.alexwayfer.customtv.auth

import name.alexwayfer.customtv.data.intOrNull
import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.textOrNull
import name.alexwayfer.customtv.data.trimmedText
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class TwitchAccount(
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerUrl: String? = null,
    val description: String? = null,
    val followerCount: Int? = null,
    val links: List<TwitchProfileLink> = emptyList(),
    val userId: String? = null,
)

internal data class TwitchProfileLink(
    val title: String,
    val url: String,
    val name: String? = null,
)

internal data class TwitchTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val grantedScopes: Set<String>? = null,
)

internal data class TwitchSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val account: TwitchAccount,
    val grantedScopes: Set<String>? = null,
)

internal fun parseTwitchTokens(json: String): TwitchTokens? {
    val root = try {
        JSONObject(json)
    } catch (_: JSONException) {
        return null
    }
    val access = root.optString("access_token").takeIf { it.isNotBlank() } ?: return null
    val refresh = root.optString("refresh_token").takeIf { it.isNotBlank() } ?: return null
    val expiresIn = root.optLong("expires_in", -1L).takeIf { it > 0 } ?: return null
    return TwitchTokens(access, refresh, expiresIn, grantedScopesFrom(root, "scope"))
}

internal fun parseValidatedScopes(json: String): Set<String>? {
    val root = try {
        JSONObject(json)
    } catch (_: Exception) {
        return null
    }
    return grantedScopesFrom(root, "scopes")
}

internal fun grantedScopesFrom(root: JSONObject, key: String): Set<String>? {
    if (!root.has(key) || root.isNull(key)) return null
    val array = root.optJSONArray(key) ?: return null
    return buildSet {
        for (index in 0 until array.length()) {
            array.optString(index).textOrNull()?.let { add(it) }
        }
    }
}

internal fun parseTwitchAccount(json: String): TwitchAccount? {
    val root = try {
        JSONObject(json)
    } catch (_: JSONException) {
        return null
    }
    val data = root.optJSONArray("data") ?: return null
    if (data.length() == 0) return null
    val user = data.optJSONObject(0) ?: return null
    val login = user.optString("login").takeIf { it.isNotBlank() } ?: return null
    val displayName = user.optString("display_name").takeIf { it.isNotBlank() } ?: login
    val avatar = user.text("profile_image_url")
    val description = user.trimmedText("description")
    val userId = user.text("id")
    return TwitchAccount(
        login = login,
        displayName = displayName,
        avatarUrl = avatar,
        description = description,
        userId = userId,
    )
}

internal fun sessionPayload(session: TwitchSession): String {
    return JSONObject()
        .put("accessToken", session.accessToken)
        .put("refreshToken", session.refreshToken)
        .put("expiresAtMillis", session.expiresAtMillis)
        .put("login", session.account.login)
        .put("displayName", session.account.displayName)
        .put("avatarUrl", session.account.avatarUrl ?: JSONObject.NULL)
        .put("bannerUrl", session.account.bannerUrl ?: JSONObject.NULL)
        .put("description", session.account.description ?: JSONObject.NULL)
        .put("followerCount", session.account.followerCount ?: JSONObject.NULL)
        .put("userId", session.account.userId ?: JSONObject.NULL)
        .put(
            "grantedScopes",
            session.grantedScopes?.let { scopes ->
                JSONArray().apply { scopes.forEach { put(it) } }
            } ?: JSONObject.NULL,
        )
        .put("links", JSONArray().apply {
            session.account.links.forEach { link ->
                put(
                    JSONObject()
                        .put("title", link.title)
                        .put("url", link.url)
                        .put("name", link.name ?: JSONObject.NULL),
                )
            }
        })
        .toString()
}

internal fun parseSessionPayload(json: String): TwitchSession? {
    val root = try {
        JSONObject(json)
    } catch (_: Exception) {
        return null
    }
    val access = root.optString("accessToken").takeIf { it.isNotBlank() } ?: return null
    val refresh = root.optString("refreshToken").takeIf { it.isNotBlank() } ?: return null
    if (!root.has("expiresAtMillis")) return null
    val account = storedAccount(root) ?: return null
    return TwitchSession(
        accessToken = access,
        refreshToken = refresh,
        expiresAtMillis = root.getLong("expiresAtMillis"),
        grantedScopes = if (!root.has("grantedScopes") || root.isNull("grantedScopes")) {
            null
        } else {
            grantedScopesFrom(root, "grantedScopes")
        },
        account = account,
    )
}

/** The account kept with the session; null without its login. */
private fun storedAccount(root: JSONObject): TwitchAccount? {
    val login = root.optString("login").takeIf { it.isNotBlank() } ?: return null
    return TwitchAccount(
        login = login,
        displayName = root.optString("displayName").takeIf { it.isNotBlank() } ?: login,
        avatarUrl = root.text("avatarUrl"),
        bannerUrl = root.text("bannerUrl"),
        description = root.trimmedText("description"),
        followerCount = root.intOrNull("followerCount")?.takeIf { it >= 0 },
        links = parseStoredLinks(root.optJSONArray("links")),
        userId = root.text("userId"),
    )
}

private fun parseStoredLinks(array: JSONArray?): List<TwitchProfileLink> {
    return array?.objects()?.mapNotNull(::parseStoredLink).orEmpty()
}

private fun parseStoredLink(item: JSONObject): TwitchProfileLink? {
    val title = item.optString("title").trim().takeIf { it.isNotBlank() } ?: return null
    val url = item.optString("url").trim().takeIf { it.isNotBlank() } ?: return null
    val name = item.trimmedText("name")
    return TwitchProfileLink(title = title, url = url, name = name)
}
