package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.auth.TwitchProfileLink
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal data class ChatterProfile(
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerImageUrl: String?,
    val createdAtMillis: Long?,
    val userId: String? = null,
    val about: String? = null,
    val links: List<TwitchProfileLink> = emptyList(),
)

internal data class ChatterBadgeLine(
    val imageUrl: String,
    val title: String,
)

internal data class ChatterSubscription(
    val founder: Boolean,
    val months: Int?,
)

internal fun chatterBadgeLines(
    badges: List<ChatBadge>,
    imageUrls: Map<String, String>,
    titles: Map<String, String>,
): List<ChatterBadgeLine> = badges.mapNotNull { badge ->
    val imageUrl = imageUrls[badge.key] ?: return@mapNotNull null
    val title = titles[badge.key]?.takeIf { it.isNotBlank() } ?: badgeFallbackTitle(badge)
    ChatterBadgeLine(imageUrl = imageUrl, title = title)
}

internal fun badgeFallbackTitle(badge: ChatBadge): String =
    badge.setId.split('-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            if (word.equals("vip", ignoreCase = true)) {
                "VIP"
            } else {
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
        }
        .ifBlank { badge.setId }

/** Whether a chatter follows the open channel, when the app may know it. */
sealed interface ChatterFollow {
    data class Following(val atMillis: Long) : ChatterFollow
    data object NotFollowing : ChatterFollow
}

internal fun chatterCardShowsOwnFollow(
    cardUserId: String?,
    cardLogin: String,
    selfUserId: String?,
    selfLogin: String,
): Boolean = selfLogin.isNotBlank() && when {
    !selfUserId.isNullOrBlank() && !cardUserId.isNullOrBlank() -> cardUserId == selfUserId
    else -> cardLogin.equals(selfLogin, ignoreCase = true)
}

/** A signed-in viewer can whisper anyone else from the card, not their own account. */
internal fun chatterCardOffersWhisper(
    cardUserId: String?,
    cardLogin: String,
    selfUserId: String?,
    selfLogin: String,
): Boolean = selfLogin.isNotBlank() && !chatterCardShowsOwnFollow(cardUserId, cardLogin, selfUserId, selfLogin)

internal fun chatterSubscription(badges: List<ChatBadge>): ChatterSubscription? {
    val founder = badges.any { it.setId.equals("founder", ignoreCase = true) }
    val subscriber = badges.firstOrNull { it.setId.equals("subscriber", ignoreCase = true) }
    if (!founder && subscriber == null) return null
    val months = subscriber?.version?.toIntOrNull()?.takeIf { it in 1..200 }
    return ChatterSubscription(founder = founder, months = months)
}

/** How long ago something happened, in calendar units of the viewer's time zone. */
internal data class CalendarAge(
    val years: Long,
    val months: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long,
)

internal fun calendarAge(fromMillis: Long, nowMillis: Long, zone: ZoneId): CalendarAge {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    var cursor = Instant.ofEpochMilli(fromMillis.coerceAtMost(nowMillis)).atZone(zone)
    val years = ChronoUnit.YEARS.between(cursor, now)
    cursor = cursor.plusYears(years)
    val months = ChronoUnit.MONTHS.between(cursor, now)
    cursor = cursor.plusMonths(months)
    val days = ChronoUnit.DAYS.between(cursor, now)
    cursor = cursor.plusDays(days)
    val hours = ChronoUnit.HOURS.between(cursor, now)
    cursor = cursor.plusHours(hours)
    return CalendarAge(years, months, days, hours, ChronoUnit.MINUTES.between(cursor, now))
}

internal enum class AgeUnit { Years, Months, Days, Hours, Minutes }

/**
 * The units worth naming, leaving out zeros: hours and minutes for less than a day, otherwise years down to hours.
 * Something less than a minute old reads as zero minutes.
 */
internal fun CalendarAge.shownUnits(): List<Pair<AgeUnit, Long>> {
    val underADay = years == 0L && months == 0L && days == 0L
    val units = if (underADay) {
        listOf(AgeUnit.Hours to hours, AgeUnit.Minutes to minutes)
    } else {
        listOf(AgeUnit.Years to years, AgeUnit.Months to months, AgeUnit.Days to days, AgeUnit.Hours to hours)
    }
    return units.filter { (_, count) -> count > 0 }.ifEmpty { listOf(AgeUnit.Minutes to 0L) }
}

internal enum class ChatterFollowSource {
    /** The user's own card: their follow is read on any channel. */
    Own,

    /** Someone else on a channel the user moderates. */
    Moderator,

    /** Nobody may learn this chatter's follow here, so the card has no follow row and no loader. */
    None,
}

internal fun chatterFollowSource(ownCard: Boolean, chatterUserId: String?, moderatesChannel: Boolean): ChatterFollowSource =
    when {
        ownCard -> ChatterFollowSource.Own
        !chatterUserId.isNullOrBlank() && moderatesChannel -> ChatterFollowSource.Moderator
        else -> ChatterFollowSource.None
    }

/** What a long press copies from a card fact: its title and the line under it, such as the follow date and age. */
internal fun chatterFactCopyText(title: String, supporting: String?): String =
    if (supporting.isNullOrBlank()) title else "$title $supporting"
