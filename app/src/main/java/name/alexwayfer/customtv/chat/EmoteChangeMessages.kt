package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

internal fun emoteChangeMessage(
    platform: EmotePlatform,
    action: EmoteChangeAction,
    actorName: String,
    actorColor: Color = Color.Unspecified,
    emoteName: String,
    previousName: String? = null,
    emote: SevenTvEmote? = null,
    emoteId: String? = null,
    timestampMillis: Long = System.currentTimeMillis(),
    id: String = "emote-change-${System.nanoTime()}-$emoteName",
): ChatMessage {
    val change = ChatEmoteChange(
        platform = platform,
        action = action,
        actorName = actorName,
        actorColor = actorColor,
        emoteName = emoteName,
        previousName = previousName,
        emote = emote,
        emoteId = emoteId,
    )
    return ChatMessage(
        id = id,
        userLogin = actorName.lowercase(),
        displayName = actorName,
        color = actorColor,
        rawText = emoteName,
        parts = emptyList(),
        timestampMillis = timestampMillis,
        eventKind = ChatEventKind.EmoteChange,
        emoteChange = change,
    )
}
