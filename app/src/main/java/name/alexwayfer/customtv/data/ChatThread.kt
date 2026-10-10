package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatThreadEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal fun parseChatThread(body: String): List<ChatThreadEntry>? {
    val data = runCatching { JSONObject(body) }.getOrNull()?.optJSONObject("data") ?: return null
    if (data.isNull("message")) return null
    val message = data.optJSONObject("message") ?: return null
    val entries = mutableListOf<ChatThreadEntry>()
    collectThreadMessage(message, entries, depth = 0)
    return entries.distinctBy { it.id }
}

private fun collectThreadMessage(node: JSONObject, into: MutableList<ChatThreadEntry>, depth: Int) {
    if (node.isNull("deletedAt") || node.optString("deletedAt").isBlank()) {
        threadEntry(node)?.let(into::add)
    }
    if (depth >= 6) return
    val nodes = node.optJSONObject("replies")?.optJSONArray("nodes") ?: return
    for (index in 0 until nodes.length()) {
        val child = nodes.optJSONObject(index) ?: continue
        collectThreadMessage(child, into, depth + 1)
    }
}

private fun threadEntry(node: JSONObject): ChatThreadEntry? {
    val id = node.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: return null
    val sender = node.optJSONObject("sender") ?: return null
    val login = sender.optString("login").takeIf { it.isNotBlank() && it != "null" } ?: return null
    val displayName = sender.optString("displayName").takeIf { it.isNotBlank() && it != "null" } ?: login
    val text = messageText(node.optJSONObject("content"))
    val body = if (node.optJSONObject("parentMessage")?.optString("id").isNullOrBlank()) {
        text
    } else {
        stripLeadingMention(text)
    }
    val sentAt = node.optString("sentAt")
    val timestamp = runCatching { Instant.parse(sentAt).toEpochMilli() }.getOrDefault(0L)
    return ChatThreadEntry(
        id = id,
        login = login,
        displayName = displayName,
        body = body,
        timestampMillis = timestamp,
    )
}

private fun messageText(content: JSONObject?): String {
    if (content == null) return ""
    val text = content.optString("text").takeIf { it.isNotBlank() && it != "null" }
    if (text != null) return text
    val fragments = content.optJSONArray("fragments") ?: return ""
    return fragmentText(fragments)
}

private fun fragmentText(fragments: JSONArray): String {
    return buildString {
        for (index in 0 until fragments.length()) {
            val fragment = fragments.optJSONObject(index) ?: continue
            append(fragment.optString("text"))
        }
    }
}

private fun stripLeadingMention(text: String): String {
    if (!text.startsWith("@")) return text
    val space = text.indexOf(' ')
    if (space <= 1) return text
    return text.substring(space + 1).trimStart()
}
