package name.alexwayfer.customtv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

val TwitchBg = Color(0xFF0E0E10)
val TwitchSurface = Color(0xFF18181B)
val TwitchSurfaceAlt = Color(0xFF1F1F23)
val TwitchPurple = Color(0xFF9147FF)
val TwitchPurpleHover = Color(0xFF772CE8)
val TwitchText = Color(0xFFEFEFF1)
val TwitchTextSecondary = Color(0xFFADADB8)
val TwitchHint = Color(0xFF53535F)
val TwitchDivider = Color(0xFF2F2F35)
val TwitchLive = Color(0xFFEB0400)
val TwitchHighlightDefault = Color(0xFFFF6905)

/** The flame of a watch streak, as the Twitch web draws it. */
val TwitchWatchStreak = Color(0xFFFFB31A)
val PremiumGold = Color(0xFFE8B923)
val ChatFirstMessage = Color(0xFF1C4524).copy(alpha = 0.52f)
val ChatFirstTimeChatter = Color(0xFFC832C8).copy(alpha = 0.4f)
val ChatMention = Color(0xFF8C1D1D).copy(alpha = 0.55f)

/** The mention color as it looks on the chat background, for a view drawn over chat rows. */
val ChatMentionOnChat = ChatMention.compositeOver(TwitchBg)
val ChatFlaggedTerm = Color(0xFF8C1D1D)
val AutoModShield = Color(0xFF00B35C)
val ChatLink = Color(0xFFBF94FF)
val ChatRaiderFrame = Color(0xFFE6D21E)

fun parseTwitchHexColor(raw: String?): Color? {
    val hex = raw?.trim()?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
    return try {
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        Color(red = r, green = g, blue = b)
    } catch (_: NumberFormatException) {
        null
    }
}
