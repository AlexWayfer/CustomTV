package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

/** A chat message for a test: [text] from [login], with the fields a test does not name at their defaults. */
internal fun testChatMessage(
    id: String = "1",
    text: String = "text",
    login: String = "viewer",
    displayName: String = login,
    color: Color = Color.White,
    parts: List<ChatPart> = listOf(ChatPart.Text(text)),
    timestampMillis: Long = 1L,
) = ChatMessage(
    id = id,
    userLogin = login,
    displayName = displayName,
    color = color,
    rawText = text,
    parts = parts,
    timestampMillis = timestampMillis,
)
