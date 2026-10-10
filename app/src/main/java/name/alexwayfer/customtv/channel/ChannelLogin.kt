package name.alexwayfer.customtv.channel

private val CHANNEL_REGEX = Regex("^[a-zA-Z0-9_]{1,25}$")

fun normalizeChannelInput(raw: String): String {
    var value = raw.trim()
    value = value.removePrefixIgnoreCase("https://").removePrefixIgnoreCase("http://")
    value = value.removePrefixIgnoreCase("www.")
    if (value.startsWith("twitch.tv/", ignoreCase = true)) {
        value = value.substringAfter('/')
    }
    value = value.substringBefore('/').substringBefore('?').substringBefore('#')
    return value.removePrefix("@").lowercase()
}

fun isValidChannelLogin(login: String): Boolean = CHANNEL_REGEX.matches(login)

private fun String.removePrefixIgnoreCase(prefix: String): String {
    return if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
}

fun channelNameWithCollaborators(displayName: String, collaboratorCount: Int?): String {
    if (collaboratorCount == null || collaboratorCount <= 0) return displayName
    return "$displayName +$collaboratorCount"
}

fun displayNameLabel(displayName: String, login: String): String {
    return displayName + (displayNameLoginSuffix(displayName, login).orEmpty())
}

fun displayNameLoginSuffix(displayName: String, login: String): String? {
    return if (displayName.equals(login, ignoreCase = true)) {
        null
    } else {
        " ($login)"
    }
}
