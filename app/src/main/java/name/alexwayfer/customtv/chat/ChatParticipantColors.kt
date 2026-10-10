package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

internal fun rememberParticipantColor(
    colors: Map<String, Color>,
    message: ChatMessage,
    maxColors: Int,
): Map<String, Color> {
    if (message.notice != null || message.emoteChange != null) return colors
    if (message.displayName.isBlank() && message.userLogin.isBlank()) return colors
    val color = message.color.takeUnless { it == Color.Unspecified } ?: Color.White
    val keys = participantKeys(message.userLogin, message.displayName)
    if (keys.isEmpty()) return colors
    if (keys.all { colors[it] == color }) return colors

    return LinkedHashMap<String, Color>(colors.size + keys.size).apply {
        colors.forEach { (key, value) ->
            if (key !in keys) put(key, value)
        }
        keys.forEach { put(it, color) }
        while (size > maxColors) remove(this.keys.first())
    }
}

internal fun knownParticipantColor(
    colors: Map<String, Color>,
    actorName: String,
    login: String,
): Color? = participantKeys(actorName, login).firstNotNullOfOrNull { key ->
    colors[key]?.takeUnless { it == Color.Unspecified }
}

private fun participantKeys(first: String, second: String): Set<String> = buildSet {
    first.trim().lowercase().takeIf { it.isNotEmpty() }?.let(::add)
    second.trim().lowercase().takeIf { it.isNotEmpty() }?.let(::add)
}
