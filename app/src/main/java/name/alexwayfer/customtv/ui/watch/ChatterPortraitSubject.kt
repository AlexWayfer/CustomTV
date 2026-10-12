package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatterFollow

/** The chatter whose portrait the card's page shows, in the channel whose logs it reads, and what the card knows. */
internal data class ChatterPortraitSubject(
    val channelId: String,
    val channelName: String,
    val userId: String,
    val login: String,
    val displayName: String,
    /** The signed-in viewer's login, empty when logged out. */
    val selfLogin: String,
    val badgeTitles: List<String>,
    val badgeUrls: Map<String, String>,
    val createdAtMillis: Long?,
    val follow: ChatterFollow?,
)
