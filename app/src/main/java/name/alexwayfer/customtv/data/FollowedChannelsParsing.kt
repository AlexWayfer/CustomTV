package name.alexwayfer.customtv.data

import org.json.JSONObject

internal fun parseFollowedPage(body: String): FollowedPage? {
    val root = body.toJsonObjectOrNull() ?: return null
    val data = root.optJSONArray("data") ?: return null
    val channels = data.objects().mapNotNull(::parseFollowedChannel)
    return FollowedPage(channels, root.optJSONObject("pagination")?.text("cursor"))
}

private fun parseFollowedChannel(item: JSONObject): FollowedChannel? {
    val id = item.text("broadcaster_id") ?: return null
    val login = item.text("broadcaster_login") ?: return null
    val displayName = item.text("broadcaster_name") ?: login
    return FollowedChannel(id = id, login = login, displayName = displayName)
}

internal fun parseFollowedLive(body: String): Map<String, FollowedLive>? {
    val data = body.toJsonObjectOrNull()?.optJSONArray("data") ?: return null
    return data.objects().mapNotNull(::parseFollowedLiveStream).toMap()
}

/** What a live stream shows, by its channel's ID; a stream that is not live is none. */
private fun parseFollowedLiveStream(item: JSONObject): Pair<String, FollowedLive>? {
    if (!item.optString("type").equals("live", ignoreCase = true)) return null
    val id = item.text("user_id") ?: return null
    val category = item.text("game_name")
    val title = item.trimmedText("title").orEmpty()
    return id to FollowedLive(
        categoryName = category,
        viewerCount = item.optInt("viewer_count"),
        title = title,
        categoryId = item.trimmedText("game_id"),
    )
}

internal fun parseLiveFollowedStreamsPage(body: String): FollowedPage? {
    val root = body.toJsonObjectOrNull() ?: return null
    val data = root.optJSONArray("data") ?: return null
    val channels = data.objects().mapNotNull(::parseLiveFollowedChannel)
    return FollowedPage(channels, root.optJSONObject("pagination")?.text("cursor"))
}

/** A followed channel from its live stream; a stream that is not live is none. */
private fun parseLiveFollowedChannel(item: JSONObject): FollowedChannel? {
    if (!item.optString("type").equals("live", ignoreCase = true)) return null
    val id = item.text("user_id") ?: return null
    val login = item.text("user_login") ?: return null
    val displayName = item.text("user_name") ?: login
    val title = item.trimmedText("title").orEmpty()
    return FollowedChannel(
        id = id,
        login = login,
        displayName = displayName,
        isLive = true,
        categoryName = item.text("game_name"),
        viewerCount = item.optInt("viewer_count"),
        streamTitle = title,
        categoryId = item.trimmedText("game_id"),
    )
}

internal fun parseFollowedAvatars(body: String): Map<String, String>? {
    val data = body.toJsonObjectOrNull()?.optJSONArray("data") ?: return null
    return data.objects().mapNotNull { item ->
        val id = item.text("id")
        val avatar = item.text("profile_image_url")
        if (id != null && avatar != null) id to avatar else null
    }.toMap()
}
