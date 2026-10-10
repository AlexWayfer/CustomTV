package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

internal fun mergeKnownReward(
    message: ChatMessage,
    knownReward: ChatReward?,
): ChatMessage {
    val reward = message.reward ?: return message
    val known = knownReward ?: return message
    val merged = reward.copy(
        title = reward.title.ifBlank { known.title },
        cost = if (reward.cost > 0) reward.cost else known.cost,
        backgroundColorHex = reward.backgroundColorHex ?: known.backgroundColorHex,
        imageUrl = reward.imageUrl ?: known.imageUrl,
        prompt = reward.prompt ?: known.prompt,
    )
    return if (merged == reward) message else message.copy(reward = merged)
}

internal fun resolveEmoteActorColor(
    message: ChatMessage,
    knownColor: Color?,
): ChatMessage {
    val change = message.emoteChange ?: return message
    val color = knownColor ?: Color.White
    return message.copy(
        color = color,
        emoteChange = change.copy(actorColor = color),
    )
}

internal fun resolveBttvRemovalMessage(
    message: ChatMessage,
    resolvedName: String,
    cachedEmote: SevenTvEmote?,
): ChatMessage {
    val change = message.emoteChange ?: return message
    val displayName = resolvedName.ifBlank { change.emoteName }
    return message.copy(
        emoteChange = change.copy(
            emoteName = displayName,
            emote = change.emote ?: cachedEmote,
        ),
        rawText = displayName,
    )
}

internal fun resolveBttvRenameMessage(
    message: ChatMessage,
    previousName: String?,
): ChatMessage {
    val change = message.emoteChange ?: return message
    return message.copy(emoteChange = change.copy(previousName = previousName))
}
