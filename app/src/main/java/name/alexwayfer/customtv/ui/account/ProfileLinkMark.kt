package name.alexwayfer.customtv.ui.account

import java.net.URI

internal enum class ProfileLinkMark {
    Link,
    Twitter,
    GitHub,
    Discord,
    Deezer,
    YouTube,
    Instagram,
    TikTok,
    Facebook,
    Steam,
    Boosty,
    Telegram,
}

/** Brand icon for a known service name or host. Anything else stays the plain link mark. */
internal fun profileLinkMark(name: String?, url: String): ProfileLinkMark {
    val fromName = markForName(name)
    if (fromName != ProfileLinkMark.Link) return fromName
    val host = runCatching { URI(url).host }.getOrNull()?.lowercase()?.removePrefix("www.")
        ?: return ProfileLinkMark.Link
    return markForHost(host) ?: markForLabel(host.substringBefore('.'))
}

private val nameWord = Regex("""[\p{L}\p{N}]+""")

private val nameWordMarks = listOf(
    ProfileLinkMark.Steam to setOf("steam", "стим"),
    ProfileLinkMark.Boosty to setOf("boosty", "бусти"),
    ProfileLinkMark.Telegram to setOf("telegram", "telega", "tg", "тг", "телеграм", "телеграмм", "телега"),
)

private fun markForName(name: String?): ProfileLinkMark {
    if (name == null) return ProfileLinkMark.Link
    // Streamers decorate titles ("🎮 Мой Стим"), so a whole word is enough, not the whole title.
    val words = nameWord.findAll(name.lowercase()).map { it.value }.toSet()
    nameWordMarks.firstOrNull { (_, marks) -> marks.any { it in words } }?.let { return it.first }
    val key = name.trim().lowercase().substringBefore('/').removePrefix("www.").substringBefore('.')
    return markForLabel(key)
}

/** Services matched by their exact domains, so a look-alike host keeps the plain link mark. */
private fun markForHost(host: String): ProfileLinkMark? = when {
    host == "steamcommunity.com" || host == "steampowered.com" || host.endsWith(".steampowered.com") ->
        ProfileLinkMark.Steam
    host == "boosty.to" -> ProfileLinkMark.Boosty
    host == "t.me" -> ProfileLinkMark.Telegram
    else -> null
}

private fun markForLabel(label: String): ProfileLinkMark {
    return when (label) {
        "twitter", "x" -> ProfileLinkMark.Twitter
        "github" -> ProfileLinkMark.GitHub
        "discord" -> ProfileLinkMark.Discord
        "deezer" -> ProfileLinkMark.Deezer
        "youtube", "youtu" -> ProfileLinkMark.YouTube
        "instagram" -> ProfileLinkMark.Instagram
        "tiktok" -> ProfileLinkMark.TikTok
        "facebook", "fb" -> ProfileLinkMark.Facebook
        else -> ProfileLinkMark.Link
    }
}
