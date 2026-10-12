package name.alexwayfer.customtv.data


fun profileAfterStreamStatus(profile: ChannelProfile, event: StreamStatusEvent): ChannelProfile? {
    if (!profile.login.equals(event.login, ignoreCase = true)) return null
    return when (event) {
        is StreamStatusEvent.Title -> profile.copy(
            streamTitle = event.title,
            categoryName = event.categoryName,
        )
        is StreamStatusEvent.Online -> profile.copy(
            isLive = true,
            streamStartedAtMillis = event.startedAtMillis ?: profile.streamStartedAtMillis,
        )
        is StreamStatusEvent.Offline -> profile.copy(
            isLive = false,
            viewerCount = null,
            sharedViewerCount = null,
        )
    }
}

fun parseStreamStatusNotification(raw: String): StreamStatusEvent? {
    val root = raw.toJsonObjectOrNull() ?: return null
    val metadata = root.optJSONObject("metadata") ?: return null
    if (metadata.optString("message_type") != "notification") return null
    val payload = root.optJSONObject("payload") ?: return null
    val type = payload.optJSONObject("subscription")?.optString("type").orEmpty()
    val event = payload.optJSONObject("event") ?: return null
    val login = event.optString("broadcaster_user_login").trim().lowercase()
    if (login.isEmpty()) return null
    return when (type) {
        "channel.update" -> StreamStatusEvent.Title(
            login = login,
            title = event.optString("title"),
            categoryName = event.optString("category_name").trim().ifBlank { null },
        )
        "stream.online" -> StreamStatusEvent.Online(
            login = login,
            startedAtMillis = event.optString("started_at").trim().ifBlank { null }
                ?.let(::instantMillisOrNull),
        )
        "stream.offline" -> StreamStatusEvent.Offline(login)
        else -> null
    }
}

fun eventSubSessionId(raw: String): String? {
    val root = raw.toJsonObjectOrNull() ?: return null
    if (root.optJSONObject("metadata")?.optString("message_type") != "session_welcome") return null
    return root.optJSONObject("payload")
        ?.optJSONObject("session")
        ?.optString("id")
        ?.takeIf { it.isNotBlank() }
}
