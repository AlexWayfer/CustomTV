package name.alexwayfer.customtv.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * The server forgets a free badge a day after the app last confirmed it, so the app confirms its own badge this often
 * while open, and tries again this long after a failure.
 */
internal val OWN_BADGE_REFRESH = 1.hours

/** What this device registers its own badge with. */
internal sealed interface CustomTvBadgeDevice {
    /** The free build: a free badge. */
    data object Free : CustomTvBadgeDevice

    /** The premium build with access: the Premium badge, by the secret the Mecena server knows the device by. */
    data class Premium(val secret: String) : CustomTvBadgeDevice

    /** The premium build without access, which is locked: no badge. */
    data object Locked : CustomTvBadgeDevice
}

/** The account whose badge this device shows, and in which edition. */
internal data class OwnBadgeKey(val twitchUserId: String, val edition: CustomTvBadgeEdition)

/** The badge this device registered last in this run, and when, in elapsed realtime. */
internal data class OwnBadgeRegistration(val key: OwnBadgeKey, val atMillis: Long)

internal sealed interface OwnBadgeStep {
    data object Wait : OwnBadgeStep
    data class Register(val key: OwnBadgeKey) : OwnBadgeStep
    data class Remove(val key: OwnBadgeKey) : OwnBadgeStep
}

/** The badge the logged-in account should show from this device; null while logged out or locked. */
internal fun ownBadgeKey(twitchUserId: String?, device: CustomTvBadgeDevice): OwnBadgeKey? {
    if (twitchUserId == null) return null
    return when (device) {
        CustomTvBadgeDevice.Free -> OwnBadgeKey(twitchUserId, CustomTvBadgeEdition.Free)
        is CustomTvBadgeDevice.Premium -> OwnBadgeKey(twitchUserId, CustomTvBadgeEdition.Premium)
        CustomTvBadgeDevice.Locked -> null
    }
}

/**
 * What to do about the own badge. A badge turned off is taken off; one turned on is registered for the account, and
 * confirmed again [OWN_BADGE_REFRESH] after. Logging out or switching accounts takes nothing off: a free badge ends a
 * day later, and a Premium one stays until a third account pushes it out.
 */
internal fun ownBadgeStep(
    enabled: Boolean,
    wanted: OwnBadgeKey?,
    registered: OwnBadgeRegistration?,
    nowMillis: Long,
): OwnBadgeStep = when {
    !enabled -> registered?.let { OwnBadgeStep.Remove(it.key) } ?: OwnBadgeStep.Wait
    wanted == null -> OwnBadgeStep.Wait
    registered != null && registered.key == wanted &&
        nowMillis - registered.atMillis < OWN_BADGE_REFRESH.inWholeMilliseconds ->
        OwnBadgeStep.Wait
    else -> OwnBadgeStep.Register(wanted)
}

/** How long to wait after a [OwnBadgeStep.Wait]: until [registered] is due again, or a whole refresh without one. */
internal fun ownBadgeWait(wanted: OwnBadgeKey?, registered: OwnBadgeRegistration?, nowMillis: Long): Duration {
    if (wanted == null || registered == null || registered.key != wanted) return OWN_BADGE_REFRESH
    val left = registered.atMillis + OWN_BADGE_REFRESH.inWholeMilliseconds - nowMillis
    return left.coerceAtLeast(0).milliseconds
}
