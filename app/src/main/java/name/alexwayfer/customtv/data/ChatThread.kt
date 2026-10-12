package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatThreadEntry
import org.json.JSONArray
import org.json.JSONObject

internal fun parseChatThread(body: String): List<ChatThreadEntry>? {
    val data = body.toJsonObjectOrNull()?.optJSONObject("data") ?: return null
    if (data.isNull("message")) return null
    val message = data.optJSONObject("message") ?: return null
    val entries = mutableListOf<ChatThreadEntry>()
    collectThreadMessage(message, entries, depth = 0)
    return entries.distinctBy { it.id }
}

/** Replies nested deeper than this are not read. */
private const val THREAD_DEPTH = 6

private fun collectThreadMessage(node: JSONObject, into: MutableList<ChatThreadEntry>, depth: Int) {
    if (node.isNull("deletedAt") || node.optString("deletedAt").isBlank()) {
        threadEntry(node)?.let(into::add)
    }
    if (depth >= THREAD_DEPTH) return
    val nodes = node.optJSONObject("replies")?.optJSONArray("nodes") ?: return
    for (index in 0 until nodes.length()) {
        val child = nodes.optJSONObject(index) ?: continue
        collectThreadMessage(child, into, depth + 1)
    }
}

private fun threadEntry(node: JSONObject): ChatThreadEntry? {
    val id = node.text("id") ?: return null
    val sender = node.optJSONObject("sender") ?: return null
    val login = sender.text("login") ?: return null
    val displayName = sender.text("displayName") ?: login
    val text = messageText(node.optJSONObject("content"))
    val body = if (node.optJSONObject("parentMessage")?.optString("id").isNullOrBlank()) {
        text
    } else {
        stripLeadingMention(text)
    }
    val sentAt = node.optString("sentAt")
    val timestamp = instantMillisOrNull(sentAt) ?: 0L
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
    return content.text("text") ?: content.optJSONArray("fragments")?.let(::fragmentText).orEmpty()
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
