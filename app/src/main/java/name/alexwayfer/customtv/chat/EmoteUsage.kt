package name.alexwayfer.customtv.chat

import org.json.JSONObject

internal data class EmoteUse(
    val name: String,
    val count: Int,
    val usedAtMillis: Long,
)

internal fun emoteUsesAfterMessage(
    current: List<EmoteUse>,
    message: String,
    knownNames: Set<String>,
    usedAtMillis: Long,
): List<EmoteUse> {
    if (knownNames.isEmpty()) return current
    val next = current.associateBy { it.name }.toMutableMap()
    var changed = false
    for (token in message.split(' ', '\n', '\r', '\t')) {
        if (token.isEmpty() || token !in knownNames) continue
        val previous = next[token]
        next[token] = EmoteUse(
            name = token,
            count = (previous?.count ?: 0) + 1,
            usedAtMillis = usedAtMillis,
        )
        changed = true
    }
    return if (changed) next.values.toList() else current
}

/**
 * The text of a live chat message the signed-in user wrote, from this device or another one,
 * or null when it must not count toward emote usage.
 */
internal fun ownEmoteUsageText(message: ChatMessage, ownUserId: String?): String? {
    if (ownUserId.isNullOrBlank() || message.userId != ownUserId) return null
    when (message.eventKind) {
        ChatEventKind.MessageDeleted,
        ChatEventKind.UserMessagesDeleted,
        ChatEventKind.System -> return null
        else -> Unit
    }
    return message.rawText.takeIf { it.isNotBlank() }
}

internal fun encodeEmoteUsage(channels: Map<String, List<EmoteUse>>): String {
    val root = JSONObject()
    for ((channelId, uses) in channels) {
        if (channelId.isBlank()) continue
        val channel = JSONObject()
        for ((name, count, usedAtMillis) in uses) {
            if (name.isEmpty() || count <= 0) continue
            channel.put(
                name,
                JSONObject()
                    .put("count", count)
                    .put("usedAt", usedAtMillis),
            )
        }
        if (channel.length() > 0) root.put(channelId, channel)
    }
    return root.toString()
}

internal fun decodeEmoteUsage(raw: String): Map<String, List<EmoteUse>>? {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    val channels = linkedMapOf<String, List<EmoteUse>>()
    val channelIds = root.keys()
    while (channelIds.hasNext()) {
        val channelId = channelIds.next()
        val channel = root.optJSONObject(channelId) ?: continue
        val uses = ArrayList<EmoteUse>()
        val names = channel.keys()
        while (names.hasNext()) {
            val name = names.next()
            val item = channel.optJSONObject(name) ?: continue
            val count = item.optInt("count", 0)
            if (name.isEmpty() || count <= 0) continue
            uses.add(EmoteUse(name, count, item.optLong("usedAt", 0L)))
        }
        if (uses.isNotEmpty()) channels[channelId] = uses
    }
    return channels
}
