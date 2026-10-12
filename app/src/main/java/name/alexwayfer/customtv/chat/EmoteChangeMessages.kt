package name.alexwayfer.customtv.chat

internal fun emoteChangeMessage(
    change: ChatEmoteChange,
    timestampMillis: Long = System.currentTimeMillis(),
    id: String = "emote-change-${System.nanoTime()}-${change.emoteName}",
): ChatMessage {
    return ChatMessage(
        id = id,
        userLogin = change.actorName.lowercase(),
        displayName = change.actorName,
        color = change.actorColor,
        rawText = change.emoteName,
        parts = emptyList(),
        timestampMillis = timestampMillis,
        eventKind = ChatEventKind.EmoteChange,
        emoteChange = change,
    )
}
