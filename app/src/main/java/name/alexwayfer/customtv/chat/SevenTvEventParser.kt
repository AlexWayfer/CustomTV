package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.SevenTvRepository
import org.json.JSONArray
import org.json.JSONObject

internal object SevenTvEventParser {
    fun parseFrame(text: String): SevenTvFrame {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return SevenTvFrame.Ignore
        return when (json.optInt("op", -1)) {
            OP_DISPATCH -> SevenTvFrame.Dispatch(parseDispatch(json))
            OP_HELLO -> {
                val interval = json.optJSONObject("d")?.optLong("heartbeat_interval") ?: 25_000L
                SevenTvFrame.Hello(heartbeatMs = interval.coerceAtLeast(5_000L))
            }
            OP_RECONNECT -> SevenTvFrame.Reconnect
            else -> SevenTvFrame.Ignore
        }
    }

    internal fun parseDispatch(json: JSONObject): List<ChatMessage> {
        val data = json.optJSONObject("d") ?: return emptyList()
        val type = data.optString("type")
        if (type != "emote_set.update") return emptyList()
        val body = data.optJSONObject("body") ?: data
        val actor = body.optJSONObject("actor")
        val actorName = actorDisplayName(actor)
        val timestamp = eventTimestamp(json)
        val messages = mutableListOf<ChatMessage>()
        parseChanged(body.optJSONArray("pushed"), valueKey = "value")
            .forEach { change ->
                messages += emoteChangeMessage(
                    platform = EmotePlatform.SevenTv,
                    action = EmoteChangeAction.Added,
                    actorName = actorName,
                    emoteName = change.name,
                    emote = change.emote,
                    emoteId = change.emote?.id,
                    timestampMillis = timestamp,
                )
            }
        parseChanged(body.optJSONArray("pulled"), valueKey = "old_value")
            .forEach { change ->
                messages += emoteChangeMessage(
                    platform = EmotePlatform.SevenTv,
                    action = EmoteChangeAction.Removed,
                    actorName = actorName,
                    emoteName = change.name,
                    emote = change.emote,
                    emoteId = change.emote?.id,
                    timestampMillis = timestamp,
                )
            }
        val updated = body.optJSONArray("updated") ?: return messages
        for (index in 0 until updated.length()) {
            val item = updated.optJSONObject(index) ?: continue
            val oldChange = parseValue(item.optJSONObject("old_value"))
            val newChange = parseValue(item.optJSONObject("value")) ?: continue
            if (oldChange != null && oldChange.name != newChange.name) {
                messages += emoteChangeMessage(
                    platform = EmotePlatform.SevenTv,
                    action = EmoteChangeAction.Renamed,
                    actorName = actorName,
                    emoteName = newChange.name,
                    previousName = oldChange.name,
                    emote = newChange.emote,
                    emoteId = newChange.emote?.id,
                    timestampMillis = timestamp,
                )
            }
        }
        return messages
    }

    private fun parseChanged(array: JSONArray?, valueKey: String): List<ParsedEmote> {
        if (array == null) return emptyList()
        val result = mutableListOf<ParsedEmote>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            parseValue(item.optJSONObject(valueKey))?.let { result += it }
        }
        return result
    }

    private fun parseValue(value: JSONObject?): ParsedEmote? {
        if (value == null) return null
        val parsed = SevenTvRepository.parseSetItem(value)
        if (parsed != null) {
            return ParsedEmote(parsed.first, parsed.second)
        }
        val name = value.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: return null
        return ParsedEmote(name, null)
    }

    private fun actorDisplayName(actor: JSONObject?): String {
        return actor?.optString("display_name")?.takeIf { it.isNotBlank() && it != "null" }
            ?: actor?.optString("username")?.takeIf { it.isNotBlank() && it != "null" }
            ?: "Unknown"
    }

    private fun eventTimestamp(json: JSONObject): Long {
        val raw = json.optLong("t")
        return when {
            raw > 10_000_000_000_000_000L -> raw / 1_000_000L
            raw > 1_000_000_000_000L -> raw
            raw > 1_000_000_000L -> raw * 1_000L
            else -> System.currentTimeMillis()
        }
    }

    private data class ParsedEmote(
        val name: String,
        val emote: SevenTvEmote?,
    )

    private const val OP_DISPATCH = 0
    private const val OP_HELLO = 1
    private const val OP_RECONNECT = 4
}

internal sealed interface SevenTvFrame {
    data class Hello(val heartbeatMs: Long) : SevenTvFrame
    data class Dispatch(val messages: List<ChatMessage>) : SevenTvFrame
    data object Reconnect : SevenTvFrame
    data object Ignore : SevenTvFrame
}
