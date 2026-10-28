package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val MAX_NICK_COLORS = 800

/**
 * The chat colors of the chatters seen in the open channel, for rows that arrive without their own, and their user
 * IDs, for the paints of the nicks a message mentions.
 */
internal class ParticipantColors {
    private val _colors = MutableStateFlow<Map<String, Color>>(emptyMap())
    val colors: StateFlow<Map<String, Color>> = _colors
    private val _ids = MutableStateFlow<Map<String, String>>(emptyMap())
    val ids: StateFlow<Map<String, String>> = _ids

    fun clear() {
        _colors.value = emptyMap()
        _ids.value = emptyMap()
    }

    fun remember(message: ChatMessage) {
        _colors.update { current ->
            rememberParticipantColor(current, message, MAX_NICK_COLORS)
        }
        _ids.update { current ->
            rememberParticipantId(current, message, MAX_NICK_COLORS)
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
    val color = message.color.takeUnless { it == Color.Unspecified } ?: Color.White
    return rememberByParticipant(colors, message, color, maxColors)
}

/** [ids] with the Twitch user ID of [message]'s author under their login and display name. */
internal fun rememberParticipantId(
    ids: Map<String, String>,
    message: ChatMessage,
    maxIds: Int,
): Map<String, String> {
    if (message.notice != null || message.emoteChange != null) return ids
    val id = message.userId?.takeIf { it.isNotBlank() } ?: return ids
    return rememberByParticipant(ids, message, id, maxIds)
}

// The author's keys move to the end, so the chatters not seen for longest go first past [max].
private fun <T> rememberByParticipant(
    values: Map<String, T>,
    message: ChatMessage,
    value: T,
    max: Int,
): Map<String, T> {
    if (message.displayName.isBlank() && message.userLogin.isBlank()) return values
    val keys = participantKeys(message.userLogin, message.displayName)
    if (keys.isEmpty()) return values
    if (keys.all { values[it] == value }) return values

    return LinkedHashMap<String, T>(values.size + keys.size).apply {
        values.forEach { (key, known) ->
            if (key !in keys) put(key, known)
        }
        keys.forEach { put(it, value) }
        while (size > max) remove(this.keys.first())
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
