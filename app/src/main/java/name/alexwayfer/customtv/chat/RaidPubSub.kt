package name.alexwayfer.customtv.chat

import org.json.JSONObject

internal data class OutgoingRaid(
    val id: String,
    val targetLogin: String,
    val targetDisplayName: String,
    val targetAvatarUrl: String? = null,
    val viewerCount: Int,
    val goAtMillis: Long,
    val leaving: Boolean,
)

internal sealed interface RaidPubSubEvent {
    data class Update(val raid: OutgoingRaid) : RaidPubSubEvent
    data class Go(val raid: OutgoingRaid) : RaidPubSubEvent
    data class Cancel(val raidId: String) : RaidPubSubEvent
}

internal fun outgoingRaidCreatedId(current: OutgoingRaid?, event: RaidPubSubEvent): String? {
    return when (event) {
        is RaidPubSubEvent.Cancel -> null
        is RaidPubSubEvent.Update -> event.raid.id.takeIf { it != current?.id }
        is RaidPubSubEvent.Go -> event.raid.id.takeIf { current == null }
    }
}

internal fun applyRaidEvent(current: OutgoingRaid?, event: RaidPubSubEvent): OutgoingRaid? {
    return when (event) {
        is RaidPubSubEvent.Cancel -> if (current?.id == event.raidId) null else current
        is RaidPubSubEvent.Update -> mergeRaid(current, event.raid, keepDeadline = true)
        is RaidPubSubEvent.Go -> mergeRaid(current, event.raid, keepDeadline = false)
    }
}

internal fun parseRaidPubSubFrame(raw: String, nowMillis: Long): RaidPubSubEvent? {
    val frame = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (frame.optString("type") != "MESSAGE") return null
    val data = frame.optJSONObject("data") ?: return null
    val topic = data.optString("topic")
    if (!topic.startsWith("raid.")) return null
    val message = data.optString("message").takeIf { it.isNotBlank() && it != "null" } ?: return null
    return parseRaidPubSubMessage(message, nowMillis)
}

internal fun parseRaidPubSubMessage(raw: String, nowMillis: Long): RaidPubSubEvent? {
    val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    val raid = payload.optJSONObject("raid") ?: return null
    return when (payload.optString("type")) {
        "raid_update_v2", "raid_update" -> parseRaidUpdate(raid, nowMillis)
        "raid_go_v2", "raid_go" -> parseRaidGo(raid, nowMillis)
        "raid_cancel_v2", "raid_cancel" -> {
            val id = text(raid, "id") ?: return null
            RaidPubSubEvent.Cancel(id)
        }
        else -> null
    }
}

internal fun raidSecondsRemaining(goAtMillis: Long, nowMillis: Long): Int {
    val left = goAtMillis - nowMillis
    if (left <= 0L) return 0
    return ((left + 999L) / 1000L).toInt()
}

internal fun raidFractionRemaining(goAtMillis: Long, nowMillis: Long, durationMillis: Long): Float {
    if (durationMillis <= 0L) return 0f
    val left = (goAtMillis - nowMillis).coerceAtLeast(0L)
    return (left.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
}

internal fun raidTargetToOpen(currentLogin: String, targetLogin: String): String? {
    val target = targetLogin.trim()
    if (target.isEmpty() || target.equals(currentLogin.trim(), ignoreCase = true)) return null
    return target
}

private fun parseRaidUpdate(raid: JSONObject, nowMillis: Long): RaidPubSubEvent.Update? {
    return parseRaid(raid, nowMillis, raid.optLong("force_raid_now_seconds", 0L), leaving = false)
        ?.let(RaidPubSubEvent::Update)
}

private fun parseRaidGo(raid: JSONObject, nowMillis: Long): RaidPubSubEvent.Go? {
    return parseRaid(raid, nowMillis, raid.optLong("transition_jitter_seconds", 0L), leaving = true)
        ?.let(RaidPubSubEvent::Go)
}

private fun parseRaid(
    raid: JSONObject,
    nowMillis: Long,
    seconds: Long,
    leaving: Boolean,
): OutgoingRaid? {
    val login = text(raid, "target_login")?.lowercase() ?: return null
    val id = text(raid, "id") ?: return null
    return OutgoingRaid(
        id = id,
        targetLogin = login,
        targetDisplayName = text(raid, "target_display_name") ?: login,
        targetAvatarUrl = text(raid, "target_profile_image"),
        viewerCount = raid.optInt("viewer_count", 0).coerceAtLeast(0),
        goAtMillis = nowMillis + seconds.coerceAtLeast(0L) * 1_000L,
        leaving = leaving,
    )
}

private fun mergeRaid(current: OutgoingRaid?, incoming: OutgoingRaid, keepDeadline: Boolean): OutgoingRaid {
    if (keepDeadline && current?.id == incoming.id) {
        return incoming.copy(goAtMillis = current.goAtMillis, leaving = current.leaving)
    }
    return incoming
}

private fun text(obj: JSONObject, key: String): String? {
    return obj.optString(key).takeIf { it.isNotBlank() && it != "null" }
}
