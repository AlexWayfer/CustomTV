package name.alexwayfer.customtv.chat

/**
 * The 7TV paints and badges of the open channel's chatters, by Twitch user ID. As with personal emotes, a chatter's
 * client tells 7TV it is in the channel, and 7TV sends every open chat there the paint and the badge they chose.
 */
internal data class SevenTvCosmetics(
    /** Paints by their ID; they hold no chatter, so another channel uses them again. */
    val paints: Map<String, NamePaint> = emptyMap(),
    val badges: Map<String, ThirdPartyBadge> = emptyMap(),
    val userPaints: Map<String, String> = emptyMap(),
    val userBadges: Map<String, String> = emptyMap(),
) {
    fun paintFor(userId: String): NamePaint? = userPaints[userId]?.let(paints::get)

    fun badgeFor(userId: String): ThirdPartyBadge? = userBadges[userId]?.let(badges::get)

    /** The paints and badges without the chatters, for another channel. */
    fun withoutChatters(): SevenTvCosmetics = copy(userPaints = emptyMap(), userBadges = emptyMap())

    fun apply(event: SevenTvCosmeticEvent): SevenTvCosmetics = when (event) {
        is SevenTvCosmeticEvent.PaintDefined -> copy(paints = paints + (event.id to event.paint))
        is SevenTvCosmeticEvent.BadgeDefined -> copy(badges = badges + (event.id to event.badge))
        is SevenTvCosmeticEvent.Assigned -> when (event.kind) {
            SevenTvCosmeticKind.Paint -> copy(userPaints = userPaints + event.userIds.associateWith { event.refId })
            SevenTvCosmeticKind.Badge -> copy(userBadges = userBadges + event.userIds.associateWith { event.refId })
        }
        is SevenTvCosmeticEvent.Revoked -> when (event.kind) {
            SevenTvCosmeticKind.Paint -> copy(userPaints = userPaints.revoked(event))
            SevenTvCosmeticKind.Badge -> copy(userBadges = userBadges.revoked(event))
        }
    }

    // Only the revoked one goes: a chatter who owns several gets one revoked while another stays chosen.
    private fun Map<String, String>.revoked(event: SevenTvCosmeticEvent.Revoked): Map<String, String> =
        this - event.userIds.filter { this[it] == event.refId }.toSet()
}

internal enum class SevenTvCosmeticKind { Paint, Badge }

/** What the 7TV socket says about paints and badges. */
internal sealed interface SevenTvCosmeticEvent {
    data class PaintDefined(val id: String, val paint: NamePaint) : SevenTvCosmeticEvent

    data class BadgeDefined(val id: String, val badge: ThirdPartyBadge) : SevenTvCosmeticEvent

    /** The chatters with these Twitch user IDs show the paint or badge [refId]. */
    data class Assigned(
        val kind: SevenTvCosmeticKind,
        val refId: String,
        val userIds: List<String>,
    ) : SevenTvCosmeticEvent

    data class Revoked(
        val kind: SevenTvCosmeticKind,
        val refId: String,
        val userIds: List<String>,
    ) : SevenTvCosmeticEvent
}
