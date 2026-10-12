package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

internal sealed interface RaidPubSubEvent {
    data class Update(val raid: OutgoingRaid) : RaidPubSubEvent
    data class Go(val raid: OutgoingRaid) : RaidPubSubEvent
    data class Cancel(val raidId: String) : RaidPubSubEvent
}

internal fun parseRaidPubSubFrame(raw: String, nowMillis: Long): RaidPubSubEvent? {
    val frame = raw.toJsonObjectOrNull() ?: return null
    if (frame.optString("type") != "MESSAGE") return null
    val data = frame.optJSONObject("data") ?: return null
    val topic = data.optString("topic")
    if (!topic.startsWith("raid.")) return null
    val message = data.text("message") ?: return null
    return parseRaidPubSubMessage(message, nowMillis)
}

internal fun parseRaidPubSubMessage(raw: String, nowMillis: Long): RaidPubSubEvent? {
    val payload = raw.toJsonObjectOrNull() ?: return null
    val raid = payload.optJSONObject("raid") ?: return null
    return when (payload.optString("type")) {
        "raid_update_v2", "raid_update" -> parseRaidUpdate(raid, nowMillis)
        "raid_go_v2", "raid_go" -> parseRaidGo(raid, nowMillis)
        "raid_cancel_v2", "raid_cancel" -> {
            val id = raid.text("id") ?: return null
            RaidPubSubEvent.Cancel(id)
        }
        else -> null
    }
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
    val login = raid.text("target_login")?.lowercase() ?: return null
    val id = raid.text("id") ?: return null
    return OutgoingRaid(
        id = id,
        targetLogin = login,
        targetDisplayName = raid.text("target_display_name") ?: login,
        targetAvatarUrl = raid.text("target_profile_image"),
        viewerCount = raid.optInt("viewer_count", 0).coerceAtLeast(0),
        goAtMillis = nowMillis + seconds.coerceAtLeast(0L) * 1_000L,
        leaving = leaving,
    )
}
