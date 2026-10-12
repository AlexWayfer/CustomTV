package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.secondsRoundedUp

internal data class OutgoingRaid(
    val id: String,
    val targetLogin: String,
    val targetDisplayName: String,
    val targetAvatarUrl: String? = null,
    val viewerCount: Int,
    val goAtMillis: Long,
    val leaving: Boolean,
)

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

internal fun raidSecondsRemaining(goAtMillis: Long, nowMillis: Long): Int {
    val left = goAtMillis - nowMillis
    if (left <= 0L) return 0
    return secondsRoundedUp(left).toInt()
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

private fun mergeRaid(current: OutgoingRaid?, incoming: OutgoingRaid, keepDeadline: Boolean): OutgoingRaid {
    if (keepDeadline && current?.id == incoming.id) {
        return incoming.copy(goAtMillis = current.goAtMillis, leaving = current.leaving)
    }
    return incoming
}
