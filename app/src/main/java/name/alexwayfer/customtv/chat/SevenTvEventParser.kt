package name.alexwayfer.customtv.chat

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.text
import org.json.JSONArray
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

internal object SevenTvEventParser {
    /** A frame of the socket that follows the channel whose own emote set is [channelSetId]. */
    fun parseFrame(text: String, channelSetId: String? = null): SevenTvFrame {
        val json = text.toJsonObjectOrNull() ?: return SevenTvFrame.Ignore
        return when (json.optInt("op", -1)) {
            OP_DISPATCH -> parsePersonal(json, channelSetId)?.let(SevenTvFrame::Personal)
                ?: parseCosmetic(json)?.let(SevenTvFrame::Cosmetic)
                ?: SevenTvFrame.Dispatch(parseDispatch(json))
            OP_HELLO -> {
                val interval = json.optJSONObject("d")?.optLong("heartbeat_interval") ?: 25_000L
                SevenTvFrame.Hello(heartbeatMs = interval.coerceAtLeast(MIN_HEARTBEAT_MILLIS))
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
                    ChatEmoteChange(
                        platform = EmotePlatform.SevenTv,
                        action = EmoteChangeAction.Added,
                        actorName = actorName,
                        emoteName = change.name,
                        emote = change.emote,
                        emoteId = change.emote?.id,
                    ),
                    timestampMillis = timestamp,
                )
            }
        parseChanged(body.optJSONArray("pulled"), valueKey = "old_value")
            .forEach { change ->
                messages += emoteChangeMessage(
                    ChatEmoteChange(
                        platform = EmotePlatform.SevenTv,
                        action = EmoteChangeAction.Removed,
                        actorName = actorName,
                        emoteName = change.name,
                        emote = change.emote,
                        emoteId = change.emote?.id,
                    ),
                    timestampMillis = timestamp,
                )
            }
        val updated = body.optJSONArray("updated") ?: return messages
        for (item in updated.objects()) {
            val oldChange = parseValue(item.optJSONObject("old_value"))
            val newChange = parseValue(item.optJSONObject("value")) ?: continue
            if (oldChange != null && oldChange.name != newChange.name) {
                messages += emoteChangeMessage(
                    ChatEmoteChange(
                        platform = EmotePlatform.SevenTv,
                        action = EmoteChangeAction.Renamed,
                        actorName = actorName,
                        emoteName = newChange.name,
                        previousName = oldChange.name,
                        emote = newChange.emote,
                        emoteId = newChange.emote?.id,
                    ),
                    timestampMillis = timestamp,
                )
            }
        }
        return messages
    }

    /**
     * A change of a chatter's personal emotes: an update of a set other than the channel's, or a set given to a
     * chatter. Null for anything else, the channel's own set among it.
     */
    private fun parsePersonal(json: JSONObject, channelSetId: String?): PersonalEmoteEvent? {
        val data = json.optJSONObject("d") ?: return null
        val body = data.optJSONObject("body") ?: return null
        return when (data.optString("type")) {
            "emote_set.update" -> {
                val setId = body.text("id")?.takeIf { it != channelSetId } ?: return null
                parsePersonalSetChange(setId, body)
            }
            "entitlement.create" -> parseEmoteSetEntitlement(body.optJSONObject("object"))
            else -> null
        }
    }

    private fun parseCosmetic(json: JSONObject): SevenTvCosmeticEvent? {
        val data = json.optJSONObject("d") ?: return null
        val body = data.optJSONObject("body") ?: return null
        return SevenTvCosmeticParser.parse(data.optString("type"), body)
    }

    private fun parsePersonalSetChange(setId: String, body: JSONObject): PersonalEmoteEvent.SevenTvSetChanged {
        val updated = body.optJSONArray("updated")?.objects().orEmpty()
        val added = parseChanged(body.optJSONArray("pushed"), valueKey = "value") +
            updated.mapNotNull { parseValue(it.optJSONObject("value")) }
        val removed = parseChanged(body.optJSONArray("pulled"), valueKey = "old_value") +
            updated.mapNotNull { parseValue(it.optJSONObject("old_value")) }
        return PersonalEmoteEvent.SevenTvSetChanged(
            setId = setId,
            added = added.mapNotNull { change -> change.emote?.let { change.name to it } }.toMap(),
            removed = removed.mapTo(HashSet()) { it.name },
        )
    }

    private fun parseEmoteSetEntitlement(entitlement: JSONObject?): PersonalEmoteEvent.SevenTvSetAssigned? {
        if (entitlement?.optString("kind") != "EMOTE_SET") return null
        val setId = entitlement.text("ref_id") ?: return null
        val userIds = entitlement.optJSONObject("user")?.optJSONArray("connections")?.objects().orEmpty()
            .filter { it.optString("platform") == "TWITCH" }
            .mapNotNull { it.text("id") }
        if (userIds.isEmpty()) return null
        return PersonalEmoteEvent.SevenTvSetAssigned(setId, userIds)
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
        val name = value.text("name") ?: return null
        return ParsedEmote(name, null)
    }

    private fun actorDisplayName(actor: JSONObject?): String =
        actor?.text("display_name") ?: actor?.text("username") ?: "Unknown"

    private fun eventTimestamp(json: JSONObject): Long {
        val raw = json.optLong("t")
        // A timestamp of today has 19 digits in nanoseconds, 13 in milliseconds, and 10 in seconds.
        return when {
            raw > NANOS_FROM -> raw.nanoseconds.inWholeMilliseconds
            raw > MILLIS_FROM -> raw
            raw > SECONDS_FROM -> raw.seconds.inWholeMilliseconds
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

    private const val MIN_HEARTBEAT_MILLIS = 5_000L

    private const val NANOS_FROM = 10_000_000_000_000_000L
    private const val MILLIS_FROM = 1_000_000_000_000L
    private const val SECONDS_FROM = 1_000_000_000L
}

internal sealed interface SevenTvFrame {
    data class Hello(val heartbeatMs: Long) : SevenTvFrame
    data class Dispatch(val messages: List<ChatMessage>) : SevenTvFrame
    data class Personal(val event: PersonalEmoteEvent) : SevenTvFrame
    data class Cosmetic(val event: SevenTvCosmeticEvent) : SevenTvFrame
    data object Reconnect : SevenTvFrame
    data object Ignore : SevenTvFrame
}
