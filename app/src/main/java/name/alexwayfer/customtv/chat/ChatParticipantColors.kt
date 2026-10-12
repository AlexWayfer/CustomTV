package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val MAX_NICK_COLORS = 800

/** The chat colors of the chatters seen in the open channel, for rows that arrive without their own. */
internal class ParticipantColors {
    private val _colors = MutableStateFlow<Map<String, Color>>(emptyMap())
    val colors: StateFlow<Map<String, Color>> = _colors

    fun clear() {
        _colors.value = emptyMap()
    }

    fun remember(message: ChatMessage) {
        _colors.update { current ->
            rememberParticipantColor(current, message, MAX_NICK_COLORS)
        }
    }

    /** A reward row from PubSub takes the color its chatter has in chat. */
    fun paintReward(message: ChatMessage): ChatMessage {
        if (message.eventKind != ChatEventKind.Reward) return message
        if (!RewardMessageDeduper.isPubSub(message)) return message
        val known = known(message.displayName, message.userLogin) ?: return message
        return message.copy(color = known)
    }

    fun resolveEmoteActor(message: ChatMessage): ChatMessage {
        val change = message.emoteChange ?: return message
        return resolveEmoteActorColor(
            message,
            known(change.actorName, message.userLogin),
        )
    }

    private fun known(actorName: String, login: String): Color? {
        return knownParticipantColor(_colors.value, actorName, login)
    }
}

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
