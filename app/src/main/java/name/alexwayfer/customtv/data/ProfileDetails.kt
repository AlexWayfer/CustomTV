package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.auth.TwitchProfileLink
import name.alexwayfer.customtv.auth.httpUrl
import name.alexwayfer.customtv.auth.parseProfileLinks
import name.alexwayfer.customtv.chat.ChatterProfile
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * What a chatter card and a profile show about a Twitch user, from one GQL answer. [links] is null when Twitch
 * left them out, and empty when the user has none.
 */
internal data class ProfileDetails(
    val userId: String?,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val description: String?,
    val followerCount: Int?,
    val createdAtMillis: Long?,
    val links: List<TwitchProfileLink>?,
    /** When the last stream started; null while the channel is live or when it never streamed. */
    val lastLiveMillis: Long? = null,
)

/** How many profiles stay on the device; the one opened longest ago leaves first. */
internal const val PROFILES_KEPT = 200

internal const val PROFILE_DETAILS_QUERY =
    $$"query($login:String!){user(login:$login){id login displayName description profileImageURL(width:300) " +
        "bannerImageURL createdAt followers{totalCount} stream{id} lastBroadcast{startedAt} " +
        "channel{socialMedias{name title url}}}}"

internal fun parseProfileDetails(body: String, fallbackLogin: String): ProfileDetails? {
    val user = runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("user")
        ?: return null
    val login = user.text("login") ?: fallbackLogin
    val followers = user.optJSONObject("followers")
    val socialMedias = user.optJSONObject("channel")
        ?.takeIf { it.has("socialMedias") && !it.isNull("socialMedias") }
        ?.optJSONArray("socialMedias")
    return ProfileDetails(
        userId = user.text("id"),
        login = login,
        displayName = user.text("displayName") ?: login,
        avatarUrl = user.text("profileImageURL"),
        bannerUrl = httpUrl(user.optString("bannerImageURL")),
        description = user.text("description")?.trim()?.takeIf { it.isNotEmpty() },
        followerCount = followers?.takeIf { !it.isNull("totalCount") }?.optInt("totalCount")?.takeIf { it >= 0 },
        createdAtMillis = user.text("createdAt")?.let(::instantMillis),
        links = socialMedias?.let(::parseProfileLinks),
        lastLiveMillis = if (user.optJSONObject("stream") != null) {
            null
        } else {
            user.optJSONObject("lastBroadcast")?.text("startedAt")?.let(::instantMillis)
        },
    )
}

private fun instantMillis(text: String): Long? = runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()

private fun JSONObject.text(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
}

internal fun ProfileDetails.toChatterProfile(): ChatterProfile = ChatterProfile(
    login = login,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bannerImageUrl = bannerUrl,
    createdAtMillis = createdAtMillis,
    userId = userId,
    about = description,
    links = links.orEmpty(),
)

/**
 * The kept profiles, most recently opened first, after [opened]: it moves to the front, replacing its older
 * entry, and the list keeps at most [kept]. A profile without a user id is not kept.
 */
internal fun profilesAfterOpen(
    entries: List<ProfileDetails>,
    opened: ProfileDetails,
    kept: Int = PROFILES_KEPT,
): List<ProfileDetails> {
    val id = opened.userId ?: return entries
    return (listOf(opened) + entries.filter { it.userId != id }).take(kept)
}

/** The kept profile whose current login is [login], in any case. */
internal fun profileByLogin(entries: List<ProfileDetails>, login: String): ProfileDetails? =
    entries.firstOrNull { it.login.equals(login, ignoreCase = true) }

internal fun profilesJson(entries: List<ProfileDetails>): String = JSONArray().apply {
    entries.forEach { details ->
        put(
            JSONObject()
                .put("userId", details.userId)
                .put("login", details.login)
                .put("displayName", details.displayName)
                .put("avatarUrl", details.avatarUrl)
                .put("bannerUrl", details.bannerUrl)
                .put("description", details.description)
                .put("followerCount", details.followerCount)
                .put("createdAtMillis", details.createdAtMillis)
                .put("lastLiveMillis", details.lastLiveMillis)
                .put(
                    "links",
                    details.links?.let { links ->
                        JSONArray().apply {
                            links.forEach { put(JSONObject().put("title", it.title).put("url", it.url).put("name", it.name)) }
                        }
                    },
                ),
        )
    }
}.toString()

internal fun parseStoredProfiles(raw: String): List<ProfileDetails> {
    val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val userId = item.text("userId") ?: return@mapNotNull null
        val login = item.text("login") ?: return@mapNotNull null
        ProfileDetails(
            userId = userId,
            login = login,
            displayName = item.text("displayName") ?: login,
            avatarUrl = item.text("avatarUrl"),
            bannerUrl = item.text("bannerUrl"),
            description = item.text("description"),
            followerCount = item.takeIf { it.has("followerCount") && !it.isNull("followerCount") }?.optInt("followerCount"),
            createdAtMillis = item.takeIf { it.has("createdAtMillis") && !it.isNull("createdAtMillis") }
                ?.optLong("createdAtMillis"),
            lastLiveMillis = item.takeIf { it.has("lastLiveMillis") && !it.isNull("lastLiveMillis") }
                ?.optLong("lastLiveMillis"),
            links = item.optJSONArray("links")?.let { links ->
                (0 until links.length()).mapNotNull { linkIndex ->
                    val link = links.optJSONObject(linkIndex) ?: return@mapNotNull null
                    val url = link.text("url") ?: return@mapNotNull null
                    TwitchProfileLink(title = link.text("title") ?: url, url = url, name = link.text("name"))
                }
            },
        )
    }
}
