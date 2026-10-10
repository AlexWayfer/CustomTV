package name.alexwayfer.customtv.auth

import name.alexwayfer.customtv.data.ProfileDetails
import org.json.JSONArray

internal fun TwitchAccount.withProfileDetails(details: ProfileDetails): TwitchAccount {
    return copy(
        bannerUrl = details.bannerUrl,
        description = details.description ?: description,
        followerCount = details.followerCount,
        links = details.links ?: links,
    )
}

/** A failed details load keeps the last banner, count, and links for the same login. */
internal fun profileAfterRefresh(
    previous: TwitchAccount,
    fresh: TwitchAccount,
    details: ProfileDetails?,
): TwitchAccount {
    if (details != null) {
        val base = if (
            details.links == null && previous.login.equals(fresh.login, ignoreCase = true)
        ) {
            fresh.copy(links = previous.links)
        } else {
            fresh
        }
        return base.withProfileDetails(details)
    }
    if (!previous.login.equals(fresh.login, ignoreCase = true)) return fresh
    return fresh.copy(
        bannerUrl = previous.bannerUrl,
        followerCount = previous.followerCount,
        links = previous.links,
        description = fresh.description ?: previous.description,
    )
}

internal fun parseProfileLinks(array: JSONArray): List<TwitchProfileLink> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val url = httpUrl(item.optString("url")) ?: continue
            val name = item.optString("name").trim().takeIf { it.isNotBlank() && it != "null" }
            val title = item.optString("title").trim().takeIf { it.isNotBlank() && it != "null" }
                ?: name
                ?: continue
            if (any { it.url == url }) continue
            add(TwitchProfileLink(title = title, url = url, name = name))
        }
    }
}

internal fun httpUrl(value: String): String? {
    val url = value.trim()
    if (url.equals("null", ignoreCase = true)) return null
    if (!url.startsWith("https://") && !url.startsWith("http://")) return null
    return url
}
